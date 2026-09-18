package hu.rayworks.vizit

import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.local.*
import hu.rayworks.vizit.data.remote.RemoteContactPhoto
import hu.rayworks.vizit.data.sync.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.Base64

@RunWith(AndroidJUnit4::class)
class ProfileStabilityInstrumentedTest {
    private lateinit var db: VizitDatabase
    private lateinit var store: RoomProfileStore
    private val owner="11111111-1111-4111-8111-111111111111"
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),VizitDatabase::class.java).build()
        store=RoomProfileStore(db.profileDao())
    }
    @After fun cleanup() { db.close() }
    private fun photo(): String {
        val image=Bitmap.createBitmap(24,20,Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.BLUE)
        val bytes=ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.JPEG,80,bytes);image.recycle()
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }
    private fun profile(jpeg: String = photo())=ContactProfile(fullName="Teszt Elek",phone="123",photoBase64=jpeg)

    @Test fun photoValidationAndDurableOutbox() = runBlocking {
        val p=profile()
        Assert.assertEquals(p.photoBase64,RemoteContactPhoto.load("data:image/jpeg;base64,"+p.photoBase64,"https://example.supabase.co"))
        store.save(owner,p,true,"first",1)
        val pending=store.dueMutation(owner,1)!!
        Assert.assertEquals(p.photoBase64,pending.payload.photoBase64)
        Assert.assertEquals(p.photoBase64,ProfileSnapshotMapper.toContactProfile(db.profileDao().getSnapshot(owner)).photoBase64)
    }
    @Test fun oldResponseNeverClearsNewEdit() = runBlocking {
        store.save(owner,profile(),true,"old",1)
        val old=store.dueMutation(owner,1)!!
        store.markSyncing(old,1)
        store.save(owner,profile(""),true,"new",2)
        Assert.assertFalse(store.completeSync(old,RemoteProfileSnapshot(1,old.payload),3))
        Assert.assertEquals("new",store.dueMutation(owner,3)?.operationId)
        Assert.assertEquals("",store.dueMutation(owner,3)?.payload?.photoBase64)
    }
    @Test fun explicitLocalConflictChoicePreservesPhotoAndRebases() = runBlocking {
        store.save(owner,profile(),true,"edit",1)
        val local=store.dueMutation(owner,1)!!
        val cloud=local.payload.copy(displayName="Másik változat",photoBase64="",displayImagePath="")
        store.markConflict(local,RemoteProfileSnapshot(10,cloud),2)
        Assert.assertEquals(ProfileSyncStatus.CONFLICT,store.observe(owner).first().sync.status)
        Assert.assertFalse(store.retryNow(owner,3))
        Assert.assertTrue(store.resolveConflict(owner,true,"resolved",4))
        val next=store.dueMutation(owner,4)!!
        Assert.assertEquals(10L,next.baseServerVersion)
        Assert.assertEquals(local.payload.photoBase64,next.payload.photoBase64)
        Assert.assertNotNull(next.payload.baseFingerprint)
    }
    @Test fun explicitCloudChoiceAppliesPhotoDeletion() = runBlocking {
        store.save(owner,profile(),true,"edit",1)
        val local=store.dueMutation(owner,1)!!
        val cloud=local.payload.copy(photoBase64="",displayImagePath="")
        store.markConflict(local,RemoteProfileSnapshot(10,cloud),2)
        Assert.assertTrue(store.resolveConflict(owner,false,"unused",3))
        Assert.assertFalse(store.hasPendingMutation(owner))
        Assert.assertEquals("",store.observe(owner).first().profile.photoBase64)
    }
    @Test fun firstCloudPullDoesNotEraseOldLocalOnlyPhoto() = runBlocking {
        val p=profile()
        store.save(owner,p,false,"old-local",1)
        val cloud=ProfileSyncPayload(displayName="Teszt Elek",photoBase64="",displayImagePath="",
            contacts=listOf(ProfileContactPayload("phone","phone","Mobil","123",0,true)))
        Assert.assertTrue(store.applyRemoteIfClean(owner,RemoteProfileSnapshot(100,cloud),2))
        Assert.assertTrue(store.hasPendingMutation(owner))
        Assert.assertEquals(p.photoBase64,store.dueMutation(owner,3)?.payload?.photoBase64)
    }
    @Test fun queuesRemainOwnerScoped() = runBlocking {
        store.save(owner,profile(),true,"a",1)
        store.save("other",profile(""),true,"b",2)
        Assert.assertEquals("a",store.dueMutation(owner,2)?.operationId)
        Assert.assertEquals("b",store.dueMutation("other",2)?.operationId)
        store.deleteUserData("other")
        Assert.assertTrue(store.hasPendingMutation(owner))
    }
}
