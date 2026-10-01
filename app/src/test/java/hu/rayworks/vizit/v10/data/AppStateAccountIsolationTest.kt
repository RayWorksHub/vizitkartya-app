package hu.rayworks.vizit.v10.data

import hu.rayworks.vizit.v10.ui.wizard.WizardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AppStateAccountIsolationTest {
    @Test
    fun `catalog refresh preserves the add card and unsaved edits by ID`() {
        val first = Profile(id = "first", label = "Első", real = true, name = "Első", phone = "111")
        val second = Profile(id = "second", label = "Második", real = true, name = "Második", phone = "222")
        val state = AppState(initialProfiles = listOf(first, second), productionMode = true)
        state.selected = 1
        state.updateFocus { it.copy(name = "Szerkesztett", phone = "333") }
        state.replaceProfiles(listOf(first, second), "second")
        assertEquals("333", state.current?.phone)
        state.selected = 2
        state.replaceProfiles(listOf(first, second), "second")
        assertNull(state.current)
        assertEquals(2, state.selected)
        assertFalse(state.wizardOpen)
    }

    @Test
    fun `new profile wizard starts blank and keeps the login identity`() {
        val state = AppState(initialProfiles = listOf(Profile("own", "Cég", true, "Profil neve", email = "contact@example.test")), productionMode = true)
        state.accountEmail = "login@example.test"
        state.openWizard()
        assertEquals("", state.wizard?.name)
        assertEquals("", state.wizard?.email)
        assertEquals("login@example.test", state.accountEmail)
    }
    @Test
    fun `catalog replacement keeps the complete data of every profile`() {
        val first = Profile(id = "first", label = "Első", real = true, name = "Első Elek", phone = "111")
        val second = Profile(id = "second", label = "Második", real = true, name = "Második Mária", phone = "222")
        val state = AppState(initialProfiles = listOf(first), productionMode = true)

        state.replaceProfiles(listOf(first, second), activeId = "second")

        assertEquals("second", state.current?.id)
        assertEquals("Második Mária", state.current?.name)
        assertEquals("222", state.current?.phone)
    }

    @Test
    fun `account change clears profile wizard and previous account cards`() {
        val state = AppState(
            initialProfiles = listOf(Profile(id = "old", label = "Régi", real = true, name = "Régi Profil")),
            productionMode = true,
        )
        state.wizard = WizardState(takenSlugs = { emptyList() }, initialName = "Régi Profil")
        state.wizardOpen = true
        state.sheet = SheetKind.Edit
        state.pages.add(Page.Settings)

        state.resetForAccountChange()

        assertEquals(listOf("loading"), state.profiles.map(Profile::id))
        assertFalse(state.wizardOpen)
        assertNull(state.wizard)
        assertNull(state.sheet)
        assertEquals(emptyList<Page>(), state.pages)
        assertEquals(Gate.Loading, state.gate)
    }
}
