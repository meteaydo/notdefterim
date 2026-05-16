package com.notdefterim.app.util

import java.util.Locale

/**
 * Türkçe karakterleri (büyük/küçük dahil) İngilizce eşdeğerlerine çevirerek
 * ve tüm harfleri küçülterek aramaya uygun hale getiren yardımcı fonksiyon.
 */
fun String.normalizeForSearch(): String {
    return this.replace("İ", "i")
        .replace("I", "i")
        .replace("ı", "i")
        .replace("Ç", "c")
        .replace("ç", "c")
        .replace("Ş", "s")
        .replace("ş", "s")
        .replace("Ğ", "g")
        .replace("ğ", "g")
        .replace("Ü", "u")
        .replace("ü", "u")
        .replace("Ö", "o")
        .replace("ö", "o")
        .lowercase(Locale.ENGLISH)
}
