package com.enlpot.notix.plugin

import android.content.Context
import android.os.Build
import android.util.Log
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import com.enlpot.notix.DebugLogManager
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
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

    // 插件固定下载路径（独立 release plugin-hanlp，不随主版本变化）
    private const val PLUGIN_PATH =
        "https://github.com/Enlpot/Notix/releases/download/plugin-hanlp/word_tokenizer_hanlp.zip"

    private const val PREF_NAME = "plugin_settings"
    private const val PREF_MIRRORS = "plugin_mirror_prefixes"

    /** 内置默认镜像源（首次使用时写入，可删除）——实测对固定 tag（plugin-hanlp）下载均可用 */
    private val DEFAULT_MIRROR_PREFIXES = listOf(
        "https://ghfast.top",
        "https://gh-proxy.com",
        "https://gh.llkk.cc",
        "https://gh.ddlc.top",
        "https://ghproxy.cn",
        "https://ghproxy.link"
    )

    /** 镜像源（不含官方）数量上限 */
    const val MAX_MIRROR_COUNT = 10

    // v8.46.0：镜像源管理
    // 官方源前缀为空字符串（直接使用 PLUGIN_PATH），固定存在不可删除
    private const val OFFICIAL_PREFIX = ""

    /** 连通性测试超时（毫秒） */
    private const val LATENCY_TIMEOUT_MS = 5000
    /** 下载单次无响应超时（毫秒） */
    private const val READ_TIMEOUT_MS = 30000
    /** 下载卡住判定：窗口内速率低于该值视为卡住（字节/秒） */
    private const val STALL_THRESHOLD_BPS = 5 * 1024
    /** 下载卡住判定窗口（毫秒） */
    private const val STALL_WINDOW_MS = 10000

    /** 安装结果 */
    sealed class PluginInstallResult {
        object Success : PluginInstallResult()
        data class Failure(val reason: String) : PluginInstallResult()
    }

    /** 镜像源前缀列表（不含官方源）；首次调用写入内置默认源 */
    fun getMirrorPrefixes(context: Context): List<String> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = sp.getString(PREF_MIRRORS, null)
        if (raw == null) {
            sp.edit().putString(PREF_MIRRORS, DEFAULT_MIRROR_PREFIXES.joinToString("\n")).apply()
            DebugLogManager.i(TAG, "首次写入默认镜像源: $DEFAULT_MIRROR_PREFIXES")
            return DEFAULT_MIRROR_PREFIXES
        }
        return raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    /** 添加镜像源前缀；返回 null 成功，否则错误文案 */
    fun addMirror(context: Context, prefix: String): String? {
        val p = prefix.trim().trimEnd('/')
        if (p.isBlank()) return "镜像源前缀不能为空"
        val cur = getMirrorPrefixes(context).toMutableList()
        if (p in cur) return "该镜像源已存在"
        if (cur.size >= MAX_MIRROR_COUNT) return "最多可添加 $MAX_MIRROR_COUNT 个镜像源"
        cur.add(p)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_MIRRORS, cur.joinToString("\n")).apply()
        DebugLogManager.i(TAG, "添加镜像源: $p")
        return null
    }

    /** 删除镜像源前缀 */
    fun removeMirror(context: Context, prefix: String) {
        val cur = getMirrorPrefixes(context).toMutableList()
        cur.remove(prefix)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_MIRRORS, cur.joinToString("\n")).apply()
        DebugLogManager.i(TAG, "删除镜像源: $prefix")
    }

    /** 官方源前缀（空字符串） */
    fun getOfficialPrefix(): String = OFFICIAL_PREFIX

    /** 根据前缀构建下载 URL（官方前缀为空则直接用固定路径） */
    fun buildDownloadUrl(prefix: String): String =
        if (prefix.isBlank()) PLUGIN_PATH else prefix.trim().trimEnd('/') + "/" + PLUGIN_PATH

    /** 测试某镜像源连通性（5 秒超时），返回延迟 ms；失败/超时返回 -1。
     *  测的是「镜像服务器前缀」本身的可达性（官方源测 github.com），而非完整插件 URL，
     *  避免插件文件未上传（404）时误判镜像不可用。 */
    suspend fun testLatency(prefix: String): Long = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        try {
            val target = if (prefix.isBlank()) "https://github.com" else prefix.trim().trimEnd('/')
            val conn = URL(target).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = LATENCY_TIMEOUT_MS
            conn.readTimeout = LATENCY_TIMEOUT_MS
            conn.instanceFollowRedirects = true
            conn.connect()
            val code = conn.responseCode
            if (code in 200..399) {
                conn.inputStream.use { it.read(ByteArray(128)) }
                System.currentTimeMillis() - t0
            } else {
                conn.disconnect()
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    /** 将异常/HTTP 错误转为用户可读原因 */
    private fun describeError(e: Exception?, httpCode: Int? = null): String = when {
        httpCode != null -> "下载失败（HTTP $httpCode）"
        e is java.net.SocketTimeoutException -> "网络超时（镜像无响应）"
        e is java.net.ConnectException -> "网络异常（无法连接）"
        e is java.net.SocketException -> "网络异常（连接被重置）"
        e is java.net.UnknownHostException -> "网络异常（无法解析域名）"
        else -> "网络异常（${e?.javaClass?.simpleName ?: "未知"}）"
    }

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

    /** v8.47.0：最近一次加载失败的具体原因（供诊断与下载重试判断） */
    @Volatile
    private var lastLoadError: String? = null

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
            DebugLogManager.w(TAG, "插件 dex 不存在: ${dexFile.absolutePath}")
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
            lastLoadError = null

            Log.i(TAG, "插件加载成功: ${currentTokenizer.name()}")
            DebugLogManager.i(TAG, "插件加载成功: ${currentTokenizer.name()} | dex=${dexFile.length()} bytes")
            true
        } catch (e: Throwable) {
            // v8.47.0：捕获 Throwable（含 OOM），崩溃优雅回退而非直接崩溃
            lastLoadError = "加载失败[${e.javaClass.simpleName}]: ${e.message}"
            // 反射/包装异常（如 InvocationTargetException）根因在 cause 链，追加摘要便于定位
            val causeChain = buildString {
                var cur: Throwable? = e.cause
                var d = 0
                while (cur != null && d < 3) {
                    append(" <- ").append(cur.javaClass.simpleName).append(": ").append(cur.message)
                    cur = cur.cause
                    d++
                }
            }
            if (causeChain.isNotEmpty()) lastLoadError += causeChain
            Log.e(TAG, "插件加载失败，回退到内置分词器", e)
            DebugLogManager.e(TAG, "插件加载失败: $lastLoadError", e)
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
        DebugLogManager.i(TAG, "插件已卸载，回退到内置分词器")
    }

    /**
     * 下载并安装插件（多源自动切换：按延迟升序依次尝试官方源和用户镜像源）。
     * 下载中监控速率，卡住/异常自动中断并尝试下一个源；全部失败返回可读原因。
     * 切换源前会断开前一个连接；协程被取消时立即终止整个流程（不再继续换源）。
     *
     * @param onStage 阶段回调（stage, progress），progress 仅 DOWNLOADING 阶段有意义（0-100）
     * @param onSourceChanged 当前使用的源回调（prefix；空字符串=官方源）
     * @param onStatus 状态信息回调（中文，单行覆盖：尝试源→失败切换→下载成功→解压→加载→安装完成/失败原因）
     * @param onDownload 下载中回传（speedText, progress%），UI 单行显示速度与进度
     * @return [PluginInstallResult]
     */
    suspend fun downloadAndInstallPlugin(
        context: Context,
        onStage: (InstallStage, Int) -> Unit = { _, _ -> },
        onSourceChanged: (String) -> Unit = {},
        onStatus: (String) -> Unit = {},
        onDownload: (String, Int) -> Unit = { _, _ -> }
    ): PluginInstallResult = withContext(Dispatchers.IO) {
        val tmpZip = File(context.cacheDir, PLUGIN_ZIP_NAME)
        fun sourceName(prefix: String): String = if (prefix.isEmpty()) "官方" else prefix

        // 组装源列表：[官方] + 用户镜像
        // v8.47.2：并行测延迟（async/awaitAll），总耗时≈最慢源（上限 5s），顺序测最坏 5 源全超时 25s
        val prefixes = listOf(OFFICIAL_PREFIX) + getMirrorPrefixes(context)
        val latencies = coroutineScope {
            prefixes.map { p -> async { p to testLatency(p) } }.awaitAll().toMap()
        }
        val sorted = prefixes.sortedBy { latencies[it] ?: Long.MAX_VALUE }
        DebugLogManager.i(TAG, "开始安装插件，源列表（按延迟升序）: ${
            sorted.joinToString(", ") { (if (it.isEmpty()) "官方" else it) + "(${latencies[it] ?: -1}ms)" }
        }")

        var lastError = "未知错误"
        for (prefix in sorted) {
            var connection: HttpURLConnection? = null
            try {
                coroutineContext.ensureActive()
                // ---- 1. 下载（当前源）----
                onStage(InstallStage.DOWNLOADING, 0)
                onSourceChanged(prefix)
                onStatus("尝试 ${sourceName(prefix)} 源")
                connection = URL(buildDownloadUrl(prefix)).openConnection() as HttpURLConnection
                connection.connectTimeout = LATENCY_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                connection.connect()

                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    lastError = describeError(null, code)
                    onStatus("${sourceName(prefix)} 源失败（$lastError），切换下一镜像源")
                    Log.w(TAG, "源[$prefix]下载失败: $lastError")
                    DebugLogManager.w(TAG, "源[${sourceName(prefix)}]下载失败: $lastError")
                    continue
                }

                val totalLength = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = tmpZip.outputStream()
                val buffer = ByteArray(8192)
                var downloaded = 0
                var bytesRead: Int
                // 速度监控：连续 STALL_WINDOW_MS 内速率低于阈值视为卡住
                val t0 = System.currentTimeMillis()
                var lastCheckTime = t0
                var lastCheckBytes = 0L
                var lastEmitTime = t0
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    coroutineContext.ensureActive()
                    outputStream.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalLength > 0) {
                        onStage(InstallStage.DOWNLOADING, (downloaded * 100 / totalLength).toInt())
                    }
                    val now = System.currentTimeMillis()
                    // 每 500ms 回传平均速度 + 进度（UI 单行覆盖显示）
                    if (now - lastEmitTime >= 500) {
                        val elapsed = (now - t0).coerceAtLeast(1)
                        val speedBps = downloaded * 1000L / elapsed
                        val pct = if (totalLength > 0) (downloaded * 100 / totalLength).toInt() else 0
                        onDownload(formatSpeed(speedBps), pct)
                        lastEmitTime = now
                    }
                    if (now - lastCheckTime >= STALL_WINDOW_MS) {
                        val bps = (downloaded - lastCheckBytes) * 1000L / (now - lastCheckTime)
                        if (bps < STALL_THRESHOLD_BPS) {
                            throw java.net.SocketTimeoutException("下载卡住（网速过慢）")
                        }
                        lastCheckTime = now
                        lastCheckBytes = downloaded.toLong()
                    }
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                connection.disconnect()
                connection = null
                Log.i(TAG, "插件下载完成（源=$prefix）: $downloaded bytes")
                DebugLogManager.i(TAG, "下载完成（源=${sourceName(prefix)}）: $downloaded bytes, 期望 $totalLength")
                val mb = String.format(java.util.Locale.US, "%.1f MB", downloaded / 1024f / 1024f)
                onStatus("下载成功 $mb")

                // ---- 2. 解压 ----
                onStage(InstallStage.EXTRACTING, 0)
                onStatus("解压中")
                val pluginDir = getPluginDir(context)
                if (pluginDir.exists()) pluginDir.deleteRecursively()
                pluginDir.mkdirs()
                unzip(tmpZip, pluginDir)
                Log.i(TAG, "插件解压完成")
                DebugLogManager.i(TAG, "解压完成 -> ${pluginDir.absolutePath}")

                // ---- 3. 加载 ----
                onStage(InstallStage.LOADING, 0)
                onStatus("加载中")
                val loaded = loadPlugin(context)
                tmpZip.delete()
                if (loaded) {
                    DebugLogManager.i(TAG, "安装成功")
                    onStatus("安装完成")
                    return@withContext PluginInstallResult.Success
                } else {
                    // v8.47.2：解压成功即 zip 有效——加载失败是插件依赖/环境问题
                    // （如插件 dex 缺 kotlin-stdlib，release 版 app R8 裁剪后运行时 ClassNotFoundException），
                    // 换源重下同一 zip 毫无意义，直接报失败，不再反复下载。
                    val reason = lastLoadError ?: "插件解压成功但加载失败"
                    DebugLogManager.e(TAG, "插件加载失败（zip 有效，非源问题，不再切换源重试）: $reason")
                    onStatus("安装失败：$reason")
                    return@withContext PluginInstallResult.Failure(reason)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程被取消（用户中断/页面退出）：断开连接，立即终止整个流程，不再换源
                try { connection?.disconnect() } catch (_: Exception) { }
                throw e
            } catch (e: Exception) {
                try { connection?.disconnect() } catch (_: Exception) { }
                lastError = describeError(e)
                onStatus("${sourceName(prefix)} 源失败（$lastError），切换下一镜像源")
                Log.w(TAG, "源[$prefix]下载异常: $lastError")
                DebugLogManager.w(TAG, "源[${sourceName(prefix)}]下载异常: $lastError | ${e.javaClass.simpleName}: ${e.message}")
                try {
                    tmpZip.delete()
                    val dir = getPluginDir(context)
                    if (dir.exists()) dir.deleteRecursively()
                } catch (_: Exception) { }
            } finally {
                // 换源前确保前一个连接已断开（防止重复下载/连接泄漏）
                try { connection?.disconnect() } catch (_: Exception) { }
            }
        }
        onStatus("安装失败：$lastError")
        PluginInstallResult.Failure(lastError)
    }

    /** 下载速度格式化（B/s → MB/s） */
    private fun formatSpeed(bps: Long): String = when {
        bps >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB/s", bps / 1024f / 1024f)
        bps >= 1024L -> String.format(java.util.Locale.US, "%.1f KB/s", bps / 1024f)
        else -> "$bps B/s"
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
                DebugLogManager.i(TAG, "启动时自动加载插件成功")
            } else {
                DebugLogManager.w(TAG, "启动时自动加载插件失败: ${lastLoadError ?: "未知原因"}")
            }
        } else {
            DebugLogManager.i(TAG, "启动：未安装插件，使用内置分词器")
        }
    }
}
