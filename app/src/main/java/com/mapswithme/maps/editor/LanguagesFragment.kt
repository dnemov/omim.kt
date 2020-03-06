package com.mapswithme.maps.editor

import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.editor.data.Language
import java.util.*

class LanguagesFragment : BaseMwmRecyclerFragment<LanguagesAdapter?>() {
    interface Listener {
        fun onLanguageSelected(language: Language)
    }

    override fun createAdapter(): LanguagesAdapter {
        val args = arguments
        val existingLanguages: Set<String> =
            HashSet(args!!.getStringArrayList(EXISTING_LOCALIZED_NAMES))
        val languages: MutableList<Language> =
            ArrayList()
        for (lang in Editor.nativeGetSupportedLanguages()) {
            if (!existingLanguages.contains(lang!!.code)) languages.add(lang)
        }
        Collections.sort(
            languages,
            kotlin.Comparator { o1, o2 -> if (o1!!.isDefaultLang && !o2!!.isDefaultLang) return@Comparator 1
                if (!o1.isDefaultLang && o2!!.isDefaultLang) -1 else o1.name.compareTo(o2!!.name) }
            )
        return LanguagesAdapter(this, languages.toTypedArray())
    }

    fun onLanguageSelected(language: Language) {
        if (parentFragment is Listener) (parentFragment as Listener?)!!.onLanguageSelected(
            language
        )
    }

    companion object {
        const val EXISTING_LOCALIZED_NAMES = "ExistingLocalizedNames"
    }
}