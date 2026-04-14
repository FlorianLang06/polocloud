package de.polocloud.common.extension

import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.api.trInfo
import org.slf4j.Logger

fun Logger.trInfoWithSubline(pack: String, key: String, sublineKey: String, vararg placeholders: Pair<String, Any?>) {
    trInfo(pack, key, *placeholders)
    info("%color{${TranslationService.tr(pack, sublineKey, *placeholders)}}{light_gray}")
}

fun Logger.trInfoWithSubline(pack: String, key: String, sublinePack: String, sublineKey: String, vararg placeholders: Pair<String, Any?>) {
    trInfo(pack, key, *placeholders)
    info("%color{${TranslationService.tr(sublinePack, sublineKey, *placeholders)}}{light_gray}")
}