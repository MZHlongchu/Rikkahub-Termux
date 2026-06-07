package me.rerere.rikkahub.data.datastore

enum class AppLanguage(val languageTag: String) {
    SYSTEM(""),
    ENGLISH("en-US"),
    CHINESE_SIMPLIFIED("zh"),
    CHINESE_TRADITIONAL("zh-TW"),
    JAPANESE("ja"),
    KOREAN("ko-KR"),
    RUSSIAN("ru");

    companion object {
        fun fromLanguageTag(languageTag: String): AppLanguage {
            return entries.firstOrNull { it.languageTag == languageTag } ?: SYSTEM
        }
    }
}
