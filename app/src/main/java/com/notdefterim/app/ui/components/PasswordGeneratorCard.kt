package com.notdefterim.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.SecureRandom
import kotlin.math.roundToInt

/**
 * Kriptografik olarak güvenli ve özelleştirilebilir şifre üreten modern bir Compose kart bileşeni.
 *
 * @param onPasswordGenerated Üretilen şifre onaylandığında çağrılan callback fonksiyonu.
 * @param modifier Düzenleyici nesnesi.
 */
@Composable
fun PasswordGeneratorCard(
  onPasswordGenerated: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var length by remember { mutableFloatStateOf(6f) }
  var includeUppercase by remember { mutableStateOf(true) }
  var includeLowercase by remember { mutableStateOf(true) }
  var includeNumbers by remember { mutableStateOf(true) }
  var includeSpecial by remember { mutableStateOf(true) }
  
  var generatedPassword by remember { mutableStateOf("") }

  // Kriterler veya uzunluk her değiştiğinde otomatik olarak şifre üretilir
  LaunchedEffect(length, includeUppercase, includeLowercase, includeNumbers, includeSpecial) {
    generatedPassword = generateSecurePassword(
      length = length.roundToInt(),
      includeUppercase = includeUppercase,
      includeLowercase = includeLowercase,
      includeNumbers = includeNumbers,
      includeSpecial = includeSpecial
    )
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    border = BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = "Güvenli Şifre Üretici",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      // Üretilen Şifre Önizleme Alanı
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
          .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = generatedPassword.ifEmpty { "Seçim yapınız..." },
          style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = if (generatedPassword.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Start,
          maxLines = 2
        )
        IconButton(
          onClick = {
            generatedPassword = generateSecurePassword(
              length = length.roundToInt(),
              includeUppercase = includeUppercase,
              includeLowercase = includeLowercase,
              includeNumbers = includeNumbers,
              includeSpecial = includeSpecial
            )
          },
          enabled = includeUppercase || includeLowercase || includeNumbers || includeSpecial
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "Yeniden Üret",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }

      // Şifre Uzunluğu Seçici (Slider)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Şifre Uzunluğu",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          // Şık bir kapsül badge içinde değer gösterimi
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(MaterialTheme.colorScheme.primaryContainer)
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "${length.roundToInt()} Karakter",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "4",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
          Slider(
            value = length,
            onValueChange = { length = it },
            valueRange = 4f..32f,
            steps = 27,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f)
          )
          Text(
            text = "32",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
        }
      }

      // Seçenekler / Kriterler
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Kriterler",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium
        )
        
        val defaultCheckboxColors = CheckboxDefaults.colors(
          checkedColor = MaterialTheme.colorScheme.primary,
          uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = includeUppercase,
              onCheckedChange = { includeUppercase = it },
              colors = defaultCheckboxColors
            )
            Text(text = "Büyük Harf (A-Z)", style = MaterialTheme.typography.bodySmall)
          }
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = includeLowercase,
              onCheckedChange = { includeLowercase = it },
              colors = defaultCheckboxColors
            )
            Text(text = "Küçük Harf (a-z)", style = MaterialTheme.typography.bodySmall)
          }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = includeNumbers,
              onCheckedChange = { includeNumbers = it },
              colors = defaultCheckboxColors
            )
            Text(text = "Sayılar (0-9)", style = MaterialTheme.typography.bodySmall)
          }
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = includeSpecial,
              onCheckedChange = { includeSpecial = it },
              colors = defaultCheckboxColors
            )
            Text(text = "Özel Karakter (!@#)", style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      // Şifreyi Kullan Butonu
      Button(
        onClick = {
          if (generatedPassword.isNotEmpty()) {
            onPasswordGenerated(generatedPassword)
          }
        },
        enabled = generatedPassword.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Şifreyi Uygula",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    }
  }
}

/**
 * Kriptografik olarak güvenli rastgele şifre üretir.
 */
private fun generateSecurePassword(
  length: Int,
  includeUppercase: Boolean,
  includeLowercase: Boolean,
  includeNumbers: Boolean,
  includeSpecial: Boolean
): String {
  val uppercaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  val lowercaseChars = "abcdefghijklmnopqrstuvwxyz"
  val numberChars = "0123456789"
  val specialChars = "!@#$%^&*()_-+=<>?/{}[]|~"

  val random = SecureRandom()
  val resultChars = mutableListOf<Char>()

  // Aktif grupları belirle
  val activeGroups = mutableListOf<String>()
  if (includeUppercase) activeGroups.add("UPPER")
  if (includeLowercase) activeGroups.add("LOWER")
  if (includeNumbers) activeGroups.add("NUMBER")
  if (includeSpecial) activeGroups.add("SPECIAL")

  if (activeGroups.isEmpty()) return ""

  // Eğer hem sayı hem özel karakter seçilmişse, oranları kesin belirleyelim (Özel = Sayı / 2)
  if (includeNumbers && includeSpecial) {
    val letterGroupsCount = (if (includeUppercase) 1 else 0) + (if (includeLowercase) 1 else 0)
    
    if (letterGroupsCount == 0) {
      // Sadece Sayı ve Özel karakter aktifse
      val specCount = (length / 3).coerceAtLeast(1)
      val numCount = length - specCount
      
      for (i in 0 until numCount) {
        resultChars.add(numberChars[random.nextInt(numberChars.length)])
      }
      for (i in 0 until specCount) {
        resultChars.add(specialChars[random.nextInt(specialChars.length)])
      }
    } else {
      // Harfler de aktifse, oranın (2:1) bozulmaması için sayı ve özel karakter adetlerini
      // toplam şifre uzunluğuna göre dengeli ve 3'ün katı olacak şekilde ayırıyoruz.
      val targetNumSpec = (length / 2).coerceAtLeast(3)
      val specCount = targetNumSpec / 3
      val numCount = specCount * 2
      
      val totalNumSpec = numCount + specCount
      val lettersNeeded = length - totalNumSpec
      
      // Rakamları ekle
      for (i in 0 until numCount) {
        resultChars.add(numberChars[random.nextInt(numberChars.length)])
      }
      // Özel karakterleri ekle
      for (i in 0 until specCount) {
        resultChars.add(specialChars[random.nextInt(specialChars.length)])
      }
      
      // Harfleri ekle
      val lettersPool = StringBuilder()
      if (includeUppercase) lettersPool.append(uppercaseChars)
      if (includeLowercase) lettersPool.append(lowercaseChars)
      
      if (lettersPool.isNotEmpty()) {
        // Her harf grubundan en az bir karakter eklemeyi garanti edelim
        if (includeUppercase) {
          resultChars.add(uppercaseChars[random.nextInt(uppercaseChars.length)])
        }
        if (includeLowercase) {
          resultChars.add(lowercaseChars[random.nextInt(lowercaseChars.length)])
        }
        
        val minLettersAdded = letterGroupsCount
        val remainingLetters = lettersNeeded - minLettersAdded
        for (i in 0 until remainingLetters) {
          resultChars.add(lettersPool[random.nextInt(lettersPool.length)])
        }
      }
      
      // Olası boyutsal kaymaları düzelt (özellikle harfleri ekleyerek/kırparak oranı koru)
      while (resultChars.size < length) {
        if (lettersPool.isNotEmpty()) {
          resultChars.add(lettersPool[random.nextInt(lettersPool.length)])
        } else {
          resultChars.add(numberChars[random.nextInt(numberChars.length)])
        }
      }
      while (resultChars.size > length) {
        // Oranı (2:1) korumak adına sadece harflerden silmeye çalışalım
        var removed = false
        for (i in resultChars.indices.reversed()) {
          val char = resultChars[i]
          if (char.isLetter()) {
            resultChars.removeAt(i)
            removed = true
            break
          }
        }
        if (!removed) {
          resultChars.removeAt(resultChars.lastIndex)
        }
      }
    }
  } else {
    // Standart dağılım (Her aktif gruptan en az bir tane ekle)
    if (includeUppercase) resultChars.add(uppercaseChars[random.nextInt(uppercaseChars.length)])
    if (includeLowercase) resultChars.add(lowercaseChars[random.nextInt(lowercaseChars.length)])
    if (includeNumbers) resultChars.add(numberChars[random.nextInt(numberChars.length)])
    if (includeSpecial) resultChars.add(specialChars[random.nextInt(specialChars.length)])

    val charPool = StringBuilder()
    if (includeUppercase) charPool.append(uppercaseChars)
    if (includeLowercase) charPool.append(lowercaseChars)
    if (includeNumbers) charPool.append(numberChars)
    if (includeSpecial) charPool.append(specialChars)

    while (resultChars.size < length && charPool.isNotEmpty()) {
      resultChars.add(charPool[random.nextInt(charPool.length)])
    }
  }

  // Karakterleri karıştır ve sınırla
  val shuffled = resultChars.take(length).shuffled(random)
  return shuffled.joinToString("")
}
