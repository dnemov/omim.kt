package com.mapswithme.maps.editor.data

// Corresponds to StringUtf8Multilang::Lang in core.
class Language(val code: String, val name: String) {
    val isDefaultLang: Boolean
        get() = code == DEFAULT_LANG_CODE

    companion object {
        // StringUtf8Multilang::GetLangByCode(StringUtf8Multilang::kDefaultCode).
        const val DEFAULT_LANG_CODE = "default"
    }

}