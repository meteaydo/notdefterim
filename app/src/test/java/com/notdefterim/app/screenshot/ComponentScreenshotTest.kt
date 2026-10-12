package com.notdefterim.app.screenshot

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notdefterim.app.domain.model.ChecklistItem
import com.notdefterim.app.ui.home.SegmentedControl
import com.notdefterim.app.ui.notedetail.components.ChecklistEditor
import org.junit.Test

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
  fun yapilacaklarListesi_aramaVurgusu() = snapshotComponent {
    ChecklistEditor(
      items = SampleData.shoppingItems,
      searchQuery = "ekm",
      onItemChange = {}, onAddItem = {}, onRemoveItem = {},
      modifier = Modifier.width(379.dp)
    )
  }
}
