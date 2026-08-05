package com.alijafari.red.astronomy.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    /**
     * Sets the application locale for the given context and returns a new Context with the updated configuration.
     *
     * @param context The base context.
     * @param languageCode Language code ("fa" for Persian, "en" for English).
     * @return Context wrapped with updated configuration locale.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale)
        }

        // Apply per-app language preferences for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
            } catch (e: Exception) {
                // Fallback handled via configuration context
            }
        }

        return context.createConfigurationContext(config)
    }

    /**
     * Helper to get current language tag from locale
     */
    fun getLanguageCode(language: com.alijafari.red.astronomy.domain.AppLanguage): String {
        return when (language) {
            com.alijafari.red.astronomy.domain.AppLanguage.PERSIAN -> "fa"
            com.alijafari.red.astronomy.domain.AppLanguage.ENGLISH -> "en"
        }
    }
}

fun String.toPersianDigits(): String {
    val latinDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    var result = this
    for (i in 0..9) {
        result = result.replace(latinDigits[i], persianDigits[i])
    }
    return result
}

