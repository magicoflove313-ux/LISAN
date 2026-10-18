package com.lisan.translator

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Prototype only: captures text exposed through Android Accessibility APIs. */
class LisanAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences("lisan_settings", MODE_PRIVATE)
        if (!prefs.getBoolean("screen_translation_enabled", true)) return
        val texts = linkedSetOf<String>()
        event.text?.forEach { value -> value?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(texts::add) }
        rootInActiveWindow?.let { root ->
            collect(root, texts, 0)
            root.recycle()
        }
        val latest = texts.joinToString("\n").take(5000)
        if (latest.isNotBlank()) prefs.edit().putString("latest_visible_text", latest).apply()
    }
    private fun collect(node: AccessibilityNodeInfo, out: MutableSet<String>, depth: Int) {
        if (depth > 16 || out.size > 120) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collect(child, out, depth + 1)
            child.recycle()
        }
    }
    override fun onInterrupt() = Unit
}
