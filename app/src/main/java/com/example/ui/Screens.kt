@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Official
import com.example.data.SuratHistory
import com.example.data.SyncStatus
import com.example.data.Villager
import com.example.ui.theme.*

// Simple representing destinations
enum class Screen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Dashboard("Dashboard", Icons.Default.Home),
    Penduduk("Penduduk", Icons.Default.Person),
    Surat("Buat Surat", Icons.Default.Send),
    AIAgent("Asisten AI", Icons.Default.Build),
    DraftKK("Draft KK", Icons.Default.List),
    Riwayat("Arsip Surat", Icons.Default.Refresh),
    Pengaturan("Pengaturan", Icons.Default.Settings)
}

@Composable
fun AppNavigationScaffold(
    viewModel: VillageViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    val syncState by viewModel.syncState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    // Screen navigation layout adaptive selection (Drawer rail on wide, bottom navigation bar on mobile screen)
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWideScreen = maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                // Draw elegant sidebar
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(100.dp).fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send, 
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Screen.values().forEach { screen ->
                        NavigationRailItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }

            // Main Content Area
            Scaffold(
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            Screen.values().forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title, fontSize = 9.sp, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { target ->
                        when (target) {
                            Screen.Dashboard -> DashboardScreen(viewModel, onNavigate = { currentScreen = it })
                            Screen.Penduduk -> VillagersScreen(viewModel)
                            Screen.Surat -> CreateLetterScreen(viewModel)
                            Screen.AIAgent -> AiAgentScreen(viewModel)
                            Screen.DraftKK -> DraftKkScreen(viewModel)
                            Screen.Riwayat -> HistoryLettersScreen(viewModel)
                            Screen.Pengaturan -> SettingsScreen(viewModel)
                        }
                    }

                    // Global Top Sync Alert toast overlay if triggering syncing state
                    SyncIndicatorOverlay(
                        syncState = syncState,
                        onDismiss = { viewModel.triggerSync() }
                    )
                }
            }
        }
    }
}

// 1. DASHBOARD COMPOSABLE SCREEN
@Composable
fun DashboardScreen(viewModel: VillageViewModel, onNavigate: (Screen) -> Unit) {
    val villagers by viewModel.villagersState.collectAsState()
    val history by viewModel.historyState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val activeOfficial by viewModel.activeOfficialState.collectAsState()

    // Calculate metrics
    val totalVillagers = villagers.size
    val totalKks = villagers.distinctBy { it.noKk }.size
    val suratToday = history.filter { it.tanggalTerbit == "13/06/2026" }.size // Static test date matching local time
    val suratMonth = history.size
    val syncState by viewModel.syncState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "e-Surat Desa Digital",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sistem Administrasi Desa ${settings.namaDesa} • Kec. ${settings.kecamatan}, Kab. ${settings.kabupaten}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.AIAgent) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("dashboard_quick_ai")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "AI")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Asisten AI")
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.triggerSync() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Cloud")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sinkron Cloud")
                        }
                    }
                }
            }
        }

        // Metrics Grid (2x2 Grid)
        item {
            Text(
                text = "Statistik Pelayanan Hari Ini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Penduduk",
                    value = "$totalVillagers Jiwa",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Kepala Keluarga",
                    value = "$totalKks KK",
                    icon = Icons.Default.List,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Surat Hari Ini",
                    value = "$suratToday Berkas",
                    icon = Icons.Default.Send,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Arsip",
                    value = "$suratMonth Berkas",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Official Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = "Official",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Pejabat Penandatangan Aktif:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = activeOfficial?.nama ?: "Belum Memilih Penandatangan",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeOfficial?.jabatan ?: "Staff"} • NIP. ${activeOfficial?.nip ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Quick shortcut list
        item {
            Text(
                text = "Aksi Administrasi Cepat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    title = "Tambah Penduduk",
                    onClick = { onNavigate(Screen.Penduduk) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Buat Surat Baru",
                    onClick = { onNavigate(Screen.Surat) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun QuickActionButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

// 2. DATA PENDUDUK SCREENS COMPOSABLE
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VillagersScreen(viewModel: VillageViewModel) {
    val villagers by viewModel.villagersState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDusun by viewModel.selectedDusunFilter.collectAsState()
    val selectedGender by viewModel.selectedGenderFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Filter header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Cari NIK, Nama, No KK...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_villager_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_villager_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Villager")
            }
        }

        // Filter chips bar
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Dusun Mulyo filter
            FilterChip(
                selected = selectedDusun == "Dusun Mulyo",
                onClick = { viewModel.setDusunFilter(if (selectedDusun == "Dusun Mulyo") null else "Dusun Mulyo") },
                label = { Text("Dusun Mulyo") }
            )
            // Dusun Krajan filter
            FilterChip(
                selected = selectedDusun == "Dusun Krajan",
                onClick = { viewModel.setDusunFilter(if (selectedDusun == "Dusun Krajan") null else "Dusun Krajan") },
                label = { Text("Dusun Krajan") }
            )
            // Laki-laki filter
            FilterChip(
                selected = selectedGender == "Laki-laki",
                onClick = { viewModel.setGenderFilter(if (selectedGender == "Laki-laki") null else "Laki-laki") },
                label = { Text("Laki-laki") }
            )
            // Perempuan filter
            FilterChip(
                selected = selectedGender == "Perempuan",
                onClick = { viewModel.setGenderFilter(if (selectedGender == "Perempuan") null else "Perempuan") },
                label = { Text("Perempuan") }
            )
        }

        // Residents Registry list
        Text(
            text = "Registry Kependudukan (${villagers.size} Terdaftar)",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        if (villagers.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = "Empty", 
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Data Penduduk Tidak Ditemukan", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(villagers, key = { it.nik }) { villager ->
                    VillagerItemCard(
                        villager = villager,
                        onDelete = { viewModel.removeVillager(villager) }
                    )
                }
            }
        }
    }

    // Add Resident dialog
    if (showAddDialog) {
        AddVillagerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = {
                viewModel.addVillager(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun VillagerItemCard(
    villager: Villager,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("villager_card_${villager.nik}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = villager.namaLengkap,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Info rows
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("N.I.K:", fontSize = 10.sp, color = Color.Gray)
                    Text(villager.nik, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nomor KK:", fontSize = 10.sp, color = Color.Gray)
                    Text(villager.noKk, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tempat, Tanggal Lahir:", fontSize = 10.sp, color = Color.Gray)
                    Text("${villager.tempatLahir}, ${villager.tanggalLahir}", fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alamat / Dusun:", fontSize = 10.sp, color = Color.Gray)
                    Text("${villager.dusun} RT ${villager.rt} RW ${villager.rw}", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pendidikan / Hubungan KK:", fontSize = 10.sp, color = Color.Gray)
                    Text("${villager.pendidikan} • ${villager.hubunganKeluarga}", fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Orang Tua (Ayah / Ibu):", fontSize = 10.sp, color = Color.Gray)
                    Text("${villager.namaAyah} / ${villager.namaIbu}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AddVillagerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Villager) -> Unit
) {
    var nik by remember { mutableStateOf("") }
    var noKk by remember { mutableStateOf("") }
    var namaLengkap by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Laki-laki") }
    var tempatLahir by remember { mutableStateOf("Rembang") }
    var tanggalLahir by remember { mutableStateOf("1995-10-10") }
    var namaAyah by remember { mutableStateOf("") }
    var namaIbu by remember { mutableStateOf("") }
    var pendidikan by remember { mutableStateOf("SMA") }
    var pekerjaan by remember { mutableStateOf("Wiraswasta") }
    var statusKawin by remember { mutableStateOf("Belum Kawin") }
    var hubKeluarga by remember { mutableStateOf("Anak") }
    var agama by remember { mutableStateOf("Islam") }
    var rt by remember { mutableStateOf("01") }
    var rw by remember { mutableStateOf("01") }
    var dusun by remember { mutableStateOf("Dusun Mulyo") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Registrasi Penduduk Baru", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Divider()

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = nik,
                            onValueChange = { if (it.length <= 16) nik = it },
                            label = { Text("N.I.K. (16 Digit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("add_nik_field"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = noKk,
                            onValueChange = { if (it.length <= 16) noKk = it },
                            label = { Text("Nomor Kartu Keluarga (KK)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("add_kk_field"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = namaLengkap,
                            onValueChange = { namaLengkap = it },
                            label = { Text("Nama Lengkap") },
                            modifier = Modifier.fillMaxWidth().testTag("add_name_field"),
                            singleLine = true
                        )
                    }
                    item {
                        // Gender Row
                        Text("Jenis Kelamin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedGender == "Laki-laki", onClick = { selectedGender = "Laki-laki" })
                                Text("Laki-laki", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedGender == "Perempuan", onClick = { selectedGender = "Perempuan" })
                                Text("Perempuan", fontSize = 14.sp)
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tempatLahir,
                                onValueChange = { tempatLahir = it },
                                label = { Text("Tempat Lahir") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = tanggalLahir,
                                onValueChange = { tanggalLahir = it },
                                label = { Text("Tgl Lahir (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = namaAyah,
                                onValueChange = { namaAyah = it },
                                label = { Text("Nama Ayah") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = namaIbu,
                                onValueChange = { namaIbu = it },
                                label = { Text("Nama Ibu") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rt,
                                onValueChange = { rt = it },
                                label = { Text("RT") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rw,
                                onValueChange = { rw = it },
                                label = { Text("RW") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dusun,
                                onValueChange = { dusun = it },
                                label = { Text("Dusun") },
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }

                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nik.length == 16 && noKk.length == 16 && namaLengkap.isNotBlank()) {
                                onConfirm(
                                    Villager(
                                        nik = nik, noKk = noKk, namaLengkap = namaLengkap, jenisKelamin = selectedGender,
                                        tempatLahir = tempatLahir, tanggalLahir = tanggalLahir, namaAyah = namaAyah, namaIbu = namaIbu,
                                        pendidikan = pendidikan, pekerjaan = pekerjaan, statusPerkawinan = statusKawin,
                                        hubunganKeluarga = hubKeluarga, agama = agama, rt = rt, rw = rw, dusun = dusun,
                                        alamat = "RT $rt RW $rw, $dusun, Soditan"
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("submit_villager")
                    ) {
                        Text("Simpan data", color = Color.White)
                    }
                }
            }
        }
    }
}

// 3. LETTER PRODUCTION SCREENS COMPOSABLE
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateLetterScreen(viewModel: VillageViewModel) {
    val villagers by viewModel.villagersState.collectAsState()
    val activeOfficial by viewModel.activeOfficialState.collectAsState()

    var selectedVillager by remember { mutableStateOf<Villager?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedCategoryTab by remember { mutableStateOf(0) } // 0: SK, 1: SP, 2: Form
    var customNotes by remember { mutableStateOf("") }
    var showingSuccessToast by remember { mutableStateOf(false) }
    var createdLetterName by remember { mutableStateOf("") }

    // Official checklist from PRD Section 7
    val suratKeterangan = listOf(
        "Surat Keterangan Umum", "Surat Keterangan Usaha", "Surat Keterangan Tidak Mampu (SKTM)",
        "Surat Keterangan Domisili Tempat Tinggal", "Surat Keterangan Domisili Lembaga", "Surat Keterangan Wali Nikah",
        "Surat Keterangan Belum Pernah Menikah", "Surat Keterangan Wali Hakim", "Surat Keterangan Ayah/Ibu Meninggal"
    )

    val suratPengantar = listOf(
        "Surat Pengantar Umum", "Surat Pengantar Nikah", "Pengantar F-1.02", "Pengantar Pindah F-1.03",
        "Pengantar Kelahiran F-2.01"
    )

    val formulirKependudukan = listOf(
        "N1 (Surat Pengantar Nikah)", "N2 (Permohonan Kehendak)", "N3 (Persetujuan Mempelai)",
        "N4 (Izin Orang Tua)", "N5 (Izin Wali)", "Biodata WNI F-1.01", "Formulir F-1.06",
        "SPTJM Kelahiran F-2.03", "SPTJM Pernikahan F-2.04", "SPTJM Kebenaran Data Kelahiran"
    )

    val currentLettersList = when (selectedCategoryTab) {
        0 -> suratKeterangan
        1 -> suratPengantar
        else -> formulirKependudukan
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Mesin Template Surat Resmi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Sesuaikan format template Disdukcapil Kabupaten Rembang secara instan berdasarkan data penduduk lokal.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Divider()
        }

        // STEP 1: Select Resident
        item {
            Text("Langkah 1: Pilih Penduduk Penerima Surat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedDropdown = !expandedDropdown }
                        .testTag("select_resident_anchor"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedVillager?.let { "${it.namaLengkap} - NIK. ${it.nik}" } 
                                ?: "Klik untuk memilih penduduk dari database...",
                            fontWeight = if (selectedVillager != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedVillager != null) MaterialTheme.colorScheme.onSurface else Color.Gray,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = if (expandedDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                            contentDescription = null
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                ) {
                    villagers.forEach { villager ->
                        DropdownMenuItem(
                            text = { Text("${villager.namaLengkap} (${villager.nik}) - RT ${villager.rt}") },
                            onClick = {
                                selectedVillager = villager
                                expandedDropdown = false
                            },
                            modifier = Modifier.testTag("resident_item_${villager.nik}")
                        )
                    }
                }
            }
        }

        // STEP 2: Choose Template Group tab
        item {
            Text("Langkah 2: Pilih Kategori Template Disdukcapil", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedCategoryTab, containerColor = Color.Transparent) {
                Tab(
                    selected = selectedCategoryTab == 0,
                    onClick = { selectedCategoryTab = 0 },
                    text = { Text("S. Keterangan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedCategoryTab == 1,
                    onClick = { selectedCategoryTab = 1 },
                    text = { Text("S. Pengantar", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedCategoryTab == 2,
                    onClick = { selectedCategoryTab = 2 },
                    text = { Text("Formulir Dukcapil", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // STEP 3: Choose actual letter
        item {
            Text("Pilih Template Surat:", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                currentLettersList.forEach { letter ->
                    val isSelected = createdLetterName == letter
                    FilterChip(
                        selected = isSelected,
                        onClick = { createdLetterName = letter },
                        label = { Text(letter, fontSize = 11.sp) },
                        modifier = Modifier.testTag("letter_chip_${letter.take(6).trim().replace("/", "_").lowercase()}")
                    )
                }
            }
        }

        // STEP 4: Extra info inputs
        item {
            Text("Langkah 3: Input Karakter Pendukung Surat (Opsional)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customNotes,
                onValueChange = { customNotes = it },
                placeholder = { Text("Contoh: Keperluan mendaftar beasiswa kuliah, Menikah dengan Nurul Arofah di KUA Lasem, dsb...") },
                modifier = Modifier.fillMaxWidth().testTag("custom_notes_field"),
                minLines = 3,
                maxLines = 5
            )
        }

        // Action Button: Publish Letter
        item {
            val isEnabled = selectedVillager != null && createdLetterName.isNotBlank() && activeOfficial != null

            Button(
                onClick = {
                    val villager = selectedVillager
                    if (villager != null) {
                        viewModel.generateLetter(
                            villager = villager,
                            jenisSurat = createdLetterName,
                            additionalInfo = customNotes
                        )
                        showingSuccessToast = true
                        customNotes = ""
                    }
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_create_letter"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buat Surat & Tanda Tangani Elektronik", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (!isEnabled && activeOfficial == null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Peringatan: Mohon register / aktifkan Minimal 1 Perangkat Desa penandatangan di menu Pengaturan terlebih dahulu.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showingSuccessToast) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sukses!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "Berkas '$createdLetterName' untuk ${selectedVillager?.namaLengkap} berhasil dicetak, ditandatangani elektrik digital (QR) dan disimpan ke data arsip lokal.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = { showingSuccessToast = false }) {
                            Icon(Icons.Default.Delete, contentDescription = "Close", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// 4. AI AGENT COMPOSABLE SCREEN
@Composable
fun AiAgentScreen(viewModel: VillageViewModel) {
    val aiInput by viewModel.aiInput.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val aiStatusLog by viewModel.aiStatusLog.collectAsState()
    
    var promptTyped by remember { mutableStateOf("") }

    val examplePrompts = listOf(
        "Buat berkas nikah Ahmad Fauzi",
        "Buatkan surat pindah atas nama Siti Aminah",
        "Permohonan SKTM untuk Budi Santoso karena sakit",
        "Buat berkas kelahiran untuk Supardi dan Sumarni"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Asisten AI e-Surat Desa (Gemini Smart Agent)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Mengetik satu perintah bahasa alami untuk menghasilkan sekumpulan paket administrasi surat resmi sekaligus.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Chat Input box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ketik Perintah Layanan Desa:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = promptTyped,
                        onValueChange = { promptTyped = it },
                        placeholder = { Text("Contoh: Buat semua berkas nikah untuk Ahmad Fauzi...") },
                        modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                        minLines = 2,
                        maxLines = 4,
                        trailingIcon = {
                            if (promptTyped.isNotBlank()) {
                                IconButton(onClick = { promptTyped = "" }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bebas Mati Lampu (Offline Sandbox Ready)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        
                        Button(
                            onClick = {
                                viewModel.executeAiCommand(promptTyped)
                                promptTyped = ""
                            },
                            enabled = promptTyped.isNotBlank() && !aiLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("ai_prompt_submit")
                        ) {
                            if (aiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Parse")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Analisis AI")
                            }
                        }
                    }
                }
            }
        }

        // Preloaded prompt chips
        item {
            Text("Rekomendasi Template Perintah Cepat:", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                examplePrompts.forEach { ex ->
                    FilterChip(
                        selected = false,
                        onClick = { promptTyped = ex },
                        label = { Text(ex, fontSize = 11.sp) },
                        modifier = Modifier.testTag("ai_sample_chip_${ex.take(6).trim().lowercase()}")
                    )
                }
            }
        }

        // Status logs
        if (aiStatusLog != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(aiStatusLog ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Parse Output Result Panel
        if (aiResult != null) {
            val res = aiResult!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("ai_result_box"),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("Hasil Analisis AI Berhasil", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.clearAiState() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Discard")
                            }
                        }

                        Text("Penduduk Terdeteksi: ${res.residentName}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Kategori Layanan: ${res.category}", fontSize = 13.sp, color = Color.Gray)

                        Divider()

                        Text("Daftar Dokumen yang Dirancang Otomatis:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            res.letterCodes.forEach { code ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = VerifyGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(code, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        ) {
                            Text(
                                text = res.explanation,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = { viewModel.approveBatchAiSurat(res) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ai_batch_approve"),
                            colors = ButtonDefaults.buttonColors(containerColor = VerifyGreen)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Approve")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Terbitkan & Cetak ${res.letterCodes.size} Berkas Sekaligus", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 5. DRAFT KARTU KELUARGA VIEWER COMPOSABLE
@Composable
fun DraftKkScreen(viewModel: VillageViewModel) {
    val villagers by viewModel.villagersState.collectAsState()
    val DistinctKks = villagers.distinctBy { it.noKk }
    var selectedKkNo by remember { mutableStateOf("") }
    
    // Choose active KK
    if (selectedKkNo.isBlank() && DistinctKks.isNotEmpty()) {
        selectedKkNo = DistinctKks.first().noKk
    }

    val currentKkMembers = villagers.filter { it.noKk == selectedKkNo }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Draft Kartu Keluarga Digital",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Desain draft struktural KK sebelum pengajuan formal Dukcapil Rembang, lengkap dengan QR Code verifikasi legalitas.",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Divider()
        }

        // KK Selection area
        item {
            Text("Pilih Nomor Kartu Keluarga (KK):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DistinctKks.forEach { kk ->
                    FilterChip(
                        selected = selectedKkNo == kk.noKk,
                        onClick = { selectedKkNo = kk.noKk },
                        label = { Text("No. KK: ${kk.noKk}") },
                        modifier = Modifier.testTag("kk_chip_${kk.noKk.takeLast(4)}")
                    )
                }
            }
        }

        // Draw Official Look KK Structure
        if (currentKkMembers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(2.dp, Color(0xFFB0BEC5)), // Silver metallic border mimicking governmental draft paper
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        Text(
                            text = "KARTU KELUARGA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "No. $selectedKkNo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Table Grid of members!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFECEFF1))
                                .border(1.dp, Color.Black)
                                .padding(6.dp)
                        ) {
                            Text("Nama Lengkap", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 10.sp)
                            Text("NIK", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 10.sp)
                            Text("Hubungan", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 10.sp)
                        }

                        currentKkMembers.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, Color.LightGray)
                                    .padding(6.dp)
                            ) {
                                Text(member.namaLengkap, modifier = Modifier.weight(2f), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(member.nik, modifier = Modifier.weight(1.8f), color = Color.Black, fontSize = 10.sp)
                                Text(member.hubunganKeluarga, modifier = Modifier.weight(1.5f), color = Color.Black, fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Signature block
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // QR Verifikasi Block (Custom stylized Canvas)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("VERIFIKASI SISTEM", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                // Custom Mock QR Canvas as mandated by Section 10/11
                                Canvas(modifier = Modifier.size(54.dp).border(1.dp, Color.Black)) {
                                    // Draw stylized security QR/Barcode markings
                                    drawRect(Color.Black, size = size / 4f)
                                    drawRect(Color.Black, topLeft = Offset(size.width * 0.75f, 0f), size = size / 4f)
                                    drawRect(Color.Black, topLeft = Offset(0f, size.height * 0.75f), size = size / 4f)
                                    // Draw random grid squares inside
                                    val block = size.width / 8f
                                    drawRect(Color.DarkGray, topLeft = Offset(block * 3, block * 3), size = size / 4f)
                                    drawRect(Color.Black, topLeft = Offset(block * 5, block * 4), size = size / 8f)
                                    drawRect(Color.Black, topLeft = Offset(block * 2, block * 5), size = size / 8f)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Scan Legalitas KK", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            // signature
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Soditan, 13/06/2026", fontSize = 9.sp, color = Color.Black)
                                Text("KEPALA DESA SODITAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("TTE BSrE DISDUKCAPIL", fontSize = 8.sp, color = VerifyGreen, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("H. MOH. RIDWAN, S.H.", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. RIWAYAT ARSIP LETTERS SCREEN COMPOSABLE
@Composable
fun HistoryLettersScreen(viewModel: VillageViewModel) {
    val history by viewModel.historyState.collectAsState()
    var selectedSuratForVerification by remember { mutableStateOf<SuratHistory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Arsip Surat & Riwayat Kependudukan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Daftar seluruh surat kependudukan yang telah dicetak, lengkap dengan fitur verifikasi barcode digital instan.",
            fontSize = 11.sp,
            color = Color.Gray
        )
        Divider()

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Send, contentDescription = "Empty", tint = Color.LightGray, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Belum ada arsip surat keluar saat ini", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_card_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.jenisSurat,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { viewModel.deleteLetter(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No Surat: ${item.nomorSurat}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Pemohon (NIK): ${item.namaPemohon} (${item.nik})", fontSize = 13.sp)
                            Text("Tanggal Terbit & TTD: ${item.tanggalTerbit} oleh ${item.pejabatTtd}", fontSize = 12.sp, color = Color.Gray)
                            
                            if (item.isiSuratJson.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Catatan: ${item.isiSuratJson}", fontSize = 11.sp, color = VerifyGreen, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Button(
                                onClick = { selectedSuratForVerification = item },
                                modifier = Modifier.fillMaxWidth().testTag("scan_verify_${item.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "View Verification Screen")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulasikan Verifikasi Elektronik (QR)")
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal view resembling Scan KK/AKM Verification of Disdukcapil Rembang
    if (selectedSuratForVerification != null) {
        Dialog(onDismissRequest = { selectedSuratForVerification = null }) {
            VerificationReceiptDialog(
                surat = selectedSuratForVerification!!,
                onDismiss = { selectedSuratForVerification = null }
            )
        }
    }
}

@Composable
fun VerificationReceiptDialog(
    surat: SuratHistory,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag("verification_panel_dialog"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDarkBg) // Theme matching Scan KK Disdukcapil dark verification screens
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Green Badge
            Card(
                colors = CardDefaults.cardColors(containerColor = VerifyGreen.copy(alpha = 0.2f)),
                border = BorderStroke(1.5.dp, VerifyGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = VerifyGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SURAT TERVERIFIKASI AKTIF",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Text(
                "SISTEM VERIFIKASI ELEKTRONIK DISDUKCAPIL",
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Divider(color = Color.DarkGray)

            // Table of fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerificationFieldRow("Nomor Surat", surat.nomorSurat)
                VerificationFieldRow("Jenis Surat Resmi", surat.jenisSurat)
                VerificationFieldRow("Nama Pemegang", surat.namaPemohon)
                VerificationFieldRow("N.I.K Pemohon", surat.nik)
                VerificationFieldRow("Tanggal Terbit", surat.tanggalTerbit)
                VerificationFieldRow("Penandatangan", surat.pejabatTtd)
                VerificationFieldRow("Keluaran Server", "BSrE TTE Kabupaten Rembang")
            }

            Divider(color = Color.DarkGray)

            // Graphical QR code illustration
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(modifier = Modifier.size(80.dp).background(Color.White).padding(4.dp)) {
                    // Draw stylized security QR/Barcode markings
                    drawRect(Color.Black, size = size / 3.5f)
                    drawRect(Color.Black, topLeft = Offset(size.width * 0.7f, 0f), size = size / 3.5f)
                    drawRect(Color.Black, topLeft = Offset(0f, size.height * 0.7f), size = size / 3.5f)
                    // Draw random grid squares inside
                    val block = size.width / 10f
                    drawRect(Color.DarkGray, topLeft = Offset(block * 4, block * 4), size = size / 3f)
                    drawRect(Color.Black, topLeft = Offset(block * 6, block * 5), size = size / 8f)
                    drawRect(Color.Black, topLeft = Offset(block * 3, block * 7), size = size / 8f)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = surat.qrVerificationUrl, fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tutup Lembar Verifikasi", color = Color.White)
            }
        }
    }
}

@Composable
fun VerificationFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
    }
}

// 7. ADMINISTRATIVE CONFIGURATION SETTINGS SCREEN
@Composable
fun SettingsScreen(viewModel: VillageViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val officials by viewModel.officialsState.collectAsState()

    var desa by remember { mutableStateOf("") }
    var kec by remember { mutableStateOf("") }
    var kab by remember { mutableStateOf("") }
    var pos by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }

    // On-demand settings population
    LaunchedEffect(settings) {
        desa = settings.namaDesa
        kec = settings.kecamatan
        kab = settings.kabupaten
        pos = settings.kodePos
        alamat = settings.alamat
    }

    var newOffName by remember { mutableStateOf("") }
    var newOffNip by remember { mutableStateOf("") }
    var newOffJabatan by remember { mutableStateOf("Kepala Desa (Kades)") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Modul Pengaturan Administrasi Desa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Modifikasi kop surat kependudukan resmi serta kolam penandatangan perangkat desa aktif.",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Divider()
        }

        // Section 1: Kop Settings
        item {
            Text("Pengaturan Kop Surat Desa", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = desa,
                onValueChange = { desa = it },
                label = { Text("Nama Desa") },
                modifier = Modifier.fillMaxWidth().testTag("settings_desa_field")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = kec,
                    onValueChange = { kec = it },
                    label = { Text("Kecamatan") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = kab,
                    onValueChange = { kab = it },
                    label = { Text("Kabupaten") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat Kantor Desa") },
                    modifier = Modifier.weight(2.5f)
                )
                OutlinedTextField(
                    value = pos,
                    onValueChange = { pos = it },
                    label = { Text("Kode Pos") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.updateSettings(
                        settings.copy(
                            namaDesa = desa,
                            kecamatan = kec,
                            kabupaten = kab,
                            alamat = alamat,
                            kodePos = pos
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("save_settings_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Kop Surat", color = Color.White)
            }
        }

        // Section 2: Village Officials pool
        item {
            Divider()
            Spacer(modifier = Modifier.height(4.dp))
            Text("Pengaturan Pejabat Penandatangan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tambah Perangkat Desa:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = newOffName,
                        onValueChange = { newOffName = it },
                        label = { Text("Nama Lengkap & Gelar") },
                        modifier = Modifier.fillMaxWidth().testTag("add_official_name_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newOffNip,
                        onValueChange = { newOffNip = it },
                        label = { Text("N.I.P.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = newOffJabatan,
                        onValueChange = { newOffJabatan = it },
                        label = { Text("Jabatan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (newOffName.isNotBlank() && newOffNip.isNotBlank()) {
                                viewModel.addOfficial(
                                    Official(
                                        nama = newOffName,
                                        nip = newOffNip,
                                        jabatan = newOffJabatan,
                                        isActive = officials.isEmpty() // Auto set active if first official
                                    )
                                )
                                newOffName = ""
                                newOffNip = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("save_official_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Daftarkan Perangkat Desa")
                    }
                }
            }
        }

        // Display current officials list
        item {
            Text("Daftar Perangkat Terdaftar (Klik untuk mengaktifkan TTD):", fontSize = 12.sp, color = Color.Gray)
        }

        if (officials.isEmpty()) {
            item {
                Text("Belum ada perangkat desa terdaftar.", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        } else {
            items(officials, key = { it.id }) { official ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.activateOfficial(official.id) }
                        .testTag("official_item_${official.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (official.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (official.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (official.isActive) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Active TTD", tint = VerifyGreen, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Person, contentDescription = "Inactive", tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(official.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${official.jabatan} • NIP: ${official.nip}", fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteOfficial(official) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// Global cloud-to-local syncing toast feedback overlay Composable
@Composable
fun SyncIndicatorOverlay(
    syncState: SyncStatus,
    onDismiss: () -> Unit
) {
    if (syncState != SyncStatus.Idle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (syncState) {
                        is SyncStatus.Error -> VerifyRed
                        is SyncStatus.Success -> VerifyGreen
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (syncState) {
                        is SyncStatus.Idle -> {}
                        is SyncStatus.Syncing -> {
                            CircularProgressIndicator(
                                progress = syncState.progress / 100f,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Mensinkronisasikan SQLite & Firestore Cloud: ${syncState.progress}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        is SyncStatus.Success -> {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Synced", tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(syncState.message, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        is SyncStatus.Error -> {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(syncState.error, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
