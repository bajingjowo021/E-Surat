package com.bajingjowo.esurat.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VillagerDao {
    @Query("SELECT * FROM villagers ORDER BY namaLengkap ASC")
    fun getAllVillagers(): Flow<List<Villager>>

    @Query("SELECT * FROM villagers WHERE nik = :nik LIMIT 1")
    suspend fun getVillagerByNik(nik: String): Villager?

    @Query("SELECT * FROM villagers WHERE nik LIKE :query OR namaLengkap LIKE :query OR noKk LIKE :query ORDER BY namaLengkap ASC")
    fun searchVillagers(query: String): Flow<List<Villager>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillager(villager: Villager)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(villagers: List<Villager>)

    @Delete
    suspend fun deleteVillager(villager: Villager)
}

@Dao
interface OfficialDao {
    @Query("SELECT * FROM officials ORDER BY jabatan ASC")
    fun getAllOfficials(): Flow<List<Official>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfficial(official: Official)

    @Delete
    suspend fun deleteOfficial(official: Official)

    @Query("UPDATE officials SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE officials SET isActive = 1 WHERE id = :id")
    suspend fun activateOfficial(id: Int)
}

@Dao
interface VillageSettingsDao {
    @Query("SELECT * FROM village_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<VillageSettings?>

    @Query("SELECT * FROM village_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): VillageSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: VillageSettings)
}

@Dao
interface SuratHistoryDao {
    @Query("SELECT * FROM surat_history ORDER BY id DESC")
    fun getAllHistory(): Flow<List<SuratHistory>>

    @Query("SELECT * FROM surat_history WHERE id = :id LIMIT 1")
    suspend fun getSuratById(id: Int): SuratHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurat(surat: SuratHistory): Long

    @Query("DELETE FROM surat_history WHERE id = :id")
    suspend fun deleteSuratById(id: Int)
}
