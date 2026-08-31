package com.enlpot.notix.plugin

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 分词插件管理器（v8.43.0：插件化架构）
 *
 * 负责高级分词插件的下载、加载、卸载。
 * 默认使用内置简单分词器，用户可选安装 HanLP 高级分词插件。
 *
 * 插件加载原理：
 * - 用 DexClassLoader 加载插件 dex 文件
 * - 反射创建 HanLPWordTokenizer 实例
 * - 转换成主 app 的 WordTokenizer 接口
 * - 加载失败自动回退到内置简单分词器
 */
object WordTokenizerManager {

    private const val TAG = "WordTokenizerManager"
    private const val PLUGIN_CLASS_NAME = "com.enlpot.notix.plugin.wordtokenizer.HanLPWordTokenizer"
    private const val PLUGIN_FILE_NAME = "word_tokenizer_hanlp.dex"

    // GitHub Release 插件下载地址（后续替换为实际地址）
    private const val PLUGIN_DOWNLOAD_URL = "https://github.com/Enlpot/Notix/releases/download/plugins/word_tokenizer_hanlp.dex"

    @Volatile
    private var currentTokenizer: WordTokenizer = SimpleWordTokenizer()

    @Volatile
    private var pluginLoaded = false

    /** 获取当前分词器 */
    fun getTokenizer(): WordTokenizer = currentTokenizer

    /** 检查插件是否已加载 */
    fun isPluginLoaded(): Boolean = pluginLoaded

    /** 获取插件文件路径 */
    private fun getPluginFile(context: Context): File =
        File(context.filesDir, PLUGIN_FILE_NAME)

    /** 检查插件是否已下载 */
    fun isPluginDownloaded(context: Context): Boolean =
        getPluginFile(context).exists()

    /**
     * 加载插件（从本地 dex 文件）。
     * @return true 加载成功，false 加载失败（自动回退到内置分词器）
     */
    fun loadPlugin(context: Context): Boolean {
        val pluginFile = getPluginFile(context)
        if (!pluginFile.exists()) {
            Log.w(TAG, "插件文件不存在: ${pluginFile.absolutePath}")
            return false
        }

        return try {
            // 用 DexClassLoader 加载插件
            val optimizedDir = File(context.cacheDir, "plugin_opt")
            if (!optimizedDir.exists()) optimizedDir.mkdirs()

            val dexClassLoader = DexClassLoader(
                pluginFile.absolutePath,
                optimizedDir.absolutePath,
                null,
                WordTokenizer::class.java.classLoader
            )

            // 反射创建 HanLPWordTokenizer 实例
            val pluginClass = dexClassLoader.loadClass(PLUGIN_CLASS_NAME)
            val instance = pluginClass.getDeclaredConstructor().newInstance()
            currentTokenizer = instance as WordTokenizer
            pluginLoaded = true

            Log.i(TAG, "插件加载成功: ${currentTokenizer.name()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "插件加载失败，回退到内置分词器", e)
            currentTokenizer = SimpleWordTokenizer()
            pluginLoaded = false
            false
        }
    }

    /** 卸载插件，回退到内置分词器 */
    fun unloadPlugin(context: Context) {
        currentTokenizer = SimpleWordTokenizer()
        pluginLoaded = false
        // 删除插件文件
        try {
            val pluginFile = getPluginFile(context)
            if (pluginFile.exists()) {
                pluginFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除插件文件失败", e)
        }
        Log.i(TAG, "插件已卸载，回退到内置分词器")
    }

    /**
     * 下载并安装插件。
     * @param onProgress 下载进度回调（0-100）
     * @return true 下载并加载成功，false 失败
     */
    suspend fun downloadAndInstallPlugin(
        context: Context,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(PLUGIN_DOWNLOAD_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "下载失败，HTTP code: ${connection.responseCode}")
                return@withContext false
            }

            val totalLength = connection.contentLength
            val inputStream = connection.inputStream
            val pluginFile = getPluginFile(context)
            val outputStream = pluginFile.outputStream()

            val buffer = ByteArray(8192)
            var downloaded = 0
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                if (totalLength > 0) {
                    val progress = (downloaded * 100 / totalLength).toInt()
                    onProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            Log.i(TAG, "插件下载完成: ${pluginFile.absolutePath}, 大小: $downloaded")

            // 下载完成后加载插件
            val loaded = loadPlugin(context)
            if (loaded) {
                Log.i(TAG, "插件下载并加载成功")
            } else {
                Log.e(TAG, "插件下载成功但加载失败")
                pluginFile.delete()
            }
            loaded
        } catch (e: Exception) {
            Log.e(TAG, "插件下载失败", e)
            false
        }
    }

    /**
     * app 启动时尝试加载已下载的插件。
     * 在 Application.onCreate 中调用。
     */
    fun init(context: Context) {
        if (isPluginDownloaded(context)) {
            val loaded = loadPlugin(context)
            if (loaded) {
                Log.i(TAG, "启动时自动加载插件成功")
            }
        }
    }
}
