package com.notdefterim.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.data.local.ThemePreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notdefterim.app.ui.notelist.NoteListScreen
import com.notdefterim.app.ui.passwords.PasswordListScreen
import androidx.compose.ui.res.stringResource
import com.notdefterim.app.R

@Composable
fun HomeScreen(
    themePreferences: ThemePreferences,
    systemDark: Boolean,
    onNoteClick: (Long) -> Unit,
    onNewNoteClick: (Long?) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(1) } // 0: Notlar, 1: Parolalar

    val userDarkTheme by themePreferences.isDarkTheme.collectAsStateWithLifecycle()
    val isDark = userDarkTheme ?: systemDark

    Column(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        // Üst Kısım: Segmented Control ve Ayarlar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            SegmentedControl(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.weight(5f)
            )
            Row(
                modifier = Modifier.weight(2f),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { themePreferences.toggleTheme(systemDark) },
                    modifier = Modifier.offset(x = 8.dp) // Ayarlar ikonuna yaklaştırır
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // İçerik Geçişi
        Crossfade(targetState = selectedTab, modifier = Modifier.fillMaxSize(), label = "home_tabs") { tab ->
            when (tab) {
                0 -> NoteListScreen(
                    onNoteClick = onNoteClick,
                    onNewNoteClick = onNewNoteClick,
                    hideTopBar = true
                )
                1 -> PasswordListScreen(
                    hideTopBar = true
                )
            }
        }
    }
}

@Composable
fun SegmentedControl(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(stringResource(R.string.my_notes), stringResource(R.string.passwords_title))
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                val tabBgColor by animateColorAsState(if (isSelected) selectedColor else unselectedColor, label = "tabBg")
                val textColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "tabText")

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tabBgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }
            }
        }
    }
}
