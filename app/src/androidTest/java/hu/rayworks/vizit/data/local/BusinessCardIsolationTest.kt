package hu.rayworks.vizit.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BusinessCardIsolationTest {
    private lateinit var database: VizitDatabase
    private lateinit var dao: BusinessCardDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            VizitDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.businessCardDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun readsSelectionsAndDeletesStayInsideOwnerBoundary() = runBlocking {
        val sharedProfileId = "11111111-1111-4111-8111-111111111111"
        dao.upsertCards(
            listOf(
                card(ownerId = "owner-a", profileId = sharedProfileId, payload = "a"),
                card(ownerId = "owner-b", profileId = sharedProfileId, payload = "b"),
            ),
        )
        dao.upsertSelection(BusinessCardSelectionEntity("owner-a", sharedProfileId))
        dao.upsertSelection(BusinessCardSelectionEntity("owner-b", sharedProfileId))

        assertEquals(listOf("a"), dao.observeCards("owner-a").first().map { it.payloadJson })
        assertEquals(listOf("b"), dao.observeCards("owner-b").first().map { it.payloadJson })

        dao.deleteOwnedCardAndSelectNext("owner-a", sharedProfileId)

        assertNull(dao.getCard("owner-a", sharedProfileId))
        assertEquals("b", dao.getCard("owner-b", sharedProfileId)?.payloadJson)
        assertEquals(sharedProfileId, dao.getSelection("owner-b")?.profileId)
    }

    @Test
    fun replacingOneOwnerRejectsForeignEntities() = runBlocking {
        var rejected = false
        try {
            dao.replaceOwnerCards(
                "owner-a",
                listOf(card(ownerId = "owner-b", profileId = "22222222-2222-4222-8222-222222222222")),
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    private fun card(ownerId: String, profileId: String, payload: String = "{}") =
        BusinessCardCacheEntity(
            ownerId = ownerId,
            profileId = profileId,
            payloadJson = payload,
            fingerprint = "0".repeat(64),
            isPrimary = true,
            createdAt = "2026-10-01T00:00:00Z",
            updatedAt = "2026-10-01T00:00:00Z",
            cachedAtEpochMs = 0,
        )
}
