package hu.rayworks.vizit.v10
import hu.rayworks.vizit.v10.ui.wizard.*
import org.junit.Assert.*
import org.junit.Test
class WizardParityTest {
    @Test fun privateFlowUsesThreeRealBlocksAndNoDemoIdentity() {
        val w = WizardState({ emptyList() })
        assertEquals("", w.name)
        w.chooseType(ProfileType.Private); w.begin()
        assertEquals(listOf("personal", "online", "done"), w.flow.map { it.id })
        assertEquals(listOf("name", "phone"), w.fieldKeys("personal"))
        assertFalse(w.valid("personal"))
        w.input("name", "Teszt Elek"); w.next("personal", false)
        assertEquals(BlockStatus.Active, w.status("online"))
        assertEquals("Kihagyom", w.actionLabel(w.flow[1]))
        w.next("online", false)
        assertEquals(BlockStatus.Skip, w.status("online"))
        assertTrue(w.valid("done")); assertNotNull(w.buildProfile())
        w.reopen("online"); assertEquals(BlockStatus.Open, w.status("online"))
        assertEquals("Teszt Elek", w.name)
    }
    @Test fun businessFlowKeepsCompanyFieldsInTheirOwnBlock() {
        val w = WizardState({ emptyList() }); w.chooseType(ProfileType.Business); w.begin()
        assertEquals(listOf("personal", "company", "online", "done"), w.flow.map { it.id })
        assertEquals(listOf("company", "role", "place", "bio"), w.fieldKeys("company"))
        assertFalse(w.valid("company")); w.input("company", "Teszt Kft."); assertTrue(w.valid("company"))
        w.input("email", "hibas"); assertFalse(w.valid("online"))
        w.input("email", "test@example.com"); assertTrue(w.valid("online"))
    }
}
