package hu.rayworks.vizit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessCardDao {
    @Query(
        """
        SELECT * FROM business_card_cache
        WHERE ownerId = :ownerId
        ORDER BY isPrimary DESC, createdAt, profileId
        """,
    )
    fun observeCards(ownerId: String): Flow<List<BusinessCardCacheEntity>>

    @Query("SELECT * FROM business_card_selection WHERE ownerId = :ownerId LIMIT 1")
    fun observeSelection(ownerId: String): Flow<BusinessCardSelectionEntity?>

    @Query(
        """
        SELECT * FROM business_card_cache
        WHERE ownerId = :ownerId AND profileId = :profileId
        LIMIT 1
        """,
    )
    suspend fun getCard(ownerId: String, profileId: String): BusinessCardCacheEntity?

    @Query(
        """
        SELECT * FROM business_card_cache
        WHERE ownerId = :ownerId
        ORDER BY isPrimary DESC, createdAt, profileId
        """,
    )
    suspend fun getCards(ownerId: String): List<BusinessCardCacheEntity>

    @Query("SELECT * FROM business_card_selection WHERE ownerId = :ownerId LIMIT 1")
    suspend fun getSelection(ownerId: String): BusinessCardSelectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCards(cards: List<BusinessCardCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCard(card: BusinessCardCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSelection(selection: BusinessCardSelectionEntity)

    @Query("DELETE FROM business_card_cache WHERE ownerId = :ownerId")
    suspend fun deleteCards(ownerId: String)

    @Query("DELETE FROM business_card_cache WHERE ownerId = :ownerId AND profileId = :profileId")
    suspend fun deleteCard(ownerId: String, profileId: String): Int

    @Query("DELETE FROM business_card_selection WHERE ownerId = :ownerId")
    suspend fun deleteSelection(ownerId: String)

    @Transaction
    suspend fun replaceOwnerCards(ownerId: String, cards: List<BusinessCardCacheEntity>) {
        require(cards.all { it.ownerId == ownerId })
        val previousSelection = getSelection(ownerId)?.profileId
        deleteCards(ownerId)
        if (cards.isNotEmpty()) upsertCards(cards)
        val nextSelection = previousSelection?.takeIf { selected -> cards.any { it.profileId == selected } }
            ?: cards.firstOrNull { it.isPrimary }?.profileId
            ?: cards.firstOrNull()?.profileId
        if (nextSelection == null) deleteSelection(ownerId)
        else upsertSelection(BusinessCardSelectionEntity(ownerId, nextSelection))
    }

    @Transaction
    suspend fun deleteOwnedCardAndSelectNext(ownerId: String, profileId: String) {
        deleteCard(ownerId, profileId)
        val selected = getSelection(ownerId)?.profileId
        if (selected == profileId || selected == null) {
            val next = getCards(ownerId).firstOrNull()?.profileId
            if (next == null) deleteSelection(ownerId)
            else upsertSelection(BusinessCardSelectionEntity(ownerId, next))
        }
    }

    @Transaction
    suspend fun deleteOwnerData(ownerId: String) {
        deleteSelection(ownerId)
        deleteCards(ownerId)
    }
}
