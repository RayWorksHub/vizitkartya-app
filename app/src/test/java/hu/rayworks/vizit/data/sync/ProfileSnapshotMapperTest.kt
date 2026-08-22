package hu.rayworks.vizit.data.sync

import hu.rayworks.vizit.data.ContactProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSnapshotMapperTest {
    @Test
    fun `maps legacy profile into normalized privacy-safe snapshot`() {
        val source = ContactProfile(
            fullName = "  Teszt Elek  ",
            firstName = " Elek ",
            lastName = " Teszt ",
            jobTitle = " Fejlesztő ",
            company = " VIZIT ",
            phone = " +36 30 123 4567 ",
            email = " elek@example.com ",
            website = " https://example.com ",
            address = " Budapest ",
            linkedIn = " https://linkedin.com/in/elek ",
            photoBase64 = "local-photo",
            publicSlug = " teszt-elek ",
            isPublic = true,
        )

        val snapshot = ProfileSnapshotMapper.toLocalSnapshot(
            userId = USER_ID,
            profile = source,
            previous = null,
            updatedAtEpochMs = 100L,
            pendingSync = true,
        )
        val payload = ProfileSnapshotMapper.toPayload(snapshot)

        assertEquals("Teszt Elek", snapshot.profile.displayName)
        assertEquals("local-photo", snapshot.profile.localContactPhotoBase64)
        assertEquals(listOf("phone", "email"), snapshot.contacts.map { it.kind })
        assertEquals(listOf("website", "linkedin"), snapshot.links.map { it.kind })
        assertTrue(snapshot.profile.pendingSync)
        assertTrue(payload.isPublic)
        assertTrue(payload.contacts.all { !it.isPublic })
        assertTrue(payload.links.all { !it.isPublic })
        assertTrue(payload.addresses.all { !it.isPublic })
        assertNull(payload.contactImagePath)
        assertFalse(ProfilePayloadCodec.encode(payload).contains("local-photo"))
    }

    @Test
    fun `stable ids survive edits and remote snapshot preserves local photo`() {
        val first = ProfileSnapshotMapper.toLocalSnapshot(
            userId = USER_ID,
            profile = ContactProfile(fullName = "Teszt Elek", phone = "+361"),
            previous = null,
            updatedAtEpochMs = 1L,
            pendingSync = true,
        )
        val edited = ProfileSnapshotMapper.toLocalSnapshot(
            userId = USER_ID,
            profile = ContactProfile(fullName = "Teszt Elek", phone = "+362", photoBase64 = "preview"),
            previous = first,
            updatedAtEpochMs = 2L,
            pendingSync = true,
        )
        val remote = ProfileSnapshotMapper.toLocalSnapshot(
            userId = USER_ID,
            payload = ProfileSnapshotMapper.toPayload(edited).copy(
                displayName = "Szerver Elek",
                fieldOrder = listOf("display_name", "phone"),
                fieldVisibility = mapOf("display_name" to true),
            ),
            previous = edited,
            updatedAtEpochMs = 3L,
        )
        val editedAfterPull = ProfileSnapshotMapper.toLocalSnapshot(
            userId = USER_ID,
            profile = ProfileSnapshotMapper.toContactProfile(remote).copy(company = "Új cég"),
            previous = remote,
            updatedAtEpochMs = 4L,
            pendingSync = true,
        )
        val preservedPayload = ProfileSnapshotMapper.toPayload(editedAfterPull)

        assertEquals(first.contacts.single().id, edited.contacts.single().id)
        assertEquals("preview", remote.profile.localContactPhotoBase64)
        assertEquals("Szerver Elek", ProfileSnapshotMapper.toContactProfile(remote).fullName)
        assertFalse(remote.profile.pendingSync)
        assertEquals(listOf("display_name", "phone"), preservedPayload.fieldOrder)
        assertEquals(mapOf("display_name" to true), preservedPayload.fieldVisibility)
    }

    @Test
    fun `flat compatibility edit preserves unexposed repeated fields and visibility`() {
        val remote = ProfileSnapshotMapper.toLocalSnapshot(
            userId = USER_ID,
            payload = ProfileSyncPayload(
                displayName = "Teszt Elek",
                contacts = listOf(
                    ProfileContactPayload("11111111-1111-4111-8111-111111111111", "phone", "Mobil", "+361", 0, true),
                    ProfileContactPayload("22222222-2222-4222-8222-222222222222", "phone", "Munka", "+362", 1, false),
                    ProfileContactPayload("33333333-3333-4333-8333-333333333333", "email", "E-mail", "e@example.com", 2, true),
                ),
                links = listOf(
                    ProfileLinkPayload(
                        id = "44444444-4444-4444-8444-444444444444",
                        kind = "custom",
                        label = "Portfólió",
                        url = "https://portfolio.example",
                        sortOrder = 3,
                        isPublic = true,
                    ),
                ),
            ),
            previous = null,
            updatedAtEpochMs = 1L,
        )
        val edited = ProfileSnapshotMapper.toLocalSnapshot(
            userId = USER_ID,
            profile = ProfileSnapshotMapper.toContactProfile(remote).copy(phone = "+369"),
            previous = remote,
            updatedAtEpochMs = 2L,
            pendingSync = true,
        )
        val payload = ProfileSnapshotMapper.toPayload(edited)

        assertEquals(listOf("+369", "+362"), payload.contacts.filter { it.kind == "phone" }.map { it.value })
        assertTrue(payload.contacts.first { it.value == "+369" }.isPublic)
        assertEquals("https://portfolio.example", payload.links.single().url)
        assertTrue(payload.links.single().isPublic)
    }

    private companion object {
        const val USER_ID = "6bc9b40d-5317-4f78-a1d6-c44d50f4e4f4"
    }
}
