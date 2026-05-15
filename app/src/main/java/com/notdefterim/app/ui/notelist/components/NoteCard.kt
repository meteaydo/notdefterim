package com.notdefterim.app.ui.notelist.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Alarm
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
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.ui.theme.LocalNoteCardColors
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import com.notdefterim.app.R

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
  isDisabled: Boolean = false,
  isHighlighted: Boolean = false,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onTogglePin: () -> Unit,
  modifier: Modifier = Modifier
) {
  val animatedCardColor = MaterialTheme.colorScheme.surface

  val borderColor = if (isHighlighted) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
  } else {
    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
  }
  val borderWidth = if (isHighlighted) 2.dp else 1.dp

  Box(modifier = modifier.fillMaxWidth()) {
    val topPadding = if (note.category != null) 12.dp else 0.dp
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = topPadding)
        .alpha(if (isDisabled) 0.2f else 1f)
      .clip(RoundedCornerShape(16.dp))
      .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
      .background(animatedCardColor)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      // Başlık satırı
      if (note.title.isNotBlank()) {
        Text(
          text = note.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = if (note.isLocked) FontWeight.Normal else FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .fillMaxWidth()
            .padding(end = 28.dp) // Raptiye için güvenli alan
            .let { if (note.isLocked) it.blur(radius = 5.dp) else it }
        )
      }

      // İçerik önizlemesi
      if (note.content.isNotBlank()) {
        if (note.title.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
        
        val displayContent = remember(note.content, note.isChecklist) {
            if (note.isChecklist) {
                try {
                    val items = kotlinx.serialization.json.Json.decodeFromString<List<com.notdefterim.app.domain.model.ChecklistItem>>(note.content)
                    val checkedCount = items.count { it.isChecked }
                    val summary = "[$checkedCount/${items.size} tamamlandı]"
                    val previewLines = items.take(3).joinToString("\n") { (if (it.isChecked) "☑ " else "☐ ") + it.text }
                    "$summary\n$previewLines"
                } catch (e: Exception) {
                    "[Liste İçeriği]"
                }
            } else {
                note.content
            }
        }

        Text(
          text = displayContent,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .fillMaxWidth()
            .let { if (note.title.isBlank()) it.padding(end = 28.dp) else it }
            .let { if (note.isLocked) it.blur(radius = 5.dp) else it }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Alt kısım: Tarih ve Alarm ikonu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = note.updatedAt.format(cardDateFormatter),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {

          if (note.reminderAt != null) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = stringResource(R.string.reminder_title),
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    IconButton(
      onClick = onTogglePin,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .size(36.dp)
        .padding(4.dp)
    ) {
      Icon(
        imageVector = Icons.Rounded.PushPin,
        contentDescription = if (note.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin),
        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
          .size(18.dp)
          .let { if (!note.isPinned) it.rotate(45f) else it }
      )
    }

    if (note.isLocked) {
      Icon(
        imageVector = Icons.Rounded.Lock,
        contentDescription = "Kilitli",
        modifier = Modifier
          .size(48.dp)
          .align(Alignment.Center),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
      )
    }

    }
    
    if (note.category != null) {
      val catName = note.category.name
      val catIcon = when {
          catName.equals("Alışveriş", ignoreCase = true) -> Icons.Rounded.ShoppingCart
          catName.equals("Yapılacaklar", ignoreCase = true) -> Icons.Rounded.FormatListBulleted
          catName.equals("Fikirler", ignoreCase = true) -> Icons.Outlined.Lightbulb
          else -> Icons.Rounded.Bookmark
      }
      
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .background(MaterialTheme.colorScheme.background, CircleShape)
          .border(borderWidth, borderColor, CircleShape)
          .padding(4.dp),
        contentAlignment = Alignment.Center
      ) {
         Icon(
           imageVector = catIcon,
           contentDescription = catName,
           modifier = Modifier.size(14.dp),
           tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
         )
      }
    }
  }
}
