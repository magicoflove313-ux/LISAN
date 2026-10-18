package com.lisan.translator
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
object TranslationEngine {
 fun detect(text: String, done: (String?) -> Unit) {
  if (text.isBlank()) { done(null); return }
  LanguageIdentification.getClient().identifyLanguage(text).addOnSuccessListener { done(if (it == "und") null else it) }.addOnFailureListener { done(null) }
 }
 fun translate(text: String, source: String?, target: String, done: (String?,String?) -> Unit) {
  val targetCode = TranslateLanguage.fromLanguageTag(target)
  if (text.isBlank() || targetCode == null) { done(null,"اللغة غير مدعومة"); return }
  fun run(src: String?) {
   val sourceCode = src?.let { TranslateLanguage.fromLanguageTag(it) }
   if (sourceCode == null || sourceCode == targetCode) { done(if(sourceCode == targetCode) text else null, if(sourceCode == null) "تعذّر تحديد اللغة" else null); return }
   val translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(sourceCode).setTargetLanguage(targetCode).build())
   translator.downloadModelIfNeeded().addOnSuccessListener { translator.translate(text).addOnSuccessListener { result -> translator.close(); done(result,null) }.addOnFailureListener { translator.close(); done(null,"فشلت الترجمة؛ حاول مجددًا") } }.addOnFailureListener { translator.close(); done(null,"تعذّر تنزيل حزمة اللغة؛ تحقق من الإنترنت") }
  }
  if (source != null) run(source) else detect(text) { run(it) }
 }
}
