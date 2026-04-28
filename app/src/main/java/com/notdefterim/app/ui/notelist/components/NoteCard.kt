package com.notdefterim.app.ui.notelist.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.ui.theme.LocalNoteCardColors
import java.time.format.DateTimeFormatter

private val cardDateFormatter = DateTimeFormatter.ofPattern("d MMM")

/**
 * Not listesi kart bileşeni.
 *
 * [onClick] — not düzenleme için
 * [onLongClick] — silme/sabit sabitleme menüsü için
 */
@Composable
fun NoteCard(
  note: Note,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val noteCardColors = LocalNoteCardColors.current
  val cardColor = noteCardColors.colors.getOrElse(note.color.index) {
    MaterialTheme.colorScheme.surface
  }

  val animatedCardColor by animateColorAsState(
    targetValue = cardColor,
    animationSpec = tween(durationMillis = 300),
    label = "card_color_anim"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(animatedCardColor)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .padding(16.dp)
  ) {
    Column {
      // Başlık satırı + sabitleme ikonu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        if (note.title.isNotBlank()) {
          Text(
            text = note.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
        }

        if (note.isPinned) {
          Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = "Sabitlenmiş",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .size(16.dp)
              .padding(start = 4.dp)
          )
        }
      }

      // İçerik önizlemesi
      if (note.content.isNotBlank()) {
        if (note.title.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = note.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Tarih
      Text(
        text = note.updatedAt.format(cardDateFormatter),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    }
  }
}
