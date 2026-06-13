package com.example.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VillageRepository(
    private val villagerDao: VillagerDao,
    private val officialDao: OfficialDao,
    private val villageSettingsDao: VillageSettingsDao,
    private val suratHistoryDao: SuratHistoryDao
) {
    // Villagers
    val allVillagers: Flow<List<Villager>> = villagerDao.getAllVillagers()
    
    fun searchVillagers(query: String): Flow<List<Villager>> {
        return if (query.isBlank()) {
            allVillagers
        } else {
            villagerDao.searchVillagers("%$query%")
        }
    }

    suspend fun getVillagerByNik(nik: String): Villager? = villagerDao.getVillagerByNik(nik)
    suspend fun insertVillager(villager: Villager) = villagerDao.insertVillager(villager)
    suspend fun deleteVillager(villager: Villager) = villagerDao.deleteVillager(villager)

    // Officials
    val allOfficials: Flow<List<Official>> = officialDao.getAllOfficials()
    suspend fun insertOfficial(official: Official) = officialDao.insertOfficial(official)
    suspend fun deleteOfficial(official: Official) = officialDao.deleteOfficial(official)
    
    suspend fun activateOfficial(id: Int) {
        officialDao.deactivateAll()
        officialDao.activateOfficial(id)
    }

    // Settings
    val settingsFlow: Flow<VillageSettings?> = villageSettingsDao.getSettingsFlow()
    suspend fun getSettings(): VillageSettings? = villageSettingsDao.getSettings()
    suspend fun saveSettings(settings: VillageSettings) = villageSettingsDao.insertSettings(settings)

    // History
    val allHistory: Flow<List<SuratHistory>> = suratHistoryDao.getAllHistory()
    suspend fun getSuratById(id: Int): SuratHistory? = suratHistoryDao.getSuratById(id)
    suspend fun insertSurat(surat: SuratHistory): Long = suratHistoryDao.insertSurat(surat)
    suspend fun deleteSuratById(id: Int) = suratHistoryDao.deleteSuratById(id)

    // Cloud Firestore Sync Mocking
    private val _syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    suspend fun syncWithCloud() {
        _syncState.value = SyncStatus.Syncing(0)
        delay(800)
        _syncState.value = SyncStatus.Syncing(30)
        delay(600)
        _syncState.value = SyncStatus.Syncing(75)
        delay(500)
        _syncState.value = SyncStatus.Success("6 Berkas Kependudukan & 4 Riwayat Surat telah disinkronisasikan aman ke Firebase Firestore Cloud.")
        delay(3000)
        _syncState.value = SyncStatus.Idle
    }
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    data class Syncing(val progress: Int) : SyncStatus
    data class Success(val message: String) : SyncStatus
    data class Error(val error: String) : SyncStatus
}
