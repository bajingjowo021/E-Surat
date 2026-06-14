package com.bajingjowo.esurat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Villager::class, Official::class, VillageSettings::class, SuratHistory::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun villagerDao(): VillagerDao
    abstract fun officialDao(): OfficialDao
    abstract fun villageSettingsDao(): VillageSettingsDao
    abstract fun suratHistoryDao(): SuratHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "e_surat_desa_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            val database = getDatabase(context, scope)
                            prepopulateData(database)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateData(db: AppDatabase) {
            // Default settings
            db.villageSettingsDao().insertSettings(
                VillageSettings(
                    id = 1,
                    namaDesa = "Soditan",
                    kecamatan = "Lasem",
                    kabupaten = "Rembang",
                    provinsi = "Jawa Tengah",
                    alamat = "Jl. Raya Soditan No. 12",
                    kodePos = "59271",
                    email = "desa.soditan@rembangkab.go.id"
                )
            )

            // Default officials
            val officials = listOf(
                Official(nama = "H. Moh. Ridwan, S.H.", nip = "196904121990031002", jabatan = "Kepala Desa (Kades)", isActive = true),
                Official(nama = "Drs. Joko Prasetyo", nip = "197308151998031001", jabatan = "Sekretaris Desa (Sekdes)", isActive = false),
                Official(nama = "Saraswati, S.E.", nip = "198511022010012004", jabatan = "Kasi Pemerintahan", isActive = false),
                Official(nama = "Bambang Wijaya", nip = "198002202008011003", jabatan = "Kaur Umum", isActive = false)
            )
            for (off in officials) {
                db.officialDao().insertOfficial(off)
            }

            // Default villagers
            val villagers = listOf(
                Villager(
                    nik = "3317051201900001",
                    noKk = "3317051201190022",
                    namaLengkap = "Ahmad Fauzi",
                    jenisKelamin = "Laki-laki",
                    tempatLahir = "Rembang",
                    tanggalLahir = "1990-01-12",
                    namaAyah = "Supardi",
                    namaIbu = "Sumarni",
                    pendidikan = "S1 Teknik Informatika",
                    pekerjaan = "Wiraswasta",
                    statusPerkawinan = "Belum Kawin",
                    hubunganKeluarga = "Anak",
                    agama = "Islam",
                    rt = "02",
                    rw = "01",
                    dusun = "Dusun Mulyo",
                    alamat = "RT 02 RW 01, Dusun Mulyo"
                ),
                Villager(
                    nik = "3317054403920002",
                    noKk = "3317051201190022",
                    namaLengkap = "Siti Aminah",
                    jenisKelamin = "Perempuan",
                    tempatLahir = "Rembang",
                    tanggalLahir = "1992-03-04",
                    namaAyah = "Sumarto",
                    namaIbu = "Sulasmi",
                    pendidikan = "D3 Administrasi",
                    pekerjaan = "Karyawan Swasta",
                    statusPerkawinan = "Belum Kawin",
                    hubunganKeluarga = "Anak",
                    agama = "Islam",
                    rt = "02",
                    rw = "01",
                    dusun = "Dusun Mulyo",
                    alamat = "RT 02 RW 01, Dusun Mulyo"
                ),
                Villager(
                    nik = "3317051508720001",
                    noKk = "3317051201190022",
                    namaLengkap = "Supardi",
                    jenisKelamin = "Laki-laki",
                    tempatLahir = "Lasem",
                    tanggalLahir = "1972-08-15",
                    namaAyah = "Sastro",
                    namaIbu = "Sumiati",
                    pendidikan = "SMA",
                    pekerjaan = "Petani",
                    statusPerkawinan = "Kawin",
                    hubunganKeluarga = "Kepala Keluarga",
                    agama = "Islam",
                    rt = "02",
                    rw = "01",
                    dusun = "Dusun Mulyo",
                    alamat = "RT 02 RW 01, Dusun Mulyo"
                ),
                Villager(
                    nik = "3317055209750003",
                    noKk = "3317051201190022",
                    namaLengkap = "Sumarni",
                    jenisKelamin = "Perempuan",
                    tempatLahir = "Lasem",
                    tanggalLahir = "1975-09-12",
                    namaAyah = "Kardi",
                    namaIbu = "Karsini",
                    pendidikan = "SMA",
                    pekerjaan = "Mengurus Rumah Tangga",
                    statusPerkawinan = "Kawin",
                    hubunganKeluarga = "Isteri",
                    agama = "Islam",
                    rt = "02",
                    rw = "01",
                    dusun = "Dusun Mulyo",
                    alamat = "RT 02 RW 01, Dusun Mulyo"
                ),
                Villager(
                    nik = "3317051806850004",
                    noKk = "3317051806150033",
                    namaLengkap = "Budi Santoso",
                    jenisKelamin = "Laki-laki",
                    tempatLahir = "Rembang",
                    tanggalLahir = "1985-06-18",
                    namaAyah = "Hardono",
                    namaIbu = "Siti Rahma",
                    pendidikan = "D4 / S1",
                    pekerjaan = "PNS",
                    statusPerkawinan = "Kawin",
                    hubunganKeluarga = "Kepala Keluarga",
                    agama = "Islam",
                    rt = "01",
                    rw = "02",
                    dusun = "Dusun Krajan",
                    alamat = "RT 01 RW 02, Dusun Krajan"
                ),
                Villager(
                    nik = "3317052010880005",
                    noKk = "3317051806150033",
                    namaLengkap = "Dewi Lestari",
                    jenisKelamin = "Perempuan",
                    tempatLahir = "Kudus",
                    tanggalLahir = "1988-10-20",
                    namaAyah = "Priyanto",
                    namaIbu = "Partini",
                    pendidikan = "S1 Ekonomi",
                    pekerjaan = "Guru",
                    statusPerkawinan = "Kawin",
                    hubunganKeluarga = "Isteri",
                    agama = "Islam",
                    rt = "01",
                    rw = "02",
                    dusun = "Dusun Krajan",
                    alamat = "RT 01 RW 02, Dusun Krajan"
                )
            )
            db.villagerDao().insertAll(villagers)
        }
    }
}
