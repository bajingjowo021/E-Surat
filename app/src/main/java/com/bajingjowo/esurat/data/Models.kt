package com.bajingjowo.esurat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "villagers")
data class Villager(
    @PrimaryKey val nik: String,
    val noKk: String,
    val namaLengkap: String,
    val jenisKelamin: String, // Laki-laki / Perempuan
    val tempatLahir: String,
    val tanggalLahir: String,
    val namaAyah: String,
    val namaIbu: String,
    val pendidikan: String,
    val pekerjaan: String,
    val statusPerkawinan: String,
    val hubunganKeluarga: String,
    val agama: String,
    val kewarganegaraan: String = "WNI",
    val rt: String = "01",
    val rw: String = "01",
    val dusun: String = "Dusun Krajan",
    val alamat: String = "RT $rt RW $rw, $dusun"
)

@Entity(tableName = "officials")
data class Official(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val nip: String,
    val jabatan: String,
    val ttdPath: String = "", // Base64 or standard visual graphic
    val isActive: Boolean = false
)

@Entity(tableName = "village_settings")
data class VillageSettings(
    @PrimaryKey val id: Int = 1, // Only 1 settings row active
    val namaDesa: String = "Soditan",
    val kecamatan: String = "Lasem",
    val kabupaten: String = "Rembang",
    val provinsi: String = "Jawa Tengah",
    val alamat: String = "Jl. Raya Soditan No. 12",
    val kodePos: String = "59271",
    val logoUrl: String = "" // Placeholder or empty
)

@Entity(tableName = "surat_history")
data class SuratHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nomorSurat: String,
    val nik: String,
    val namaPemohon: String,
    val jenisSurat: String,
    val tanggalTerbit: String,
    val pejabatTtd: String,
    val status: String = "Draft", // Draft, Disetujui
    val barcodeContent: String = "",
    val qrVerificationUrl: String = "",
    val isiSuratJson: String = "" // Custom inputs serialized
)
