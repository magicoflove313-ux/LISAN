package com.lisan.translator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 24)
        }
        root.addView(TextView(this).apply {
            text = "لسان | LISAN\nمترجم المحادثات"
            textSize = 27f
            gravity = Gravity.CENTER
        }, matchWrap())
        root.addView(TextView(this).apply {
            text = "\nنسخة أولية مستقلة. فعّل الصلاحيات بنفسك.\nقراءة النصوص تعتمد على ما يسمح به كل تطبيق."
            textSize = 16f
            gravity = Gravity.CENTER
        }, matchWrap())
        addButton(root, "إعداد إمكانية الوصول") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        addButton(root, "السماح بالظهور فوق التطبيقات") {
            if (android.os.Build.VERSION.SDK_INT >= 23) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        addButton(root, "إعداد كيبورد لسان") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        addButton(root, "اختيار كيبورد لسان") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        addButton(root, "تشغيل الفقاعة") { startService(Intent(this, LisanOverlayService::class.java)) }
        setContentView(root)
    }
    private fun addButton(root: LinearLayout, label: String, action: () -> Unit) {
        root.addView(Button(this).apply { text = label; setOnClickListener { action() } },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 })
    }
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
