package com.notdefterim.app.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.notdefterim.app.ui.theme.NotDefterimTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Ekran görüntüsü testlerinin ortak tabanı.
 *
 * Kayıt:      ./gradlew recordRoborazziDebug   → src/test/screenshots/ altına referans PNG'ler yazar
 * Doğrulama:  ./gradlew verifyRoborazziDebug   → referanslardan farklı bir piksel varsa test düşer
 * Karşılaştır: ./gradlew compareRoborazziDebug → farkları build/outputs/roborazzi altında gösterir
 *
 * Dinamik renk kapalıdır; aksi halde sonuç cihaz duvar kağıdına göre değişirdi.
 * Diyaloglar bu sınıfı kullanmaz, bkz. [DialogScreenshotTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreenshotTest {

  @get:Rule
  val composeRule = createComposeRule()

  /** Bileşeni kendi boyutunda (16 dp kenar boşluğuyla) çizip kaydeder. */
  protected fun snapshotComponent(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    composeRule.setContent {
      NotDefterimTheme(darkTheme = darkTheme, dynamicColor = false) {
        Box(
          modifier = Modifier
            .testTag(SNAPSHOT_TAG)
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
          content()
        }
      }
    }
    composeRule.onNodeWithTag(SNAPSHOT_TAG).captureRoboImage()
  }

  /** Tam ekran bir composable'ı çizip kaydeder. */
  protected fun snapshotScreen(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    composeRule.setContent {
      NotDefterimTheme(darkTheme = darkTheme, dynamicColor = false) {
        content()
      }
    }
    composeRule.onRoot().captureRoboImage()
  }

  private companion object {
    const val SNAPSHOT_TAG = "snapshot"
  }
}
