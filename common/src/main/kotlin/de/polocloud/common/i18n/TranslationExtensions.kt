package de.polocloud.common.i18n

import de.polocloud.i18n.api.TranslationService
import org.slf4j.Logger
import java.util.Locale

fun Logger.trInfo(pack: String, key: String, vararg placeholders: Pair<String, Any?>) =
    info(TranslationService.tr(pack, key, *placeholders))

fun Logger.trWarn(pack: String, key: String, vararg placeholders: Pair<String, Any?>) =
    warn(TranslationService.tr(pack, key, *placeholders))

fun Logger.trError(pack: String, key: String, vararg placeholders: Pair<String, Any?>) =
    error(TranslationService.tr(pack, key, *placeholders))

fun Logger.trError(pack: String, key: String, exception: Exception, vararg placeholders: Pair<String, Any?>) =
    error(TranslationService.tr(pack, key, *placeholders), exception)

fun Logger.trDebug(pack: String, key: String, vararg placeholders: Pair<String, Any?>) =
    debug(TranslationService.tr(pack, key, *placeholders))


fun Logger.trInfo(pack: String, language: Locale, key: String, vararg placeholders: Pair<String, Any?>) =
    info(TranslationService.tr(pack, language, key, *placeholders))

fun Logger.trWarn(pack: String, language: Locale, key: String, vararg placeholders: Pair<String, Any?>) =
    warn(TranslationService.tr(pack, language, key, *placeholders))

fun Logger.trError(pack: String, language: Locale, key: String, vararg placeholders: Pair<String, Any?>) =
    error(TranslationService.tr(pack, language, key, *placeholders))

fun Logger.trDebug(pack: String, language: Locale, key: String, vararg placeholders: Pair<String, Any?>) =
    debug(TranslationService.tr(pack, language, key, *placeholders))