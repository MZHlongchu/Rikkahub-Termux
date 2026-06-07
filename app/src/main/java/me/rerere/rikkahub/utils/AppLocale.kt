package me.rerere.rikkahub.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit

private const val PREFERENCES_NAME = "rikkahub.preferences"
private const val APP_LANGUAGE_TAG_KEY = "app_language_tag"

fun Context.readCachedAppLanguageTag(): String {
    return getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(APP_LANGUAGE_TAG_KEY, "")
        .orEmpty()
}

fun Context.writeCachedAppLanguageTag(languageTag: String) {
    getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
        if (languageTag.isBlank()) {
            remove(APP_LANGUAGE_TAG_KEY)
        } else {
            putString(APP_LANGUAGE_TAG_KEY, languageTag)
        }
    }
}

fun Context.applyAppLanguage(languageTag: String) {
    writeCachedAppLanguageTag(languageTag)
    applyPlatformAppLanguage(languageTag)
    applyProcessDefaultLanguage(languageTag)
}

fun Context.applyPlatformAppLanguage(languageTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSystemService(LocaleManager::class.java).applicationLocales = localeListFor(languageTag)
    }
}

fun Context.withCachedAppLanguage(): Context {
    val languageTag = readCachedAppLanguageTag()
    applyProcessDefaultLanguage(languageTag)

    if (languageTag.isBlank()) return this
    val locales = localeListFor(languageTag)
    if (locales.size() == 0) return this

    val configuration = Configuration(resources.configuration).apply {
        setLocales(locales)
        setLayoutDirection(locales[0])
    }
    return createConfigurationContext(configuration)
}

private fun applyProcessDefaultLanguage(languageTag: String) {
    val locales = if (languageTag.isBlank()) {
        Resources.getSystem().configuration.locales
    } else {
        localeListFor(languageTag)
    }
    if (locales.size() > 0) {
        LocaleList.setDefault(locales)
    }
}

private fun localeListFor(languageTag: String): LocaleList {
    return if (languageTag.isBlank()) {
        LocaleList.getEmptyLocaleList()
    } else {
        LocaleList.forLanguageTags(languageTag)
    }
}
