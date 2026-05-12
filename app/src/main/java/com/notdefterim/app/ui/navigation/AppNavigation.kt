package com.notdefterim.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.data.local.ThemePreferences
import com.notdefterim.app.ui.notedetail.NoteDetailScreen
import com.notdefterim.app.ui.notelist.NoteListScreen
import com.notdefterim.app.ui.settings.SettingsScreen
import kotlinx.coroutines.delay

object AppRoutes {
  const val NOTE_LIST = "note_list"
  const val NOTE_DETAIL = "note_detail/{noteId}?categoryId={categoryId}"
  const val SETTINGS = "settings"
  const val PASSWORDS = "passwords"

  fun noteDetail(noteId: Long = 0L, categoryId: Long? = null) = "note_detail/$noteId" + if (categoryId != null) "?categoryId=$categoryId" else ""
}

@Composable
fun AppNavigation(
  googleAuthManager: GoogleAuthManager,
  themePreferences: ThemePreferences,
  systemDark: Boolean,
  modifier: Modifier = Modifier
) {
  val navController = rememberNavController()
  var showSavedNotification by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
      navController = navController,
      startDestination = AppRoutes.NOTE_LIST,
      modifier = modifier
    ) {

      composable(route = AppRoutes.NOTE_LIST) {
        com.notdefterim.app.ui.home.HomeScreen(
          themePreferences = themePreferences,
          systemDark = systemDark,
          onNoteClick = { noteId ->
            navController.navigate(AppRoutes.noteDetail(noteId))
          },
          onNewNoteClick = { categoryId ->
            navController.navigate(AppRoutes.noteDetail(0L, categoryId))
          },
          onSettingsClick = {
            navController.navigate(AppRoutes.SETTINGS)
          }
        )
      }

      composable(
        route = AppRoutes.NOTE_DETAIL,
        arguments = listOf(
          navArgument("noteId") { type = NavType.LongType },
          navArgument("categoryId") {
              type = NavType.StringType
              nullable = true
              defaultValue = null
          }
        )
      ) {
        NoteDetailScreen(
          onNavigateBack = {
            navController.popBackStack()
          },
          onSaved = {
            showSavedNotification = true
          }
        )
      }

      composable(route = AppRoutes.SETTINGS) {
        SettingsScreen(
          onNavigateBack = {
            navController.popBackStack()
          },
          googleAuthManager = googleAuthManager
        )
      }
    }

    // Overlay
    AnimatedVisibility(
       visible = showSavedNotification,
       enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
       exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
       modifier = Modifier.align(Alignment.TopCenter).padding(top = 135.dp)
    ) {
       Box(
         modifier = Modifier
           .background(Color(0xFF4CAF50).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
           .padding(horizontal = 24.dp, vertical = 12.dp)
       ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
            Text("Notunuz Kaydedildi", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          }
       }
       LaunchedEffect(showSavedNotification) {
          if (showSavedNotification) {
             delay(2000)
             showSavedNotification = false
          }
       }
    }
  }
}
