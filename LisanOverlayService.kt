package com.lisan.translator

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/** Prototype bubble. It shows the latest accessible text; automatic translation is not wired yet. */
class LisanOverlayService : Service() {
    private var manager: WindowManager? = null
    private var bubble: TextView? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (bubble != null) return START_STICKY
        manager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubble = TextView(this).apply {
            text = "ل"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF3949AB.toInt())
            setPadding(18, 12, 18, 12)
            setOnClickListener { showLatestText() }
        }
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 20; params.y = 220
        try { manager?.addView(bubble, params) } catch (_: Exception) { stopSelf() }
        return START_STICKY
    }

    private fun showLatestText() {
        val text = getSharedPreferences("lisan_settings", MODE_PRIVATE).getString("latest_visible_text", "لم يُلتقط نص متاح") ?: "لم يُلتقط نص متاح"
        val card = TextView(this).apply {
            this.text = text.take(1200)
            textSize = 16f
            setPadding(22, 22, 22, 22)
            setTextColor(0xFF202020.toInt())
            setBackgroundColor(0xFFF5F5F5.toInt())
        }
        val p = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        p.gravity = Gravity.CENTER
        p.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        try {
            manager?.addView(card, p)
            card.postDelayed({ try { manager?.removeView(card) } catch (_: Exception) {} }, 8000)
        } catch (_: Exception) {}
    }
    private fun overlayType() = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
    override fun onDestroy() { try { bubble?.let { manager?.removeView(it) } } catch (_: Exception) {}; bubble = null; manager = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
