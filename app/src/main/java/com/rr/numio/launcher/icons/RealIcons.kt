package com.rr.numio.launcher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.rr.numio.launcher.data.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The app's own icon, as the app designed it (used by the Sol theme).
 * Second-profile apps get Android's own work/clone badge, so they're easy to tell apart.
 * Drawn off the main thread once and kept in memory.
 */
object RealIcons {

    private val cache = object : LruCache<String, ImageBitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    private fun cacheKey(entry: AppEntry, px: Int) = "${entry.key}@$px"

    fun cached(entry: AppEntry, px: Int): ImageBitmap? = cache.get(cacheKey(entry, px))

    suspend fun load(context: Context, entry: AppEntry, px: Int): ImageBitmap? =
        cached(entry, px) ?: withContext(Dispatchers.Default) {
            render(context, entry, px)?.asImageBitmap()?.also { cache.put(cacheKey(entry, px), it) }
        }

    fun evictPackage(packageName: String) {
        val prefix = "$packageName/"
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    private fun render(context: Context, entry: AppEntry, px: Int): Bitmap? {
        val density = context.resources.displayMetrics.densityDpi
        val icon: Drawable = try {
            val raw = entry.info.getIcon(density)
            if (entry.isMainUser) raw else context.packageManager.getUserBadgedIcon(raw, entry.user)
        } catch (e: Exception) {
            return null
        }
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, px, px)
        icon.draw(Canvas(bmp))
        return bmp
    }
}
