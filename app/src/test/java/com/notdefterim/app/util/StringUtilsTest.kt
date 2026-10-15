package com.notdefterim.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringUtilsTest {

  @Test
  fun bosluklar_yok_sayilir() {
    assertTrue("Metehan Aydoğdu".matchesSearch("mete han"))
    assertTrue("Mete Han Aydoğdu".matchesSearch("metehan"))
    assertTrue("mete\nhan".matchesSearch("metehan"))
  }

  @Test
  fun turkce_karakter_ve_buyuk_kucuk_harf_yok_sayilir() {
    assertTrue("ŞİFRE ÇİZELGESİ".matchesSearch("sifre cizel"))
    assertTrue("ılık su".matchesSearch("ILIK"))
  }

  @Test
  fun eslesmeyen_arama() {
    assertFalse("Metehan".matchesSearch("mete hanı"))
  }

  @Test
  fun bos_arama_her_seyle_eslesir() {
    assertTrue("Metehan".matchesSearch("   "))
    assertEquals(emptyList<IntRange>(), "Metehan".searchMatchRanges("  "))
  }

  @Test
  fun aralik_orijinal_metindeki_boslugu_kapsar() {
    val text = "Ben Mete Han, sen metehan"
    assertEquals(listOf(4..11, 18..24), text.searchMatchRanges("metehan"))
    assertEquals("Mete Han", text.substring(4, 12))
  }

  @Test
  fun aranan_bosluklu_metin_bosluksuz_metinde_bulunur() {
    val text = "Hesap: metehan123"
    assertEquals(listOf(7..13), text.searchMatchRanges("mete han"))
  }
}
