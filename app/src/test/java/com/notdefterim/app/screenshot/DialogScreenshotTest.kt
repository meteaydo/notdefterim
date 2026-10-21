package com.notdefterim.app.screenshot

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.notdefterim.app.domain.model.PinScope
import com.notdefterim.app.ui.components.SetupAppPinDialog
import com.notdefterim.app.ui.notelist.components.AddCategoryDialog
import com.notdefterim.app.ui.passwords.AddPasswordDialog
import com.notdefterim.app.ui.theme.NotDefterimTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Diyalog ekran görüntüleri.
 *
 * [ScreenshotTest]'teki Compose test kuralı burada kullanılamıyor: Robolectric'te bir diyalog
 * penceresinde metin alanı varken Compose hiç "boşta" duruma gelmiyor ve test
 * AppNotIdleException ile düşüyor. Bu yüzden Activity elle başlatılır, ana döngü sabit bir
 * süre işletilir ve diyalog penceresi Espresso üzerinden yakalanır.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialogScreenshotTest {

  @Test
  fun kategoriEkle() = snapshotDialog {
    AddCategoryDialog(onDismiss = {}, onAddCategory = { _, _ -> })
  }

  @Test
  fun parolaEkle() = snapshotDialog {
    AddPasswordDialog(
      onDismiss = {},
      onSave = { _, _, _, _ -> },
      frequentUsernames = listOf("ornek@gmail.com", "kullanici123"),
      categories = SampleData.passwordCategories
    )
  }

  @Test
  fun parolaEkle_koyuTema() = snapshotDialog(darkTheme = true) {
    AddPasswordDialog(
      onDismiss = {},
      onSave = { _, _, _, _ -> },
      frequentUsernames = listOf("ornek@gmail.com", "kullanici123"),
      categories = SampleData.passwordCategories
    )
  }

  @Test
  fun uygulamaPiniOlustur() = snapshotDialog {
    SetupAppPinDialog(
      appPin = null,
      appPinHint = null,
      appPinScope = PinScope.ALL.value,
      onDismiss = {},
      onSave = { _, _, _ -> }
    )
  }

  @Test
  fun uygulamaPiniDegistir() = snapshotDialog {
    SetupAppPinDialog(
      appPin = "hash",
      appPinHint = "Doğum yılı",
      appPinScope = PinScope.PASSWORDS_ONLY.value,
      onDismiss = {},
      onSave = { _, _, _ -> }
    )
  }

  private fun snapshotDialog(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.setContent {
          NotDefterimTheme(darkTheme = darkTheme, dynamicColor = false) {
            content()
          }
        }
      }
      // Açılış animasyonunun bitmesi için yeterli
      shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
      onView(isRoot()).inRoot(isDialog()).captureRoboImage()
    }
  }
}
