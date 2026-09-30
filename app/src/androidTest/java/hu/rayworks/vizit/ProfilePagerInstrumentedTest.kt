package hu.rayworks.vizit

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.account.*
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.local.VizitDatabase
import hu.rayworks.vizit.data.sync.ProfileSyncScheduler
import hu.rayworks.vizit.v10.data.*
import hu.rayworks.vizit.v10.ui.AccountProfileBridge
import hu.rayworks.vizit.v10.ui.VizitApp
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.Base64

/** Real ZIP UI, real pager gestures, controller and Room; only the network is controlled. */
@RunWith(AndroidJUnit4::class)
class ProfilePagerInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: VizitDatabase
    private lateinit var repo: AccountProfileRepository
    private lateinit var session: AccountProfileSession
    private lateinit var app: AppState
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val remote = ProfileTestRemote()
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, VizitDatabase::class.java).build()
        repo = AccountProfileRepository(db.accountProfileDao(), remote, object : ProfileSyncScheduler {
            override fun enqueue() {}
            override fun cancel() {}
        }, automaticSync = { true })
        session = AccountProfileSession(repo, scope)
        app = AppState(initialProfiles = emptyList(), productionMode = true)
        app.accountName = "Tesztfiók"
        app.accountEmail = "login@example.test"
        app.runtime = RuntimeBindings(
            onProfileSelected = session::select,
            onSaveProfile = { profile -> scope.launch {
                val issue = session.save(profile.id, profile.toContactProfile(context), profile.toCardPresentation())
                check(issue == null) { issue.orEmpty() }
                app.confirmSaved(profile)
                app.sheet = null
            } },
            onBeginAdditionalProfile = { null },
            onCreateAdditionalProfile = { profile, _ -> scope.launch {
                val issue = session.create(profile.toContactProfile(context), profile.toCardPresentation())
                check(issue == null) { issue.orEmpty() }
                app.closeWizard(force = true)
            } },
        )
    }
    @After fun cleanup() {
        scope.cancel()
        compose.waitForIdle()
        db.close()
    }
    private fun mount() {
        val auth = AuthViewModel(context)
        compose.setContent {
            val state by session.state.collectAsState()
            AccountProfileBridge(app, state)
            LaunchedEffect(state.loadStatus, state.needsFirstProfile) {
                app.gate = if (state.loadStatus == AccountProfileLoadStatus.READY) null else Gate.Loading
                if (state.needsFirstProfile) app.openInitialProfileWizard()
            }
            VizitApp(app, auth)
        }
        compose.runOnIdle { session.bind("one") }
        compose.waitUntil(10_000) { session.state.value.catalogVerified && app.current != null }
    }
    private fun jpeg(color: Int): String {
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        bitmap.recycle()
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }
    private fun assertCurrent(id: String, name: String, color: Int) {
        try {
            compose.waitUntil(10_000) { app.current?.id == id && session.state.value.activeId == id }
        } catch (failure: Exception) {
            val node = compose.onNodeWithTag("profile-pager").fetchSemanticsNode()
            val scroll = node.config.getOrElse(SemanticsProperties.HorizontalScrollAxisRange) { null }
            throw AssertionError("Expected=$id UI=${app.current?.id} selected=${app.selected} active=${session.state.value.activeId} scroll=${scroll?.value?.invoke()} request=${app.goToRequest}", failure)
        }
        compose.runOnIdle {
            val current = app.current!!
            assertEquals(name, current.name)
            assertEquals(name, current.label)
            assertEquals("one", session.state.value.ownerId)
            assertEquals("login@example.test", app.accountEmail)
            assertFalse(app.wizardOpen)
            assertEquals(current.url, app.cardQr(current))
            val pixel = (current.photo as Pic.Bmp).bitmap.asAndroidBitmap().getPixel(12, 12)
            if (color == Color.RED) assertTrue(Color.red(pixel) > 200 && Color.blue(pixel) < 40)
            else assertTrue(Color.blue(pixel) > 200 && Color.red(pixel) < 40)
        }
    }

    @Test fun repeatedSwipesKeepNamePhotoQrAndLoginIdentityTogether() {
        val first = testRemoteProfile("one", "Céges profil", true, jpeg(Color.BLUE))
        val second = testRemoteProfile("one", "Magánprofil", false, jpeg(Color.RED))
        remote.profiles += listOf(first, second, testRemoteProfile("two", "Idegen fiók", true))
        mount()
        assertCurrent(first.id, first.displayName, Color.BLUE)
        repeat(4) {
            compose.onNodeWithTag("profile-pager").performTouchInput { swipeLeft() }
            assertCurrent(second.id, second.displayName, Color.RED)
            compose.onNodeWithTag("profile-pager").performTouchInput { swipeRight() }
            assertCurrent(first.id, first.displayName, Color.BLUE)
        }
        compose.runOnIdle {
            app.openEdit(null)
            app.updateFocus { it.copy(name = "Céges profil szerkesztve", email = "business@example.test") }
            app.saveFocus()
        }
        compose.waitUntil(10_000) { session.state.value.active?.profile?.fullName == "Céges profil szerkesztve" && app.sheet == null }
        compose.runOnIdle {
            assertEquals(first.id, session.state.value.activeId)
            assertEquals("business@example.test", session.state.value.active!!.profile.email)
            assertEquals(second.displayName, app.profiles.first { it.id == second.id }.name)
        }
        compose.runOnIdle { app.sheet = SheetKind.Menu }
        compose.onNodeWithText("login@example.test").assertIsDisplayed()
        compose.onNodeWithText(first.profile.email).assertDoesNotExist()
        compose.runOnIdle { app.sheet = null; app.goTo(2, animate = false) }
        compose.waitUntil(10_000) { app.current == null && app.selected == 2 }
        compose.runOnIdle { assertFalse(app.wizardOpen); app.openWizard() }
        compose.waitUntil(10_000) { app.wizardOpen }
        compose.runOnIdle {
            app.addPublished(Profile("draft", "Új profil", true, "Új profil", phone = "456", email = "new@example.test"), false)
        }
        compose.waitUntil(10_000) { app.profiles.size == 3 && app.current?.name == "Új profil" && !app.wizardOpen }
        compose.runOnIdle {
            assertEquals("one", session.state.value.ownerId)
            assertTrue(session.state.value.profiles.all { it.ownerId == "one" })
            assertEquals("login@example.test", app.accountEmail)
        }
    }

    @Test fun logoutAndAnotherLoginClearTheOldProfilesAndWizard() {
        remote.profiles += listOf(testRemoteProfile("one", "Első fiók", true), testRemoteProfile("two", "Második fiók", true))
        mount()
        compose.runOnIdle {
            app.openWizard()
            session.unbind()
            app.resetForAccountChange()
            app.accountName = "Másik fiók"
            app.accountEmail = "second-login@example.test"
            remote.owner = "two"
            session.bind("two")
        }
        compose.waitUntil(10_000) { app.current?.name == "Második fiók" && session.state.value.catalogVerified }
        compose.runOnIdle {
            assertFalse(app.wizardOpen)
            assertNull(app.wizard)
            assertEquals(1, app.profiles.size)
            assertTrue(session.state.value.profiles.all { it.ownerId == "two" })
            app.sheet = SheetKind.Menu
        }
        compose.onNodeWithText("second-login@example.test").assertIsDisplayed()
        compose.onNodeWithText("login@example.test").assertDoesNotExist()
    }
}
