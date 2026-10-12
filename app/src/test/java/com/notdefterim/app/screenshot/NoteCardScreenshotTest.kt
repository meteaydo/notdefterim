package com.notdefterim.app.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.ui.notelist.components.NoteCard
import org.junit.Test

class NoteCardScreenshotTest : ScreenshotTest() {

  @Test
  fun notIzgarasi() = snapshotComponent { NoteGrid(SampleData.notes) }

  @Test
  fun notIzgarasi_koyuTema() = snapshotComponent(darkTheme = true) { NoteGrid(SampleData.notes) }

  @Test
  fun vurguluVeDevreDisi() = snapshotComponent {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      NoteCard(
        note = SampleData.ideaNote,
        isHighlighted = true,
        onClick = {}, onLongClick = {}, onTogglePin = {},
        modifier = Modifier.width(CARD_WIDTH)
      )
      NoteCard(
        note = SampleData.ideaNote,
        isDisabled = true,
        onClick = {}, onLongClick = {}, onTogglePin = {},
        modifier = Modifier.width(CARD_WIDTH)
      )
    }
  }

  /** Uygulamadaki iki sütunlu not listesine benzer yerleşim. */
  @Composable
  private fun NoteGrid(notes: List<Note>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      listOf(notes.filterIndexed { i, _ -> i % 2 == 0 }, notes.filterIndexed { i, _ -> i % 2 == 1 })
        .forEach { columnNotes ->
          Column(
            modifier = Modifier.width(CARD_WIDTH),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            columnNotes.forEach { note ->
              NoteCard(note = note, onClick = {}, onLongClick = {}, onTogglePin = {})
            }
          }
        }
    }
  }

  private companion object {
    val CARD_WIDTH = 183.dp
  }
}
