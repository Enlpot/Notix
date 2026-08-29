package com.enlpot.notix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * v7.9 通知卡片配色引擎（NotificationColorEngine）
 *
 * 核心链路：App Icon → 主色提取(聚类) → HSL 生成背景 → WCAG 2.2 对比度 → 自动黑白文字
 *
 * 设计原则：
 * - App 图标颜色只用于保持品牌识别，不直接作为通知背景色；
 * - 全部颜色由算法动态生成，禁止写死品牌色；
 * - 可读性优先于品牌还原度（正文 ≥4.5:1，尽量 ≥7:1）；
 * - 禁止 RGB 全量平均；渐变/多色图标选视觉权重高的主色；
 * - 颜色缓存以 packageName + 图标版本(lastUpdateTime) 为 Key，避免每次刷新重复分析；
 * - 分析在调用方（后台协程）执行，禁止主线程做大量 Bitmap 分析。
 *
 * v8.18 低优先级评估：Android 原生 Palette API（androidx.palette）对比
 * - 当前手写 64 桶聚类：24x24 采样，视觉权重优先，经过多轮调优，单次分析通常 <10ms；
 * - Palette API：支持 Vibrant/Muted/DarkVibrant/LightVibrant 多色板，代码量更少，
 *   但取色策略偏向"有活力"颜色，与当前"视觉权重最高"策略不同，替换需重新调优；
 * - 决策：保留手写实现，通过 debug 耗时日志监控性能；若单次分析 >50ms 或需要多色板时再迁移。
 */
data class NotificationColors(
    val backgroundColor: Int,
    val primaryColor: Int,
    val secondaryColor: Int?,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val tertiaryTextColor: Int,
    val accentColor: Int,
    val contrastRatio: Float
)

object NotificationColorEngine {

    // WCAG 2.2 对比度目标
    private const val MIN_CONTRAST_BODY = 4.5f
    private const val PREFERRED_CONTRAST = 7.0f
    private const val MAX_CACHE_SIZE = 256

    private const val TAG = "NotificationColorEngine"

    // 主色提取采样尺寸（小图分析，降低计算成本）
    private const val SAMPLE_SIZE = 24

    private val cache = ConcurrentHashMap<String, NotificationColors>()

    // v8.18：深浅主题适配——由调用方在主题切换时设置，影响目标背景亮度
    var isDarkTheme: Boolean = true

    /**
     * 主入口：根据 App 图标生成整套通知卡片配色。
     * 内部自动加载图标并按 packageName + lastUpdateTime 缓存。
     * 可在后台协程中调用；线程安全（缓存为 ConcurrentHashMap）。
     */
    fun getNotificationColors(context: Context, packageName: String?): NotificationColors {
        // v8.18 低优先级：取色耗时监控（仅 debug），用于评估是否需要切换到 Palette API
        val startTime = if (BuildConfig.DEBUG) System.currentTimeMillis() else 0L
        if (packageName == null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Neutral fallback: packageName is null")
            return hashFallbackColors(null)
        }
        val key = buildCacheKey(context, packageName)
        cache[key]?.let { return it }
        val icon = loadAppIcon(context, packageName)
        if (icon == null) return hashFallbackColors(packageName)
        val colors = compute(icon, packageName)
        if (BuildConfig.DEBUG) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Color analysis for $packageName took ${elapsed}ms (cache miss)")
        }
        // v8.18 优化：满时分批淘汰最旧的 1/4，避免全清导致的集中性能抖动
        if (cache.size >= MAX_CACHE_SIZE) {
            val target = MAX_CACHE_SIZE / 4
            val iterator = cache.keys.iterator()
            var removed = 0
            while (iterator.hasNext() && removed < target) {
                iterator.next()
                iterator.remove()
                removed++
            }
        }
        cache[key] = colors
        return colors
    }

    /** 清除颜色缓存（应用升级/主题切换时调用） */
    fun clearCache() {
        cache.clear()
    }

    /** 基于实际 WCAG 对比度选择黑/白文字（禁止简单按亮度判断） */
    fun chooseTextColor(bg: Int): Int =
        if (contrastRatio(Color.WHITE, bg) >= contrastRatio(Color.BLACK, bg)) Color.WHITE else Color.BLACK

    /** WCAG 2.2 对比度 */
    fun contrastRatio(fg: Int, bg: Int): Float {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    // ------------------------------------------------------------------
    // 图标加载与缓存
    // ------------------------------------------------------------------

    private fun buildCacheKey(context: Context, packageName: String): String {
        return try {
            val version = context.packageManager.getPackageInfo(packageName, 0).lastUpdateTime
            "$packageName#$version"
        } catch (_: Exception) {
            "$packageName#0"
        }
    }

    private fun loadAppIcon(context: Context, packageName: String): Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val w = drawable.intrinsicWidth.coerceAtLeast(1)
                val h = drawable.intrinsicHeight.coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                bmp
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Neutral fallback: icon not found for $packageName: ${e.message}")
            null
        }
    }

    // ------------------------------------------------------------------
    // 颜色生成主流程
    // ------------------------------------------------------------------

    private fun compute(icon: Bitmap?, packageName: String?): NotificationColors {
        val extracted = extractPrimaryColor(icon)
        val primary = extracted.primary

        // 无主色（单色/纯黑白图标，聚类失败）→ 哈希兜底色（取代中性灰）
        if (primary == null) {
            if (icon != null) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Neutral fallback: no dominant color for $packageName (monochrome icon?)")
            }
            return hashFallbackColors(packageName)
        }

        // 主色为黑白灰：尝试第二主色；没有则哈希兜底色（禁止白底白字/黑底黑字）
        if (isGrayish(primary)) {
            val secondary = extracted.secondary
            if (secondary != null && !isGrayish(secondary)) {
                return buildColors(secondary, primary)
            }
            if (BuildConfig.DEBUG) Log.w(TAG, "Neutral fallback: no dominant color for $packageName (monochrome icon?)")
            return hashFallbackColors(packageName)
        }

        val secondary = extracted.secondary?.takeUnless { isGrayish(it) }
        return buildColors(primary, secondary)
    }

    private fun buildColors(primary: Int, secondary: Int?): NotificationColors {
        val bg = generateBackgroundWithContrast(primary)
        val text = chooseTextColor(bg)
        val crWhite = contrastRatio(Color.WHITE, bg)
        val crBlack = contrastRatio(Color.BLACK, bg)
        val accent = buildAccent(primary)
        // v8.18：三级文字色——主文字纯黑/白，正文 0.85 alpha，辅助 0.60 alpha
        val textSecondary = withAlpha(text, 0.85f)
        val textTertiary = withAlpha(text, 0.60f)
        return NotificationColors(
            backgroundColor = bg,
            primaryColor = primary,
            secondaryColor = secondary,
            primaryTextColor = text,
            secondaryTextColor = textSecondary,
            tertiaryTextColor = textTertiary,
            accentColor = accent,
            contrastRatio = maxOf(crWhite, crBlack)
        )
    }

    /** 给 Int 颜色设置透明度（0-1） */
    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * 哈希兜底色：图标不可解析 / 单色 / packageName 为空时，用 packageName 哈希生成
     * 确定性色相（0–360°），固定柔和饱和度与明度，再迭代到 WCAG ≥4.5:1。
     * 同一 packageName 永远返回同一颜色；packageName 为 null 时用 "unknown" 兜底。
     * 取代原固定中性灰 NEUTRAL_BG，保证第三方 App 即使无彩色图标也能拿到稳定可辨识的颜色。
     */
    private fun hashFallbackColors(packageName: String?): NotificationColors {
        val seed = packageName ?: "unknown"
        val hue = ((seed.hashCode().toLong() and 0x7FFFFFFF) % 360).toFloat()
        val s = 0.42f
        var l = 0.58f
        var bg = hslToRgb(hue, s, l)
        repeat(12) {
            val crW = contrastRatio(Color.WHITE, bg)
            val crB = contrastRatio(Color.BLACK, bg)
            if (maxOf(crW, crB) >= MIN_CONTRAST_BODY) return@repeat
            l = if (crW >= crB) (l - 0.05f).coerceAtLeast(0.20f) else (l + 0.05f).coerceAtMost(0.85f)
            bg = hslToRgb(hue, s, l)
        }
        val text = chooseTextColor(bg)
        val accent = hslToRgb(hue, 0.55f, (l + 0.14f).coerceAtMost(0.85f))
        val crWhite = contrastRatio(Color.WHITE, bg)
        val crBlack = contrastRatio(Color.BLACK, bg)
        return NotificationColors(
            backgroundColor = bg,
            primaryColor = bg,
            secondaryColor = null,
            primaryTextColor = text,
            secondaryTextColor = withAlpha(text, 0.85f),
            tertiaryTextColor = withAlpha(text, 0.60f),
            accentColor = accent,
            contrastRatio = maxOf(crWhite, crBlack)
        )
    }

    // ------------------------------------------------------------------
    // 主色提取（聚类，禁止全图 RGB 平均）
    // ------------------------------------------------------------------

    private class ExtractedColors(val primary: Int?, val secondary: Int?)

    private fun extractPrimaryColor(icon: Bitmap?): ExtractedColors {
        if (icon == null) return ExtractedColors(null, null)
        return try {
            val scaled = Bitmap.createScaledBitmap(icon, SAMPLE_SIZE, SAMPLE_SIZE, true)
            // 量化桶：每通道 4bit → 64 桶；桶内累加，最后取桶内平均（聚类主色，非全局平均）
            val buckets = HashMap<Int, LongArray>()
            for (x in 0 until scaled.width) {
                for (y in 0 until scaled.height) {
                    val c = scaled.getPixel(x, y)
                    if (Color.alpha(c) < 128) continue // 去透明
                    val r = Color.red(c)
                    val g = Color.green(c)
                    val b = Color.blue(c)
                    val key = (r shr 6) shl 4 or ((g shr 6) shl 2) or (b shr 6)
                    val arr = buckets.getOrPut(key) { LongArray(4) }
                    arr[0] += r
                    arr[1] += g
                    arr[2] += b
                    arr[3] += 1
                }
            }
            // 排除接近纯白/纯黑的无意义桶
            val meaningful = buckets.mapNotNull { (_, arr) ->
                val n = arr[3]
                if (n == 0L) return@mapNotNull null
                val argb = Color.rgb((arr[0] / n).toInt(), (arr[1] / n).toInt(), (arr[2] / n).toInt())
                if (!isMeaningful(argb)) null else argb
            }
            if (meaningful.isEmpty()) return ExtractedColors(null, null)

            // 按桶权重排序：取最高权重桶作为主色（视觉权重优先）
            val sorted = meaningful.sortedByDescending { c -> bucketWeight(buckets, c) }
            val primary = sorted[0]

            // 第二主色：与主色色相差 ≥25°，或同色相但亮度差明显（双色/渐变）
            var secondary: Int? = null
            if (sorted.size >= 2) {
                val hp = hueOf(primary)
                for (i in 1 until sorted.size) {
                    val cand = sorted[i]
                    val hc = hueOf(cand)
                    if (hueDistance(hp, hc) >= 25f) {
                        secondary = cand
                        break
                    }
                    if (kotlin.math.abs(relativeLuminance(cand) - relativeLuminance(primary)) > 0.15f) {
                        secondary = cand
                        break
                    }
                }
            }
            ExtractedColors(primary, secondary)
        } catch (_: Exception) {
            ExtractedColors(null, null)
        }
    }

    private fun bucketWeight(buckets: HashMap<Int, LongArray>, argb: Int): Long {
        val key = ((Color.red(argb) shr 6) shl 4) or ((Color.green(argb) shr 6) shl 2) or (Color.blue(argb) shr 6)
        return buckets[key]?.get(3) ?: 0L
    }

    /** 排除近黑/近白（无意义颜色） */
    private fun isMeaningful(argb: Int): Boolean {
        val hsv = FloatArray(3)
        Color.colorToHSV(argb, hsv)
        val s = hsv[1]
        val v = hsv[2]
        if (v < 0.10f) return false // 近黑
        if (v > 0.93f && s < 0.20f) return false // 近白
        return true
    }

    /** 是否黑白灰（低饱和无品牌色） */
    private fun isGrayish(argb: Int): Boolean {
        val hsv = FloatArray(3)
        Color.colorToHSV(argb, hsv)
        return hsv[1] < 0.12f
    }

    // ------------------------------------------------------------------
    // 背景生成（HSL 调整 + 对比度达标迭代）
    // ------------------------------------------------------------------

    private fun generateBackgroundWithContrast(primary: Int): Int {
        val hsl = rgbToHsl(primary)
        var l = targetLightness(hsl.l)
        var s = targetSaturation(hsl.s)
        var bg = hslToRgb(hsl.h, s, l)

        // 若最佳对比度仍不足 4.5:1，继续调整亮度直到达标
        repeat(10) {
            val crW = contrastRatio(Color.WHITE, bg)
            val crB = contrastRatio(Color.BLACK, bg)
            if (maxOf(crW, crB) >= MIN_CONTRAST_BODY) {
                return bg
            }
            // 白字优先则进一步压暗，黑字优先则提亮
            l = if (crW >= crB) {
                (l - 0.06f).coerceAtLeast(0.04f)
            } else {
                (l + 0.06f).coerceAtMost(0.96f)
            }
            bg = hslToRgb(hsl.h, s, l)
        }
        return bg
    }

    /** 目标背景亮度：深色主题偏暗（0.30-0.38），浅色主题提亮（0.45-0.52） */
    private fun targetLightness(l: Float): Float = if (isDarkTheme) {
        when {
            l > 0.80f -> 0.30f
            l > 0.60f -> 0.34f
            l > 0.40f -> 0.38f
            l > 0.20f -> 0.36f
            else -> 0.30f
        }
    } else {
        when {
            l > 0.80f -> 0.45f
            l > 0.60f -> 0.48f
            l > 0.40f -> 0.52f
            l > 0.20f -> 0.50f
            else -> 0.45f
        }
    }

    /** 目标背景饱和度：太鲜艳降低，太灰小幅提高，保持品牌色相 */
    private fun targetSaturation(s: Float): Float = when {
        s > 0.85f -> 0.60f
        s > 0.50f -> 0.55f
        s > 0.20f -> 0.45f
        else -> 0.35f
    }

    /** 强调色（色条/角标）：品牌色明亮版，保持色相，用于装饰而非文字承载 */
    private fun buildAccent(primary: Int): Int {
        val hsl = rgbToHsl(primary)
        val l = when {
            hsl.l > 0.75f -> 0.55f
            hsl.l < 0.15f -> 0.45f
            else -> (hsl.l * 1.35f).coerceIn(0.42f, 0.62f)
        }
        return hslToRgb(hsl.h, 0.68f, l)
    }

    // ------------------------------------------------------------------
    // 颜色空间工具（HSL / WCAG 亮度）
    // ------------------------------------------------------------------

    /** WCAG 相对亮度（sRGB → 线性） */
    private fun relativeLuminance(argb: Int): Float {
        fun lin(c: Int): Float {
            val v = c / 255f
            return if (v <= 0.04045f) v / 12.92f
            else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }
        val r = lin(Color.red(argb))
        val g = lin(Color.green(argb))
        val b = lin(Color.blue(argb))
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun rgbToHsl(argb: Int): HslColor {
        val r = Color.red(argb) / 255f
        val g = Color.green(argb) / 255f
        val b = Color.blue(argb) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        var h = 0f
        var s = 0f
        if (max != min) {
            val d = max - min
            s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            h = when (max) {
                r -> (g - b) / d + (if (g < b) 6f else 0f)
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            h *= 60f
        }
        return HslColor(h, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Int {
        if (s <= 0f) {
            val v = (l * 255f).toInt()
            return Color.rgb(v, v, v)
        }
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        val hk = ((h % 360f) + 360f) % 360f / 360f
        fun hue2rgb(t0: Float): Float {
            var t = t0
            if (t < 0f) t += 1f
            if (t > 1f) t -= 1f
            return when {
                t < 1f / 6f -> p + (q - p) * 6f * t
                t < 1f / 2f -> q
                t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                else -> p
            }
        }
        val r = (hue2rgb(hk + 1f / 3f) * 255f).toInt()
        val g = (hue2rgb(hk) * 255f).toInt()
        val b = (hue2rgb(hk - 1f / 3f) * 255f).toInt()
        return Color.rgb(r, g, b)
    }

    private fun hueOf(argb: Int): Float = rgbToHsl(argb).h

    private fun hueDistance(a: Float, b: Float): Float {
        val d = Math.abs(a - b)
        return if (d > 180f) 360f - d else d
    }
}

/** HSL 颜色（H: 0-360°, S/L: 0-1） */
private data class HslColor(val h: Float, val s: Float, val l: Float)
