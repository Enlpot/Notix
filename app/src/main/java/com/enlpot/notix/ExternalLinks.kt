package com.enlpot.notix

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Single place external URLs are handed to the system. A device without a browser (or a
 * restricted profile) throws from startActivity; callers get `false` instead of a crash,
 * so they can avoid committing state changes — e.g. the community-share nudge is only
 * marked handled after a successful handoff. Only the launch failures startActivity is
 * documented to throw are caught; anything else is a genuine bug and propagates.
 *
 * v7.24：不再在工具类内弹系统 Toast——UI 层提示由调用方以应用内 Snackbar 等展示。
 */
object ExternalLinks {
    fun open(context: Context, url: String): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
