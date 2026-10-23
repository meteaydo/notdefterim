package com.notdefterim.app.screenshot

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notdefterim.app.domain.model.ChecklistItem
import com.notdefterim.app.domain.model.RepeatInterval
import com.notdefterim.app.ui.home.SegmentedControl
import com.notdefterim.app.ui.notedetail.components.ChecklistEditor
import com.notdefterim.app.ui.notedetail.components.ReminderSheetContent
import org.junit.Test
import java.time.DayOfWeek

class ComponentScreenshotTest : ScreenshotTest() {

  @Test
  fun sekmeler_notlarSecili() = snapshotComponent {
    SegmentedControl(selectedTab = 0, onTabSelected = {}, modifier = Modifier.width(280.dp))
  }

  @Test
  fun sekmeler_parolalarSecili() = snapshotComponent {
    SegmentedControl(selectedTab = 1, onTabSelected = {}, modifier = Modifier.width(280.dp))
  }

  @Test
  fun sekmeler_koyuTema() = snapshotComponent(darkTheme = true) {
    SegmentedControl(selectedTab = 0, onTabSelected = {}, modifier = Modifier.width(280.dp))
  }

  @Test
  fun yapilacaklarListesi() = snapshotComponent {
    ChecklistEditor(
      items = SampleData.shoppingItems + ChecklistItem(id = "c5", text = "Not: indirimli olanları al", hasCheckbox = false),
      onItemChange = {}, onAddItem = {}, onRemoveItem = {},
      modifier = Modifier.width(379.dp)
    )
  }

  @Test
  fun hatirlaticiPenceresi_yeni() = snapshotComponent {
    ReminderSheetContent(
      initialAt = null,
      initialInterval = RepeatInterval.NONE,
      initialDays = emptySet(),
      onSave = {}, onRemove = null,
      clock = { SampleData.date }
    )
  }

  @Test
  fun hatirlaticiPenceresi_belirliGunler() = snapshotComponent {
    // Cumartesi seçili ama yalnız Salı ve Perşembe istendi: ilk hatırlatma Salı'ya kayar
    ReminderSheetContent(
      initialAt = SampleData.date.plusDays(5).withHour(18).withMinute(0),
      initialInterval = RepeatInterval.DAYS_OF_WEEK,
      initialDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
      onSave = {}, onRemove = {},
      clock = { SampleData.date }
    )
  }

  @Test
  fun hatirlaticiPenceresi_gecmisZaman_koyuTema() = snapshotComponent(darkTheme = true) {
    ReminderSheetContent(
      initialAt = SampleData.date.minusHours(2),
      initialInterval = RepeatInterval.NONE,
      initialDays = emptySet(),
      onSave = {}, onRemove = {},
      clock = { SampleData.date }
    )
  }

  @Test
  fun yapilacaklarListesi_aramaVurgusu() = snapshotComponent {
    ChecklistEditor(
      items = SampleData.shoppingItems,
      searchQuery = "ekm",
      onItemChange = {}, onAddItem = {}, onRemoveItem = {},
      modifier = Modifier.width(379.dp)
    )
  }
}
