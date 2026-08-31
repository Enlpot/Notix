package com.enlpot.notix.plugin

import android.content.Context
import android.os.Build
import android.util.Log
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

/**
 * 分词插件管理器（v8.43.0：插件化架构）
 *
 * 负责高级分词插件的下载、解压、加载、卸载。
 * 默认使用内置简单分词器，用户可选安装 HanLP 高级分词插件。
 *
 * 插件分发形态：单个 zip 包（word_tokenizer_hanlp.zip），内含：
 * - word_tokenizer_hanlp.dex：插件类 + HanLP 类
 * - data/：HanLP 词典目录
 * 用户点击安装后全自动完成（下载→解压→加载），并回调阶段进度。
 *
 * 插件加载原理：
 * - 下载 zip 到缓存，解压到 filesDir/plugin_hanlp/
 * - 设置系统属性 notix.hanlp.root 指向插件目录（HanLPWordTokenizer 初始化时读取）
 * - 用 DexClassLoader 加载插件 dex，反射创建实例并调用 segment/name
 * - 加载失败自动回退到内置简单分词器
 */
object WordTokenizerManager {

    private const val TAG = "WordTokenizerManager"
    private const val PLUGIN_CLASS_NAME = "com.enlpot.notix.plugin.wordtokenizer.HanLPWordTokenizer"
    private const val PLUGIN_DEX_NAME = "word_tokenizer_hanlp.dex"
    private const val PLUGIN_ZIP_NAME = "word_tokenizer_hanlp.zip"

    // HanLP 词典根目录系统属性（HanLPWordTokenizer 初始化时读取）
    private const val DICT_ROOT_PROPERTY = "notix.hanlp.root"

    // GitHub Release 最新版插件下载地址（由 CI 随发行版发布）
    private const val PLUGIN_DOWNLOAD_URL =
        "https://github.com/Enlpot/Notix/releases/latest/download/word_tokenizer_hanlp.zip"

    /** 安装阶段 */
    enum class InstallStage {
        /** 下载中（progress 0-100） */
        DOWNLOADING,
        /** 解压中 */
        EXTRACTING,
        /** 加载中 */
        LOADING
    }

    @Volatile
    private var currentTokenizer: WordTokenizer = SimpleWordTokenizer()

    @Volatile
    private var pluginLoaded = false

    /** 获取当前分词器 */
    fun getTokenizer(): WordTokenizer = currentTokenizer

    /** 检查插件是否已加载 */
    fun isPluginLoaded(): Boolean = pluginLoaded

    /** 插件根目录 */
    private fun getPluginDir(context: Context): File =
        File(context.filesDir, "plugin_hanlp")

    /** 插件 dex 文件 */
    private fun getDexFile(context: Context): File =
        File(getPluginDir(context), PLUGIN_DEX_NAME)

    /** 检查插件是否已安装（dex 存在） */
    fun isPluginDownloaded(context: Context): Boolean =
        getDexFile(context).exists()

    /**
     * 加载插件（从已解压的本地插件目录）。
     * @return true 加载成功，false 加载失败（自动回退到内置分词器）
     */
    fun loadPlugin(context: Context): Boolean {
        val dexFile = getDexFile(context)
        if (!dexFile.exists()) {
            Log.w(TAG, "插件 dex 不存在: ${dexFile.absolutePath}")
            return false
        }

        return try {
            val pluginDir = getPluginDir(context)

            // 设置 HanLP 词典根目录（HanLPWordTokenizer 构造时读取系统属性）
            System.setProperty(DICT_ROOT_PROPERTY, pluginDir.absolutePath)

            // Android 8.0+ 禁止从可写目录加载 dex（Writable dex file is not allowed），
            // 必须用 InMemoryDexClassLoader 从内存加载；API 24-25 无此限制，可用 DexClassLoader。
            val dexClassLoader: ClassLoader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val dexBytes = dexFile.readBytes()
                InMemoryDexClassLoader(ByteBuffer.wrap(dexBytes), WordTokenizer::class.java.classLoader)
            } else {
                val optimizedDir = File(context.cacheDir, "plugin_opt")
                if (!optimizedDir.exists()) optimizedDir.mkdirs()
                DexClassLoader(
                    dexFile.absolutePath,
                    optimizedDir.absolutePath,
                    null,
                    WordTokenizer::class.java.classLoader
                )
            }

            // 反射创建 HanLPWordTokenizer 实例
            val pluginClass = dexClassLoader.loadClass(PLUGIN_CLASS_NAME)
            val instance = pluginClass.getDeclaredConstructor().newInstance()

            // 反射获取方法引用（插件不实现 app 接口，跨 classloader 无法强转，用反射包装）
            val segmentMethod = pluginClass.getMethod("segment", String::class.java)
            val nameMethod = pluginClass.getMethod("name")

            currentTokenizer = object : WordTokenizer {
                @Suppress("UNCHECKED_CAST")
                override fun segment(text: String): List<String> = try {
                    segmentMethod.invoke(instance, text) as List<String>
                } catch (e: Exception) {
                    Log.w(TAG, "插件分词调用失败，返回空", e)
                    emptyList()
                }

                override fun name(): String = try {
                    nameMethod.invoke(instance) as String
                } catch (e: Exception) {
                    "HanLP 高级分词"
                }
            }
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
        try {
            val dir = getPluginDir(context)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除插件目录失败", e)
        }
        Log.i(TAG, "插件已卸载，回退到内置分词器")
    }

    /**
     * 下载并安装插件（单 zip：下载→解压→加载）。
     * 全程通过 onStage 回调阶段进度，用户无需任何操作。
     *
     * @param onStage 阶段回调（stage, progress），progress 仅 DOWNLOADING 阶段有意义（0-100）
     * @return true 安装成功，false 失败（自动清理半成品）
     */
    suspend fun downloadAndInstallPlugin(
        context: Context,
        onStage: (InstallStage, Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val tmpZip = File(context.cacheDir, PLUGIN_ZIP_NAME)
        try {
            // ---- 1. 下载 ----
            onStage(InstallStage.DOWNLOADING, 0)
            val url = URL(PLUGIN_DOWNLOAD_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "下载失败，HTTP code: ${connection.responseCode}")
                return@withContext false
            }

            val totalLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = tmpZip.outputStream()
            val buffer = ByteArray(8192)
            var downloaded = 0
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                if (totalLength > 0) {
                    onStage(InstallStage.DOWNLOADING, (downloaded * 100 / totalLength).toInt())
                }
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            Log.i(TAG, "插件下载完成: ${tmpZip.absolutePath}, 大小: $downloaded bytes")

            // ---- 2. 解压 ----
            onStage(InstallStage.EXTRACTING, 0)
            val pluginDir = getPluginDir(context)
            if (pluginDir.exists()) {
                pluginDir.deleteRecursively()
            }
            pluginDir.mkdirs()
            unzip(tmpZip, pluginDir)
            Log.i(TAG, "插件解压完成: ${pluginDir.absolutePath}")

            // ---- 3. 加载 ----
            onStage(InstallStage.LOADING, 0)
            val loaded = loadPlugin(context)
            if (!loaded) {
                Log.e(TAG, "插件解压成功但加载失败，清理插件目录")
                pluginDir.deleteRecursively()
            }
            tmpZip.delete()
            loaded
        } catch (e: Exception) {
            Log.e(TAG, "插件安装失败", e)
            try {
                tmpZip.delete()
                val dir = getPluginDir(context)
                if (dir.exists()) dir.deleteRecursively()
            } catch (cleanup: Exception) {
                // 忽略清理异常
            }
            false
        }
    }

    /** 解压 zip 到目标目录 */
    private fun unzip(zipFile: File, targetDir: File) {
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val target = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    /**
     * app 启动时尝试加载已安装的插件。
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
