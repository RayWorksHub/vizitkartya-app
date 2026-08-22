package hu.rayworks.vizit

import android.app.Application
import hu.rayworks.vizit.data.ContactProfileRepository
import hu.rayworks.vizit.data.local.LegacyContactProfileStore
import hu.rayworks.vizit.data.local.RoomProfileStore
import hu.rayworks.vizit.data.local.VizitDatabase
import hu.rayworks.vizit.data.remote.SupabaseProfileRemoteDataSource
import hu.rayworks.vizit.data.remote.SupabaseProvider
import hu.rayworks.vizit.data.settings.AppSettingsStore
import hu.rayworks.vizit.data.sync.ProfileSyncEngine
import hu.rayworks.vizit.data.sync.WorkManagerProfileSyncScheduler

class VizitApplication : Application() {
    lateinit var container: VizitAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = VizitAppContainer(this)
    }
}

class VizitAppContainer(application: Application) {
    val settingsStore = AppSettingsStore(application)
    private val database = VizitDatabase.get(application)
    private val localStore = RoomProfileStore(database.profileDao())
    private val syncScheduler = WorkManagerProfileSyncScheduler(application)
    private val remoteDataSource = SupabaseProfileRemoteDataSource(SupabaseProvider.getOrNull())

    val profileRepository = ContactProfileRepository(
        localStore = localStore,
        settingsStore = settingsStore,
        legacyStore = LegacyContactProfileStore(application),
        syncScheduler = syncScheduler,
    )
    val profileSyncEngine = ProfileSyncEngine(
        localStore = localStore,
        remoteDataSource = remoteDataSource,
    )
}
