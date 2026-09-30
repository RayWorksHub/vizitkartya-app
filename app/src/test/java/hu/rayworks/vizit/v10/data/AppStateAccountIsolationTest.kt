package hu.rayworks.vizit.v10.data

import hu.rayworks.vizit.v10.ui.wizard.WizardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AppStateAccountIsolationTest {
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
