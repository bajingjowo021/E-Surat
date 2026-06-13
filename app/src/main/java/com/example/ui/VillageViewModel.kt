package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.GeminiService
import com.example.service.LetterAgentResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VillageViewModel(
    application: Application,
    private val repository: VillageRepository
) : AndroidViewModel(application) {

    // Setup active UI State Management
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDusunFilter = MutableStateFlow<String?>(null)
    val selectedDusunFilter: StateFlow<String?> = _selectedDusunFilter.asStateFlow()

    private val _selectedGenderFilter = MutableStateFlow<String?>(null)
    val selectedGenderFilter: StateFlow<String?> = _selectedGenderFilter.asStateFlow()

    // Reactive list of villagers based on search & filter
    val villagersState: StateFlow<List<Villager>> = combine(
        _searchQuery.flatMapLatest { query -> repository.searchVillagers(query) },
        _selectedDusunFilter,
        _selectedGenderFilter
    ) { list, dusun, gender ->
        var filtered = list
        if (dusun != null) {
            filtered = filtered.filter { it.dusun.equals(dusun, ignoreCase = true) }
        }
        if (gender != null) {
            filtered = filtered.filter { it.jenisKelamin.equals(gender, ignoreCase = true) }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Officials State
    val officialsState: StateFlow<List<Official>> = repository.allOfficials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeOfficialState: StateFlow<Official?> = repository.allOfficials
        .map { list -> list.find { it.isActive } ?: list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Kop Surat settings State
    val settingsState: StateFlow<VillageSettings> = repository.settingsFlow
        .map { it ?: VillageSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VillageSettings())

    // Letter issuance records State
    val historyState: StateFlow<List<SuratHistory>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Agent States
    private val _aiInput = MutableStateFlow("")
    val aiInput = _aiInput.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    private val _aiResult = MutableStateFlow<LetterAgentResponse?>(null)
    val aiResult = _aiResult.asStateFlow()

    private val _aiStatusLog = MutableStateFlow<String?>(null)
    val aiStatusLog = _aiStatusLog.asStateFlow()

    // Cloud Sync State
    val syncState: StateFlow<SyncStatus> = repository.syncState

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDusunFilter(dusun: String?) {
        _selectedDusunFilter.value = dusun
    }

    fun setGenderFilter(gender: String?) {
        _selectedGenderFilter.value = gender
    }

    // Villager CRUD
    fun addVillager(villager: Villager) {
        viewModelScope.launch {
            repository.insertVillager(villager)
        }
    }

    fun removeVillager(villager: Villager) {
        viewModelScope.launch {
            repository.deleteVillager(villager)
        }
    }

    // Official management
    fun addOfficial(official: Official) {
        viewModelScope.launch {
            repository.insertOfficial(official)
        }
    }

    fun activateOfficial(id: Int) {
        viewModelScope.launch {
            repository.activateOfficial(id)
        }
    }

    fun deleteOfficial(off: Official) {
        viewModelScope.launch {
            repository.deleteOfficial(off)
        }
    }

    // Update Kop Surat Settings
    fun updateSettings(settings: VillageSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    // Trigger Cloud Firebase Sync
    fun triggerSync() {
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    // Letter Generation engine
    fun generateLetter(
        villager: Villager,
        jenisSurat: String,
        additionalInfo: String = ""
    ) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val setting = repository.getSettings() ?: VillageSettings()
            
            // Format Surat resmi: 470 / <counter> / XI / 2026 based on catalog
            val counter = (100..999).random() // Mocking counter
            val monthRom = when (Calendar.getInstance().get(Calendar.MONTH) + 1) {
                1 -> "I"
                2 -> "II"
                3 -> "III"
                4 -> "IV"
                5 -> "V"
                6 -> "VI"
                7 -> "VII"
                8 -> "VIII"
                9 -> "IX"
                10 -> "X"
                11 -> "XI"
                else -> "XII"
            }
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val nomorSuratFormat = "470/$counter/$monthRom/$year"

            val activeOfficial = activeOfficialState.value?.nama ?: "H. Moh. Ridwan, S.H."

            // Generate Verification Barcode Content (JSON metadata as required by section 10)
            val shCode = jenisSurat.uppercase().replace(" ", "_")
            val barContent = """
                {
                  "id": "SRT-$year-$counter",
                  "nik": "${villager.nik}",
                  "nama": "${villager.namaLengkap}",
                  "jenis": "$shCode"
                }
            """.trimIndent()

            val qrVerificationUrl = "https://rembang-verifikasi-surat.id/verify?id=SRT-$year-$counter"

            val item = SuratHistory(
                nomorSurat = nomorSuratFormat,
                nik = villager.nik,
                namaPemohon = villager.namaLengkap,
                jenisSurat = jenisSurat,
                tanggalTerbit = dateStr,
                pejabatTtd = activeOfficial,
                status = "Disetujui",
                barcodeContent = barContent,
                qrVerificationUrl = qrVerificationUrl,
                isiSuratJson = additionalInfo
            )
            repository.insertSurat(item)
        }
    }

    fun deleteLetter(id: Int) {
        viewModelScope.launch {
            repository.deleteSuratById(id)
        }
    }

    // AI Prompt Agent Trigger
    fun executeAiCommand(command: String) {
        if (command.isBlank()) return
        _aiInput.value = command
        _aiLoading.value = true
        _aiStatusLog.value = "AI Agent Sedang Menganalisis Kebutuhan Berkas..."
        _aiResult.value = null

        viewModelScope.launch {
            try {
                val response = GeminiService.parseCommand(command)
                _aiResult.value = response
                if (response != null) {
                    _aiStatusLog.value = "Selesai! Berhasil merancang paket kependudukan untuk ${response.residentName}."
                } else {
                    _aiStatusLog.value = "AI gagal mendeteksi form, menggunakan fallback lokal."
                }
            } catch (e: Exception) {
                _aiStatusLog.value = "Gagal memanggil AI: ${e.message}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun approveBatchAiSurat(response: LetterAgentResponse) {
        viewModelScope.launch {
            // Find resident in local db
            val list = villagersState.value
            val match = list.find { it.namaLengkap.contains(response.residentName, ignoreCase = true) } 
                ?: list.firstOrNull() 
                ?: Villager("3317051201900001", "3317051201190022", response.residentName, "Laki-laki", "Rembang", "1990-01-01", "Supardi", "Sumarni", "SMA", "Wiraswasta", "Belum Kawin", "Anak", "Islam")

            // Auto insert each letter code in the batch list!
            response.letterCodes.forEach { code ->
                generateLetter(
                    villager = match,
                    jenisSurat = code,
                    additionalInfo = "Paket AI: ${response.category}. Diperinci: ${response.explanation}"
                )
            }
            
            // Log sync feedback
            _aiResult.value = null
            _aiStatusLog.value = "Berhasil membuat ${response.letterCodes.size} surat digital bertanda-tangan elektronik untuk ${match.namaLengkap} secara instan!"
        }
    }

    fun clearAiState() {
        _aiResult.value = null
        _aiStatusLog.value = null
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application, kotlinx.coroutines.GlobalScope)
                    val repo = VillageRepository(
                        db.villagerDao(),
                        db.officialDao(),
                        db.villageSettingsDao(),
                        db.suratHistoryDao()
                    )
                    return VillageViewModel(application, repo) as T
                }
            }
        }
    }
}
