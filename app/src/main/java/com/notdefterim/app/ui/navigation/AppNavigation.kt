package com.notdefterim.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.ui.notedetail.NoteDetailScreen
import com.notdefterim.app.ui.notelist.NoteListScreen
import com.notdefterim.app.ui.settings.SettingsScreen

/**
 * Uygulama navigasyon grafiği.
 *
 * Rotalar:
 * - NoteList: Ana not listesi (başlangıç rotası)
 * - NoteDetail/{noteId}: Düzenleme (noteId=0 → yeni not)
 * - Settings: Ayarlar
 */
object AppRoutes {
  const val NOTE_LIST = "note_list"
  const val NOTE_DETAIL = "note_detail/{noteId}"
  const val SETTINGS = "settings"

  fun noteDetail(noteId: Long = 0L) = "note_detail/$noteId"
}

@Composable
fun AppNavigation(
  googleAuthManager: GoogleAuthManager,
  modifier: Modifier = Modifier
) {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = AppRoutes.NOTE_LIST,
    modifier = modifier
  ) {

    composable(route = AppRoutes.NOTE_LIST) {
      NoteListScreen(
        onNoteClick = { noteId ->
          navController.navigate(AppRoutes.noteDetail(noteId))
        },
        onNewNoteClick = {
          navController.navigate(AppRoutes.noteDetail(0L))
        },
        onSettingsClick = {
          navController.navigate(AppRoutes.SETTINGS)
        }
      )
    }

    composable(
      route = AppRoutes.NOTE_DETAIL,
      arguments = listOf(
        navArgument("noteId") { type = NavType.LongType }
      )
    ) {
      NoteDetailScreen(
        onNavigateBack = {
          navController.popBackStack()
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
}
