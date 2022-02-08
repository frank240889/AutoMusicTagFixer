package mx.dev.franco.automusictagfixer.utilities

import com.gracenote.gnsdk.GnImageSize
import com.gracenote.gnsdk.GnLanguage

/**
 * Created by franco on 19/05/17.
 */
/**
 * Helper class to store the values og¡f ImageSize from settings.
 */
object Settings {
    //Determine the size of downloaded cover art, default value is not download cover art.
    @Volatile
    var SETTING_SIZE_ALBUM_ART = GnImageSize.kImageSizeUnknown

    @Volatile
    var SETTING_LANGUAGE = GnLanguage.kLanguageSpanish
    fun setValueImageSize(preferenceSaved: String?): GnImageSize? {
        var size: GnImageSize
        when (preferenceSaved) {
            "-1" -> return null
            "0" -> return GnImageSize.kImageSizeThumbnail
            "1" -> return GnImageSize.kImageSizeSmall
            "5" -> return GnImageSize.kImageSizeMedium
            "7" -> return GnImageSize.kImageSize720
            "10" -> return GnImageSize.kImageSize1080
            "1000" -> return GnImageSize.kImageSizeXLarge
        }
        return GnImageSize.kImageSize1080
    }

    fun setValueLanguage(preferenceSaved: String?): GnLanguage {
        val language: GnLanguage? = null
        when (preferenceSaved) {
            "0" -> return GnLanguage.kLanguageSpanish
            "1" -> return GnLanguage.kLanguageEnglish
            "2" -> return GnLanguage.kLanguageGerman
            "3" -> return GnLanguage.kLanguageFrench
            "4" -> return GnLanguage.kLanguageItalian
            "5" -> return GnLanguage.kLanguagePortuguese
            "6" -> return GnLanguage.kLanguageRussian
            "7" -> return GnLanguage.kLanguageChineseTraditional
            "8" -> return GnLanguage.kLanguageJapanese
            "9" -> return GnLanguage.kLanguageKorean
        }
        return GnLanguage.kLanguageSpanish
    }
}