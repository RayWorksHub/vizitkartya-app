package hu.rayworks.vizit

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.Profile
import hu.rayworks.vizit.v10.ui.theme.ThemeState
import hu.rayworks.vizit.v10.ui.wizard.BlockStatus
import hu.rayworks.vizit.v10.ui.wizard.ProfileType
import hu.rayworks.vizit.v10.ui.wizard.ThemedWizardHost as WizardHost
import hu.rayworks.vizit.v10.ui.wizard.WizardState
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZipWizardInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun privateWizardRendersOriginalBlockAndCallsRealSaveBridge() {
        val wizard = WizardState({ emptyList() })
        val bridge = AppState(wizard)
        var published: Profile? = null
        bridge.onPublish = { published = it }
        compose.setContent { WizardHost(bridge) }
        compose.onNodeWithText("Magánszemély").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("1. BLOKK").assertIsDisplayed()
        assertEquals(listOf("personal", "online", "done"), wizard.flow.map { it.id })
        capture("zip-wizard-android-personal")
        compose.runOnIdle { wizard.input("name", "Teszt Elek") }
        compose.onNodeWithText("Tovább").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Kihagyom").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Publikálás").performClick()
        compose.runOnIdle {
            assertEquals("Teszt Elek", published?.name)
            assertEquals("teszt-elek", published?.slug)
            assertEquals(BlockStatus.Skip, wizard.status("online"))
        }
        capture("zip-wizard-android-completion")
    }

    @Test fun companyBlockRendersSeparatelyAndSupportsDarkTheme() {
        val wizard = WizardState({ emptyList() })
        wizard.chooseType(ProfileType.Business)
        wizard.begin()
        wizard.input("name", "Teszt Elek")
        wizard.next("personal", false)
        val bridge = AppState(wizard)
        compose.setContent { WizardHost(bridge) }
        compose.waitForIdle()
        compose.onNodeWithText("2. BLOKK").assertIsDisplayed()
        capture("zip-wizard-android-company")
        compose.runOnIdle { ThemeState.dark = true }
        compose.waitForIdle()
        capture("zip-wizard-android-company-dark")
        compose.runOnIdle { ThemeState.dark = false }
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val directory = if (output != null) File(output, "zip-wizard-evidence")
            else File(context.getExternalFilesDir(null), "zip-wizard-evidence")
        check(directory.isDirectory || directory.mkdirs())
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
