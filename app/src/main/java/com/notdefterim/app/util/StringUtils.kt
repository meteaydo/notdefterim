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

/**
 * Arama eşleşmesi: Türkçe karakter ve büyük/küçük harf farkının yanında boşlukları da yok sayar.
 * Böylece "mete han" araması "metehan"ı, "metehan" araması da "mete han"ı bulur.
 */
fun String.matchesSearch(query: String): Boolean {
    val compactQuery = query.compactForSearch()
    return compactQuery.isEmpty() || compactForSearch().contains(compactQuery)
}

/**
 * [matchesSearch] kurallarıyla [query]'nin geçtiği yerleri bulur. Aralıklar boşluklar dahil
 * orijinal metnin indeksleridir; vurgulama ve kaydırma doğrudan bunlarla yapılabilir.
 */
fun String.searchMatchRanges(query: String): List<IntRange> {
    val compactQuery = query.compactForSearch()
    if (compactQuery.isEmpty()) return emptyList()

    // Sıkıştırılmış metindeki her karakterin orijinal metindeki indeksi
    val compactText = StringBuilder()
    val sourceIndex = ArrayList<Int>()
    forEachIndexed { index, char ->
        if (!char.isWhitespace()) {
            for (normalized in char.toString().normalizeForSearch()) {
                compactText.append(normalized)
                sourceIndex.add(index)
            }
        }
    }

    val ranges = mutableListOf<IntRange>()
    var start = compactText.indexOf(compactQuery)
    while (start >= 0) {
        val end = start + compactQuery.length - 1
        ranges.add(sourceIndex[start]..sourceIndex[end])
        start = compactText.indexOf(compactQuery, end + 1)
    }
    return ranges
}

private fun String.compactForSearch(): String =
    filterNot { it.isWhitespace() }.normalizeForSearch()
