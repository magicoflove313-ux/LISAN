package com.lisan.translator

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Arabic compose keyboard. The draft stays inside the keyboard until the user inserts
 * either the original Arabic text or its translation into the focused app. */
class LisanInputMethodService : InputMethodService() {
    private lateinit var draft: EditText
    private lateinit var translatedView: TextView
    private lateinit var statusView: TextView
    private var translatedText: String = ""
    private var targetLanguage: String = "en"
    private var showingSymbols = false

    override fun onCreateInputView(): View {
        targetLanguage = Prefs.get(this, Prefs.TARGET_LANGUAGE, "en")
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(6), dp(7), dp(6))
            setBackgroundColor(Color.rgb(239, 242, 247))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "لسان  •  LISAN"
            textSize = 16f
            setTextColor(Color.rgb(35, 67, 115))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, dp(34), 1f))
        header.addView(smallButton("$targetLanguage ▾") { cycleLanguage() }, LinearLayout.LayoutParams(dp(90), dp(34)))
        header.addView(smallButton("إخفاء") { requestHideSelf(0) }, LinearLayout.LayoutParams(dp(64), dp(34)))
        root.addView(header)

        draft = EditText(this).apply {
            hint = "اكتب رسالتك بالعربية…"
            textSize = 18f
            minLines = 1
            maxLines = 2
            gravity = Gravity.TOP or Gravity.START
            textDirection = View.TEXT_DIRECTION_RTL
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setTextColor(Color.rgb(25, 30, 38))
            setHintTextColor(Color.GRAY)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(draft, LinearLayout.LayoutParams(-1, dp(46)))

        translatedView = TextView(this).apply {
            text = "الترجمة تظهر هنا"
            textSize = 15f
            setTextColor(Color.rgb(35, 67, 115))
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 2
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }
        root.addView(translatedView, LinearLayout.LayoutParams(-1, dp(34)))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("ترجمة") { translateDraft() }, weighted())
        actions.addView(actionButton("إدراج الترجمة") { insertText(translatedText, true) }, weighted())
        actions.addView(actionButton("إدراج الأصل") { insertText(draft.text.toString(), false) }, weighted())
        root.addView(actions, LinearLayout.LayoutParams(-1, dp(38)))

        statusView = TextView(this).apply {
            text = "اكتب ثم اختر ترجمة، أو اكتب الحروف وأدرج الأصل"
            textSize = 11f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            maxLines = 1
        }
        root.addView(statusView, LinearLayout.LayoutParams(-1, dp(18)))

        val keysArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        rebuildKeys(keysArea)

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(actionButton(if (showingSymbols) "أبجد" else "؟١٢") {
            showingSymbols = !showingSymbols
            refreshKeyboardKeys(keysArea)
        }, LinearLayout.LayoutParams(dp(62), dp(38)))
        bottom.addView(actionButton("،") { appendKey("،") }, LinearLayout.LayoutParams(dp(38), dp(38)))
        bottom.addView(actionButton("مسافة") { appendKey(" ") }, LinearLayout.LayoutParams(0, dp(38), 2f))
        bottom.addView(actionButton("سطر جديد") { appendKey("\n") }, LinearLayout.LayoutParams(0, dp(38), 1f))
        bottom.addView(actionButton("إدراج") { insertText(draft.text.toString(), false) }, LinearLayout.LayoutParams(0, dp(38), 1f))
        root.addView(keysArea)
        root.addView(bottom)
        autoDetectCounterpartLanguage()
        return ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
    }

    private fun refreshKeyboardKeys(container: LinearLayout) = rebuildKeys(container)

    private fun rebuildKeys(container: LinearLayout) {
        container.removeAllViews()
        val rows = if (showingSymbols) listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("@", "#", "٪", "&", "-", "+", "(", ")", "/", "؟"),
            listOf(".", "،", "!", ":", ";", "'", "\"", "…")
        ) else listOf(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د"),
            listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"),
            listOf("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ")
        )
        rows.forEachIndexed { index, keys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            keys.forEach { key ->
                row.addView(keyButton(key) { appendKey(key) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            if (index == rows.lastIndex) {
                row.addView(keyButton("⌫") { deleteDraftChar() }, LinearLayout.LayoutParams(dp(46), dp(38)).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            container.addView(row)
        }
    }

    private fun appendKey(value: String) {
        val start = draft.selectionStart.coerceAtLeast(0)
        val end = draft.selectionEnd.coerceAtLeast(0)
        draft.text.replace(minOf(start, end), maxOf(start, end), value)
        draft.setSelection((minOf(start, end) + value.length).coerceAtMost(draft.length()))
        translatedText = ""
        translatedView.text = "الترجمة تظهر هنا"
    }

    private fun deleteDraftChar() {
        val start = draft.selectionStart.coerceAtLeast(0)
        val end = draft.selectionEnd.coerceAtLeast(0)
        if (start != end) {
            draft.text.delete(minOf(start, end), maxOf(start, end))
            draft.setSelection(minOf(start, end))
        } else if (start > 0) {
            draft.text.delete(start - 1, start)
            draft.setSelection(start - 1)
        }
    }

    private fun translateDraft() {
        val text = draft.text.toString().trim()
        if (text.isEmpty()) {
            statusView.text = "اكتب الرسالة أولًا"
            return
        }
        targetLanguage = Prefs.get(this, Prefs.TARGET_LANGUAGE, targetLanguage)
        statusView.text = "جارٍ ترجمة النص…"
        TranslationEngine.translate(text, "ar", targetLanguage) { result, error ->
            if (result != null) {
                translatedText = result
                translatedView.text = result
                statusView.text = "جاهزة للإدراج • $targetLanguage"
            } else {
                statusView.text = error ?: "تعذرت الترجمة؛ تحقق من الاتصال واللغة"
            }
        }
    }

    private fun insertText(text: String, isTranslation: Boolean) {
        if (text.isBlank()) {
            statusView.text = if (isTranslation) "اضغط ترجمة أولًا" else "اكتب النص أولًا"
            return
        }
        val connection = currentInputConnection
        if (connection == null) {
            statusView.text = "اضغط داخل مربع الرسالة أولًا"
            return
        }
        connection.commitText(text, 1)
        draft.setText("")
        translatedText = ""
        translatedView.text = "الترجمة تظهر هنا"
        statusView.text = "أُدرج النص في مربع الرسالة؛ أرسل عندما تكون جاهزًا"
    }

    private fun autoDetectCounterpartLanguage() {
        val recentText = getSharedPreferences("lisan_settings", MODE_PRIVATE)
            .getString("latest_visible_text", "").orEmpty().trim()
        if (recentText.length < 3) return
        TranslationEngine.detect(recentText) { detected ->
            if (detected != null && detected != "ar" &&
                com.google.mlkit.nl.translate.TranslateLanguage.fromLanguageTag(detected) != null) {
                targetLanguage = detected
                Prefs.put(this, Prefs.TARGET_LANGUAGE, detected)
                if (::statusView.isInitialized) statusView.text = "اكتشفنا لغة الطرف الآخر: $detected"
            }
        }
    }

    private fun cycleLanguage() {
        val languages = listOf("en", "hi", "ur", "tr", "fr", "de", "es", "zh", "ja", "fa")
        val index = languages.indexOf(targetLanguage).coerceAtLeast(0)
        targetLanguage = languages[(index + 1) % languages.size]
        Prefs.put(this, Prefs.TARGET_LANGUAGE, targetLanguage)
        statusView.text = "لغة الترجمة: $targetLanguage"
    }

    private fun keyButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = if (label.length > 1) 13f else 18f
        isAllCaps = false
        setTextColor(Color.rgb(30, 38, 50))
        setBackgroundColor(Color.WHITE)
        setPadding(0, 0, 0, 0)
        setOnClickListener { action() }
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        setPadding(dp(2), 0, dp(2), 0)
        setOnClickListener { action() }
    }

    private fun smallButton(label: String, action: () -> Unit) = actionButton(label, action)
    private fun weighted() = LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(2), 0, dp(2), 0) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
