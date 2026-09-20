package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.sync.*
import hu.rayworks.vizit.qr.QrPayloadFactory
import org.junit.Assert.*
import org.junit.Test

class LegacyProfileCodecTest {
    private fun row() = LegacyProfileRecord("11111111-1111-4111-8111-111111111111",
        "22222222-2222-4222-8222-222222222222", "teszt-elek", "Teszt Elek", phone="123",
        updatedAt="2026-09-18T00:00:00.123456Z")

    @Test fun `timestamps retain microsecond precision`() {
        assertEquals(1L, row().copy(updatedAt="2026-09-18T00:00:00.123457Z").version - row().version)
    }
    @Test fun `legacy JSON without photo never means delete`() {
        val p = ProfilePayloadCodec.decode("""{"displayName":"Teszt Elek","contacts":[]}""")
        val update = LegacyProfileCodec.write(p, row(), row().ownerId)
        assertFalse(update.containsKey("avatar_url"))
    }
    @Test fun `explicit deletion and photo replacement are different`() {
        val empty = row().payload("").copy(displayImagePath=null)
        assertEquals("\"\"", LegacyProfileCodec.write(empty,row(),row().ownerId)["avatar_url"].toString())
        val jpeg = java.util.Base64.getEncoder().encodeToString(byteArrayOf(-1,-40,-1,0))
        val set = LegacyProfileCodec.write(empty.copy(photoBase64=jpeg),row(),row().ownerId)
        assertTrue(set["avatar_url"].toString().contains(LegacyProfileCodec.INLINE_PREFIX))
    }
    @Test fun `analytics alone do not change content revision`() {
        val base=row(); val counted=base.copy(updatedAt="2026-09-18T00:00:01Z")
        assertTrue(LegacyProfileCodec.canApply(base.version,LegacyProfileCodec.fingerprint(base.payload()),counted))
    }
    @Test fun `social link changes are part of the content revision`() {
        val base = row().copy(socialLinks = listOf(
            LegacySocialLink("33333333-3333-4333-8333-333333333333", "facebook", "Facebook", "https://facebook.com/old"),
        ))
        val hash = LegacyProfileCodec.fingerprint(base.payload())
        val changed = base.copy(socialLinks = listOf(
            LegacySocialLink("33333333-3333-4333-8333-333333333333", "facebook", "Facebook", "https://facebook.com/new"),
        ))
        assertFalse(LegacyProfileCodec.canApply(base.version, hash, changed))
    }
    @Test fun `remote photo replacement and deletion conflict`() {
        val base=row().copy(avatarUrl="data:image/jpeg;base64,/9j/AA==")
        val hash=LegacyProfileCodec.fingerprint(base.payload())
        assertFalse(LegacyProfileCodec.canApply(base.version,hash,base.copy(avatarUrl="")))
        assertFalse(LegacyProfileCodec.canApply(base.version,hash,base.copy(avatarUrl="different")))
    }
    @Test fun `unacknowledged initial write cannot overwrite remote`() {
        assertFalse(LegacyProfileCodec.canApply(0,null,row()))
    }
    @Test fun `QR needs a synchronized public profile`() {
        val p=ContactProfile(fullName="Teszt Elek",phone="123",publicSlug="teszt-elek",isPublic=true)
        assertNull(QrPayloadFactory.profileUrl(p))
        assertNull(QrPayloadFactory.profileUrl(p.copy(isPublic=false),true))
        assertEquals("https://e-nevjegy.vercel.app/p/teszt-elek",QrPayloadFactory.profileUrl(p,true))
    }
    @Test fun `first save creates a readable identifier without user input`() {
        val payload = ProfileSyncPayload(displayName = "Csukárdi Rajmund")
        val values = LegacyProfileCodec.write(payload, null, row().ownerId)
        assertEquals("\"csukardi-rajmund\"", values["slug"].toString())
    }
    @Test fun `QR uses only a verified custom domain`() {
        val profile = ContactProfile(
            fullName = "Teszt Elek", phone = "123", publicSlug = "teszt-elek", isPublic = true,
            customDomain = "nevjegy.example.hu",
        )
        assertEquals("https://e-nevjegy.vercel.app/p/teszt-elek", QrPayloadFactory.profileUrl(profile, true))
        assertEquals(
            "https://nevjegy.example.hu",
            QrPayloadFactory.profileUrl(profile.copy(customDomainVerified = true), true),
        )
    }
    @Test fun `oversized offline QR fails instead of losing fields`() {
        val p=ContactProfile(fullName="Teszt Elek",phone="123",address="Budapest ".repeat(400))
        assertTrue(QrPayloadFactory.contact(p,null).isFailure)
    }
    @Test fun `remote empty photo clears local photo`() {
        val old=ProfileSnapshotMapper.toLocalSnapshot(row().ownerId,ContactProfile(fullName="Teszt Elek",phone="123",photoBase64="old"),null,1,true)
        val next=ProfileSnapshotMapper.toLocalSnapshot(row().ownerId,row().payload(""),old,2)
        assertEquals("",next.profile.localContactPhotoBase64)
    }
    @Test fun `remote picture is preserved while editing another field`() {
        val remote=ProfileSnapshotMapper.toLocalSnapshot(row().ownerId,row().copy(avatarUrl="source-url").payload("jpeg"),null,1)
        val edited=ProfileSnapshotMapper.toLocalSnapshot(row().ownerId,ProfileSnapshotMapper.toContactProfile(remote).copy(company="New"),remote,2,true)
        assertEquals("source-url",edited.profile.displayImagePath)
        val deleted=ProfileSnapshotMapper.toLocalSnapshot(row().ownerId,ProfileSnapshotMapper.toContactProfile(remote).copy(photoBase64=""),remote,2,true)
        assertNull(deleted.profile.displayImagePath)
    }
}
