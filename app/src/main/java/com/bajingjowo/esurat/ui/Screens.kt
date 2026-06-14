@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bajingjowo.esurat.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bajingjowo.esurat.data.Official
import com.bajingjowo.esurat.data.SuratHistory
import com.bajingjowo.esurat.data.SyncStatus
import com.bajingjowo.esurat.data.Villager
import com.bajingjowo.esurat.ui.theme.*
import kotlinx.coroutines.launch

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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNotifAlert by remember { mutableStateOf(false) }

    // Screen navigation layout adaptive selection (Drawer rail on wide, bottom navigation bar on mobile screen)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 4.dp,
                modifier = Modifier.width(300.dp)
            ) {
                // Gradient Header Visual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(PrimaryBlue, SecondaryGreen)
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "e-Surat Desa Digital",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Kabupaten Rembang • Versi 2026.1",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Drawer Items
                NavigationDrawerItem(
                    label = { Text("Petunjuk Sistem (Panduan)", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showHelpDialog = true
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Sinkronisasi Firebase Cloud", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.triggerSync()
                    },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Identitas Desa & KOP", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == Screen.Pengaturan,
                    onClick = {
                        scope.launch { drawerState.close() }
                        currentScreen = Screen.Pengaturan
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Tentang Aplikasi e-Surat", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Footer in Drawer
                Text(
                    text = "Layanan Terintegrasi SIAK & BSrE",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 24.dp)
                )
            }
        }
    ) {
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
                    topBar = {
                        // Premium Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentScreen.title, 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Kabupaten Rembang • Desa ${settings.namaDesa}", 
                                        style = MaterialTheme.typography.bodySmall, 
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.triggerSync() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { showNotifAlert = true }) {
                                    Box {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary)
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                            }
                        }
                    },
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
                                        alwaysShowLabel = false,
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

    // Modal Helper Dialogs
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Panduan Pelayanan Desa Digital") },
            text = {
                Text("1. Gunakan menu Penduduk untuk melihat data registrasi warga.\n" +
                     "2. Anda dapat Tambah Anggota Keluarga, Edit data, atau Hapus.\n" +
                     "3. Gunakan menu Buat Surat untuk mencetak surat kependudukan secara otomatis (menggunakan Template KOP Dindukcapil Rembang terbaru).\n" +
                     "4. Semua surat dilengkapi verifikasi Tanda Tangan Elektronik terintegrasi barcode BSrE.")
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Tentang Aplikasi") },
            text = {
                Text("e-Surat Desa Digital Rembang v2026.1\n\n" +
                     "Sistem Informasi Kependudukan berkecepatan tinggi terintegrasi AI, SIAK terluar, database Room SQLite, dan Sinkronisasi Cloud Firebase.")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showNotifAlert) {
        AlertDialog(
            onDismissRequest = { showNotifAlert = false },
            title = { Text("Notifikasi Sistem") },
            text = {
                Text("Sistem Integrasi SIAK DINDUKCAPIL Rembang & BSrE aktif sepenuhnya di desa ${settings.namaDesa}. Seluruh data kependudukan sinkron dengan server daerah.")
            },
            confirmButton = {
                TextButton(onClick = { showNotifAlert = false }) {
                    Text("Tutup")
                }
            }
        )
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
        // Welcome Banner Card with Beautiful Linear Gradient (PrimaryBlue - SecondaryGreen)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(PrimaryBlue, SecondaryGreen)
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "e-Surat Desa Digital",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sistem Administrasi Desa ${settings.namaDesa} • Kec. ${settings.kecamatan}, Kab. Rembang",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onNavigate(Screen.AIAgent) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("dashboard_quick_ai")
                            ) {
                                Icon(Icons.Default.Build, contentDescription = "AI", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Panduan Asisten AI", fontWeight = FontWeight.Bold)
                            }
                            
                            IconButton(
                                onClick = { viewModel.triggerSync() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .size(48.dp)
                                    .testTag("sync_cloud_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Cloud",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SecondaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = "Official",
                            tint = SecondaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Pejabat Penandatangan Aktif:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = activeOfficial?.nama ?: "Belum Memilih Penandatangan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${activeOfficial?.jabatan ?: "Staff"} • NIP. ${activeOfficial?.nip ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(14.dp)
                )
            }
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
    var selectedForDetail by remember { mutableStateOf<Villager?>(null) }
    var selectedForEdit by remember { mutableStateOf<Villager?>(null) }
    var selectedForDelete by remember { mutableStateOf<Villager?>(null) }
    var prefilledKkForAddMember by remember { mutableStateOf<String?>(null) }

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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                maxLines = 1,
                placeholder = { Text("Cari NIK, Nama, No KK...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryBlue) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_villager_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
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
            FilterChip(
                selected = selectedDusun == "Dusun Mulyo",
                onClick = { viewModel.setDusunFilter(if (selectedDusun == "Dusun Mulyo") null else "Dusun Mulyo") },
                label = { Text("Dusun Mulyo") }
            )
            FilterChip(
                selected = selectedDusun == "Dusun Krajan",
                onClick = { viewModel.setDusunFilter(if (selectedDusun == "Dusun Krajan") null else "Dusun Krajan") },
                label = { Text("Dusun Krajan") }
            )
            FilterChip(
                selected = selectedGender == "Laki-laki",
                onClick = { viewModel.setGenderFilter(if (selectedGender == "Laki-laki") null else "Laki-laki") },
                label = { Text("Laki-laki") }
            )
            FilterChip(
                selected = selectedGender == "Perempuan",
                onClick = { viewModel.setGenderFilter(if (selectedGender == "Perempuan") null else "Perempuan") },
                label = { Text("Perempuan") }
            )
        }

        // Residents Registry Table Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Registry Kependudukan (${villagers.size} Terdaftar)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

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
            // Elegant Zebra Data Table
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryBlue.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NIK & Nama Lengkap",
                            modifier = Modifier.weight(1.3f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Dusun",
                            modifier = Modifier.weight(0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Aksi Operasi",
                            modifier = Modifier.weight(1.2f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryBlue,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Table Rows
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(villagers) { index, villager ->
                            val isZebra = index % 2 == 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isZebra) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("villager_card_${villager.nik}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // NIK & Nama Column
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = villager.namaLengkap,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "NIK: ${villager.nik}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                // Dusun Column
                                Text(
                                    text = villager.dusun,
                                    modifier = Modifier.weight(0.8f),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                // Simple Action Buttons (Detail, Edit, Hapus, Tambah Anggota)
                                Row(
                                    modifier = Modifier.weight(1.2f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Detail Button
                                    IconButton(
                                        onClick = { selectedForDetail = villager },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Detail",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Edit Button
                                    IconButton(
                                        onClick = { selectedForEdit = villager },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = SecondaryGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Tambah Anggota Keluarga Button
                                    IconButton(
                                        onClick = { prefilledKkForAddMember = villager.noKk },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            contentDescription = "Tambah Anggota",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Hapus Button
                                    IconButton(
                                        onClick = { selectedForDelete = villager },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        }
                    }
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

    // Edit Resident dialog
    if (selectedForEdit != null) {
        AddVillagerDialog(
            initialVillager = selectedForEdit,
            onDismiss = { selectedForEdit = null },
            onConfirm = {
                viewModel.addVillager(it)
                selectedForEdit = null
            }
        )
    }

    // Tambah Anggota Keluarga dialog
    if (prefilledKkForAddMember != null) {
        AddVillagerDialog(
            prefilledKk = prefilledKkForAddMember,
            onDismiss = { prefilledKkForAddMember = null },
            onConfirm = {
                viewModel.addVillager(it)
                prefilledKkForAddMember = null
            }
        )
    }

    // Detail dialog
    if (selectedForDetail != null) {
        VillagerDetailDialog(
            villager = selectedForDetail!!,
            onDismiss = { selectedForDetail = null }
        )
    }

    // Delete dialog
    if (selectedForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedForDelete = null },
            title = { Text("Konfirmasi Penghapusan") },
            text = { Text("Apakah Anda yakin ingin menghapus '${selectedForDelete?.namaLengkap}' dari data kependudukan desa?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedForDelete?.let { viewModel.removeVillager(it) }
                        selectedForDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedForDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun VillagerDetailDialog(
    villager: Villager,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = villager.namaLengkap,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "NIK: ${villager.nik}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { DetailItemRow("Nomor Kartu Keluarga (KK)", villager.noKk) }
                    item { DetailItemRow("Tempat, Tanggal Lahir", "${villager.tempatLahir}, ${villager.tanggalLahir}") }
                    item { DetailItemRow("Jenis Kelamin", villager.jenisKelamin) }
                    item { DetailItemRow("Hubungan Keluarga", villager.hubunganKeluarga) }
                    item { DetailItemRow("Status Menikah", villager.statusPerkawinan) }
                    item { DetailItemRow("Pendidikan", villager.pendidikan) }
                    item { DetailItemRow("Pekerjaan", villager.pekerjaan) }
                    item { DetailItemRow("Agama", villager.agama) }
                    item { DetailItemRow("Dusun / RT / RW", "${villager.dusun} RT ${villager.rt} RW ${villager.rw}") }
                    item { DetailItemRow("Ayah Kandung", villager.namaAyah) }
                    item { DetailItemRow("Ibu Kandung", villager.namaIbu) }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Tutup detail", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Text(text = value.ifBlank { "-" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
fun AddVillagerDialog(
    initialVillager: Villager? = null,
    prefilledKk: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Villager) -> Unit
) {
    var nik by remember { mutableStateOf(initialVillager?.nik ?: "") }
    var noKk by remember { mutableStateOf(initialVillager?.noKk ?: prefilledKk ?: "") }
    var namaLengkap by remember { mutableStateOf(initialVillager?.namaLengkap ?: "") }
    var selectedGender by remember { mutableStateOf(initialVillager?.jenisKelamin ?: "Laki-laki") }
    var tempatLahir by remember { mutableStateOf(initialVillager?.tempatLahir ?: "Rembang") }
    var tanggalLahir by remember { mutableStateOf(initialVillager?.tanggalLahir ?: "1995-10-10") }
    var namaAyah by remember { mutableStateOf(initialVillager?.namaAyah ?: "") }
    var namaIbu by remember { mutableStateOf(initialVillager?.namaIbu ?: "") }
    var pendidikan by remember { mutableStateOf(initialVillager?.pendidikan ?: "SMA") }
    var pekerjaan by remember { mutableStateOf(initialVillager?.pekerjaan ?: "Wiraswasta") }
    var statusKawin by remember { mutableStateOf(initialVillager?.statusPerkawinan ?: "Belum Kawin") }
    var hubKeluarga by remember { mutableStateOf(initialVillager?.hubunganKeluarga ?: (if (prefilledKk != null) "Anak" else "Kepala Keluarga")) }
    var agama by remember { mutableStateOf(initialVillager?.agama ?: "Islam") }
    var rt by remember { mutableStateOf(initialVillager?.rt ?: "01") }
    var rw by remember { mutableStateOf(initialVillager?.rw ?: "01") }
    var dusun by remember { mutableStateOf(initialVillager?.dusun ?: "Dusun Mulyo") }

    val isEditMode = initialVillager != null
    val titleText = when {
        isEditMode -> "Ubah Data Penduduk"
        prefilledKk != null -> "Tambah Anggota Keluarga"
        else -> "Registrasi Penduduk Baru"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = nik,
                            onValueChange = { if (it.length <= 16 && !isEditMode) nik = it },
                            label = { Text("N.I.K. (16 Digit)") },
                            enabled = !isEditMode, // NIK cannot be changed during Edit
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("add_nik_field"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = noKk,
                            onValueChange = { if (it.length <= 16) noKk = it },
                            label = { Text("Nomor Kartu Keluarga (KK)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("add_kk_field"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = namaLengkap,
                            onValueChange = { namaLengkap = it },
                            label = { Text("Nama Lengkap") },
                            modifier = Modifier.fillMaxWidth().testTag("add_name_field"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        Text("Jenis Kelamin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedGender == "Laki-laki", onClick = { selectedGender = "Laki-laki" })
                                Text("Laki-laki", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedGender == "Perempuan", onClick = { selectedGender = "Perempuan" })
                                Text("Perempuan", fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tempatLahir,
                                onValueChange = { tempatLahir = it },
                                label = { Text("Tempat Lahir") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = tanggalLahir,
                                onValueChange = { tanggalLahir = it },
                                label = { Text("Tgl Lahir (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = namaAyah,
                                onValueChange = { namaAyah = it },
                                label = { Text("Nama Ayah") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = namaIbu,
                                onValueChange = { namaIbu = it },
                                label = { Text("Nama Ibu") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rt,
                                onValueChange = { rt = it },
                                label = { Text("RT") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = rw,
                                onValueChange = { rw = it },
                                label = { Text("RW") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = dusun,
                                onValueChange = { dusun = it },
                                label = { Text("Dusun") },
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = hubKeluarga,
                                onValueChange = { hubKeluarga = it },
                                label = { Text("Hubungan KK (e.g. Anak)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = statusKawin,
                                onValueChange = { statusKawin = it },
                                label = { Text("Status Nikah") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pendidikan,
                                onValueChange = { pendidikan = it },
                                label = { Text("Pendidikan") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = pekerjaan,
                                onValueChange = { pekerjaan = it },
                                label = { Text("Pekerjaan") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = agama,
                            onValueChange = { agama = it },
                            label = { Text("Agama") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
                                        alamat = "RT $rt RW $rw, $dusun"
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.testTag("submit_villager"),
                        shape = RoundedCornerShape(12.dp)
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    
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
            
            item {
                Button(
                    onClick = {
                        printKkAsPdf(context, selectedKkNo, currentKkMembers, settings)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("print_draft_kk_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Cetak Draft KK"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Draft KK Resmi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// 6. RIWAYAT ARSIP LETTERS SCREEN COMPOSABLE
@Composable
fun HistoryLettersScreen(viewModel: VillageViewModel) {
    val history by viewModel.historyState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val villagers by viewModel.villagersState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
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
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { selectedSuratForVerification = item },
                                    modifier = Modifier.weight(1.1f).testTag("scan_verify_${item.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "View Verification Screen", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verifikasi QR", fontSize = 11.sp, maxLines = 1)
                                }
                                
                                Button(
                                    onClick = {
                                        val resident = villagers.find { it.nik == item.nik }
                                        printSuratAsPdf(context, item, resident, settings)
                                    },
                                    modifier = Modifier.weight(0.9f).testTag("print_pdf_${item.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Print PDF", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cetak PDF", fontSize = 11.sp, maxLines = 1)
                                }
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
    var email by remember { mutableStateOf("") }
    var logoBase64 by remember { mutableStateOf("") }

    // On-demand settings population
    LaunchedEffect(settings) {
        desa = settings.namaDesa
        kec = settings.kecamatan
        kab = settings.kabupaten
        pos = settings.kodePos
        alamat = settings.alamat
        email = settings.email
        logoBase64 = settings.logoUrl
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
            Text("Pengaturan Kop Surat Desa & Logo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                if (uri != null) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val bytes = inputStream.readBytes()
                            inputStream.close()
                            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            logoBase64 = base64
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                val logoBitmap = remember(logoBase64) {
                    if (logoBase64.isNotEmpty()) {
                        try {
                            val decodedBytes = android.util.Base64.decode(logoBase64, android.util.Base64.NO_WRAP)
                            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

                if (logoBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = "Logo Desa",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(4.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "No Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Logo Resmi Desa (.jpg / .png)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (logoBase64.isNotEmpty()) "Logo kustom aktif" else "Menggunakan logo default",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("upload_logo_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Pilih Logo", fontSize = 11.sp)
                        }
                        if (logoBase64.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { logoBase64 = "" },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("reset_logo_btn")
                            ) {
                                Text("Hapus", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

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
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Kantor Desa") },
                modifier = Modifier.fillMaxWidth().testTag("settings_email_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
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
                            kodePos = pos,
                            email = email,
                            logoUrl = logoBase64
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

// PDF Print helper function utilizing native Android WebView HTML Print adapter
fun printSuratAsPdf(
    context: android.content.Context,
    item: SuratHistory,
    villager: Villager?,
    settings: com.bajingjowo.esurat.data.VillageSettings
) {
    val logoHtml = if (settings.logoUrl.isNotEmpty()) {
        """<img src="data:image/png;base64,${settings.logoUrl}" style="max-height: 85px; max-width: 85px; object-fit: contain;" />"""
    } else {
        """<img src="https://upload.wikimedia.org/wikipedia/commons/e/ea/Logo_Kabupaten_Rembang.png" style="max-height: 85px; max-width: 85px; object-fit: contain;" alt="Logo Kabupaten Rembang" />"""
    }

    val nama = villager?.namaLengkap ?: item.namaPemohon
    val nkkText = if (villager?.noKk != null && villager.noKk.isNotBlank()) " / ${villager.noKk}" else ""
    val nik = villager?.nik ?: item.nik
    val noKk = villager?.noKk ?: "-"
    val gender = villager?.jenisKelamin ?: "-"
    val ttl = if (villager != null) "${villager.tempatLahir}, ${villager.tanggalLahir}" else "-"
    val agama = villager?.agama ?: "-"
    val status = villager?.statusPerkawinan ?: "-"
    val pekerjaan = villager?.pekerjaan ?: "-"
    val alamatPemohon = villager?.alamat ?: "Desa ${settings.namaDesa}"
    
    val tambahanKeterangan = if (item.isiSuratJson.isNotEmpty()) {
        "Menerangkan dengan sebenarnya bahwa yang bersangkutan berkelakuan baik, aktif dalam kegiatan kemasyarakatan, serta memenuhi kriteria administratif untuk keperluan: <strong>${item.isiSuratJson}</strong>."
    } else {
        "Menerangkan dengan sebenarnya bahwa yang bersangkutan adalah warga Desa resmi yang berkelakuan baik serta teregistrasi dalam data tertib sipil desa untuk dapat dipergunakan sebagaimana mestinya."
    }

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body { 
                    font-family: 'Times New Roman', Times, serif; 
                    margin: 40px; 
                    color: #000; 
                    line-height: 1.5; 
                }
                .header-wrapper {
                    position: relative;
                }
                .siak-badge {
                    position: absolute;
                    top: -15px;
                    right: 0;
                    border: 1px solid #1565c0;
                    color: #1565c0;
                    font-size: 8px;
                    font-weight: bold;
                    padding: 2px 5px;
                    border-radius: 3px;
                    font-family: sans-serif;
                    letter-spacing: 0.5px;
                    text-transform: uppercase;
                }
                .kop-table { width: 100%; border-collapse: collapse; margin-bottom: 2px; }
                .kop-logo { width: 90px; text-align: left; vertical-align: middle; }
                .kop-text { text-align: center; vertical-align: middle; padding-right: 45px; }
                .kop-text h3 { margin: 0; font-size: 14px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.2px; }
                .kop-text h2 { margin: 2px 0; font-size: 16px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.3px; }
                .kop-text h1 { margin: 2px 0; font-size: 19px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; }
                .kop-text p { margin: 2px 0; font-size: 11px; font-style: italic; color: #333; }
                .line-double { border-top: 3.5px double #000; margin-top: 8px; margin-bottom: 22px; height: 0; }
                .title { text-align: center; margin-bottom: 25px; }
                .title h3 { margin: 0; font-size: 15px; font-weight: bold; text-decoration: underline; text-transform: uppercase; letter-spacing: 0.5px; }
                .title p { margin: 3px 0; font-size: 12px; font-family: 'Courier New', Courier, monospace; font-weight: bold; }
                .content { font-size: 13.5px; text-align: justify; }
                .content p { margin-bottom: 12px; text-indent: 40px; }
                .details-table { width: 88%; margin: 15px auto; border-collapse: collapse; font-size: 13.5px; }
                .details-table td { padding: 4px 6px; vertical-align: top; }
                .details-table td.label { width: 33%; }
                .signature-section { width: 100%; margin-top: 40px; font-size: 13.5px; page-break-inside: avoid; }
                .sig-table { width: 100%; border-collapse: collapse; }
                .sig-cell { width: 50%; vertical-align: top; }
                .sig-right { text-align: center; }
                .sig-name { font-weight: bold; text-decoration: underline; margin-top: 4px; text-transform: uppercase; font-size: 14px; }
                .sig-title { margin-bottom: 10px; line-height: 1.3; font-weight: bold; }
                .tte-badge-container {
                    display: inline-block;
                    margin: 8px auto;
                    border: 1.5px solid #1b5e20;
                    border-radius: 4px;
                    padding: 3px 10px;
                    background-color: #f1f8e9;
                    box-sizing: border-box;
                    max-width: 180px;
                }
                .tte-badge { 
                    color: #1b5e20; 
                    font-size: 8.5px; 
                    font-weight: bold; 
                    text-transform: uppercase; 
                    letter-spacing: 0.8px;
                    font-family: sans-serif;
                }
                .tte-sub {
                    font-size: 7px;
                    color: #555;
                    font-family: sans-serif;
                    margin-top: 1px;
                }
                .sig-desc { 
                    font-size: 9px; 
                    color: #444; 
                    text-align: left; 
                    max-width: 270px; 
                    border: 1px dashed #777; 
                    padding: 10px; 
                    border-radius: 5px; 
                    background: #fbfbfb; 
                    line-height: 1.4;
                    font-family: sans-serif;
                }
            </style>
        </head>
        <body>
            <div class="header-wrapper">
                <span class="siak-badge">SIAK DINDUKCAPIL REMBANG</span>
                <table class="kop-table">
                    <tr>
                        <td class="kop-logo">
                            $logoHtml
                        </td>
                        <td class="kop-text">
                            <h3>PEMERINTAH KABUPATEN REMBANG</h3>
                            <h2>KECAMATAN ${settings.kecamatan.uppercase()}</h2>
                            <h1>DESA ${settings.namaDesa.uppercase()}</h1>
                            <p>Alamat: ${settings.alamat} • Kode Pos: ${settings.kodePos}</p>
                            <p style="margin: 2px 0; font-size: 11px; font-style: italic; color: #333;">Email: ${settings.email}</p>
                        </td>
                    </tr>
                </table>
            </div>
            <div class="line-double"></div>
            
            <div class="title">
                <h3>SURAT KETERANGAN KEPENDUDUKAN</h3>
                <p>Nomor: ${item.nomorSurat}</p>
            </div>
            
            <div class="content">
                <p>Berdasarkan Peraturan Menteri Dalam Negeri Republik Indonesia Nomor 109 Tahun 2019 tentang Formulir dan Buku yang Digunakan dalam Administrasi Kependudukan, serta rujukan database terpusat Dinas Kependudukan dan Pencatatan Sipil (Dindukcapil) Kabupaten Rembang, yang bertanda tangan di bawah ini Kepala Desa ${settings.namaDesa}, Kecamatan ${settings.kecamatan}, menerangkan dengan sebenarnya bahwa warga kami:</p>
                
                <table class="details-table">
                    <tr>
                        <td class="label">Nama Lengkap</td>
                        <td>: <strong>$nama</strong></td>
                    </tr>
                    <tr>
                        <td class="label">Nomor Induk Kependudukan (NIK)</td>
                        <td>: <strong>$nik</strong></td>
                    </tr>
                    <tr>
                        <td class="label">Nomor Kartu Keluarga (KK)</td>
                        <td>: $noKk</td>
                    </tr>
                    <tr>
                        <td class="label">Tempat, Tanggal Lahir</td>
                        <td>: $ttl</td>
                    </tr>
                    <tr>
                        <td class="label">Jenis Kelamin</td>
                        <td>: $gender</td>
                    </tr>
                    <tr>
                        <td class="label">Agama / Status Pernikahan</td>
                        <td>: $agama / $status</td>
                    </tr>
                    <tr>
                        <td class="label">Pekerjaan Terdaftar</td>
                        <td>: $pekerjaan</td>
                    </tr>
                    <tr>
                        <td class="label">Alamat Lengkap (Sesuai KTP)</td>
                        <td>: $alamatPemohon</td>
                    </tr>
                </table>
                
                <p>$tambahanKeterangan</p>
                
                <p>Aliran validitas status kependudukan ini sinkron secara langsung dengan Sistem Informasi Administrasi Kependudukan (SIAK) terluar. Demikian surat keterangan ini kami buat dengan penuh kesadaran dan tanggung jawab untuk dapat dipergunakan sebagaimana mestinya.</p>
            </div>
            
            <div class="signature-section">
                <table class="sig-table">
                    <tr>
                        <td class="sig-cell" style="width: 50%;">
                            <!-- Bagian Kiri Kosong -->
                        </td>
                        <td class="sig-cell sig-right" style="width: 50%;">
                            <div class="sig-title">
                                ${settings.namaDesa}, ${item.tanggalTerbit}<br>
                                Kepala Desa ${settings.namaDesa}
                            </div>
                            
                            <!-- QR Code berada tepat di atas nama pejabat penandatangan, tanpa caption teks apapun, murni QR Code -->
                            <div style="margin: 10px auto; text-align: center;">
                                <img src="https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=${java.net.URLEncoder.encode(item.qrVerificationUrl, "UTF-8")}" style="width: 90px; height: 90px; background: white; border: 1px solid #000; padding: 2px; display: inline-block;" alt="TTE" />
                            </div>
                            
                            <div class="sig-name">${item.pejabatTtd}</div>
                        </td>
                    </tr>
                </table>
            </div>
        </body>
        </html>
    """.trimIndent()

    val webView = android.webkit.WebView(context)
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "eSuratDesa_${item.nomorSurat.replace('/', '_')}"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            val printAttributes = android.print.PrintAttributes.Builder()
                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}

// PDF Print helper function specifically styled as authentic Kartu Keluarga with diagonal DRAFT watermark
fun printKkAsPdf(
    context: android.content.Context,
    kkNo: String,
    members: List<com.bajingjowo.esurat.data.Villager>,
    settings: com.bajingjowo.esurat.data.VillageSettings
) {
    val headOfFamily = members.find { it.hubunganKeluarga.equals("Kepala Keluarga", ignoreCase = true) } ?: members.firstOrNull()
    val firstMember = members.firstOrNull()
    
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset='utf-8'>
            <style>
                @page {
                    size: A4 landscape;
                    margin: 8mm 12mm 8mm 12mm;
                }
                body {
                    font-family: 'Arial', 'Helvetica', sans-serif;
                    margin: 0;
                    padding: 0;
                    color: #000;
                    background-color: #f7f9fa;
                    line-height: 1.2;
                    /* Watermarks: Repeat DUKCAPIL text pattern, and single clean Garuda Centered seal */
                    background-image: 
                        url("data:image/svg+xml;utf8,<svg width='200' height='120' viewBox='0 0 200 120' xmlns='http://www.w3.org/2000/svg'><text x='50%' y='50%' font-size='10' fill='rgba(33, 150, 243, 0.05)' font-weight='800' font-family='sans-serif' text-anchor='middle' transform='rotate(-28 100 60)'>DUKCAPIL</text></svg>"), 
                        url("data:image/svg+xml;utf8,<svg viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'><path d='M 50,15 C 49,15 48,12 50,8 C 52,12 51,15 50,15 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 50,14 C 47,15 45,19 46,24 C 48,22 49,21 50,21 C 51,21 52,22 54,24 C 55,19 53,15 50,14 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 46,24 C 45,26 44,28 44,30 C 47,28 49,27 50,27 C 51,27 53,28 56,30 C 56,28 55,26 54,24 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 46,24 C 44,23 43,24 42,26 C 44,27 45,26 46,24 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 50,21 C 41,25 40,38 41,50 C 44,52 47,53 50,53 C 53,53 56,52 59,50 C 60,38 59,25 50,21 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 41,35 C 33,26 15,28 5,45 C 10,48 18,48 24,45 C 18,52 11,56 8,62 C 14,62 25,58 31,52 C 24,62 18,70 16,80 C 24,76 34,68 38,58 C 39,52 40,43 41,35 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 59,35 C 67,26 85,28 95,45 C 90,48 82,48 76,45 C 82,52 89,56 92,62 C 86,62 75,58 69,52 C 76,62 82,70 84,80 C 76,76 66,68 62,58 C 61,52 60,43 59,35 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 43,62 C 40,75 35,85 30,92 C 34,92 41,85 43,80 C 43,85 45,90 44,95 C 47,91 49,85 49,80 C 49,85 51,91 54,95 C 53,90 55,85 55,80 C 57,85 64,92 68,92 C 63,85 58,75 55,62 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 46,55 C 43,58 40,62 39,66 C 42,65 44,63 45,61 C 44,65 43,70 41,74 C 45,71 47,67 48,63 C 48,68 49,72 50,75 C 51,72 52,68 52,63 C 53,67 55,71 59,74 C 57,70 56,65 55,61 C 56,63 58,65 61,66 Q 60,62 57,58 Q 54,55 54,55 Z' fill='rgba(13, 71, 161, 0.03)'/><path d='M 44,38 Q 44,48 50,51 Q 56,48 56,38 Z' fill='none' stroke='rgba(13, 71, 161, 0.04)' stroke-width='1.5'/><line x1='50' y1='38' x2='50' y2='51' stroke='rgba(13, 71, 161, 0.04)' stroke-width='1'/><line x1='44' y1='44.5' x2='56' y2='44.5' stroke='rgba(13, 71, 161, 0.04)' stroke-width='1'/><polygon points='50,42 51,44 53,44 51.5,45 52,47 50,45.8 48,47 48.5,45 47,44 49,44' fill='rgba(13, 71, 161, 0.04)'/><path d='M 38,58 Q 50,62 62,58' fill='none' stroke='rgba(13, 71, 161, 0.04)' stroke-width='3'/><path d='M 32,58 H 68 V 61 H 32 Z' fill='none' stroke='rgba(13, 71, 161, 0.04)' stroke-width='1'/><text x='50' y='60.2' font-size='2.5' font-weight='900' text-anchor='middle' fill='rgba(13, 71, 161, 0.04)' font-family='sans-serif'>BHINNEKA TUNGGAL IKA</text></svg>");
                    background-repeat: repeat, no-repeat;
                    background-position: center, center;
                    background-size: auto, 340px 340px;
                }
                
                /* Layout Sections */
                .top-bar {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    border-bottom: 2px solid #000;
                    padding-bottom: 5px;
                    margin-bottom: 8px;
                }
                .logo-left {
                    width: 60px;
                    height: 60px;
                }
                .title-center {
                    text-align: center;
                    flex-grow: 1;
                }
                .title-center h1 {
                    margin: 0;
                    font-size: 21px;
                    font-weight: 900;
                    letter-spacing: 2px;
                    color: #000;
                }
                .title-center h2 {
                    margin: 2px 0 0 0;
                    font-size: 15px;
                    font-weight: bold;
                    letter-spacing: 1px;
                }
                .code-right {
                    width: 140px;
                    font-size: 8px;
                    font-weight: bold;
                    text-align: right;
                    font-family: monospace;
                    text-transform: uppercase;
                }
                .code-box {
                    border: 1px solid #000;
                    padding: 3px 6px;
                    display: inline-block;
                    background: #fff;
                    letter-spacing: 0.5px;
                }
                
                /* Identity Section */
                .identity-table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 9.5px;
                    margin-bottom: 8px;
                    font-weight: bold;
                }
                .identity-table td {
                    padding: 1.5px 0;
                    vertical-align: top;
                }
                .col-left {
                    width: 18%;
                }
                .col-middle-left {
                    width: 32%;
                }
                .col-right {
                    width: 18%;
                }
                .col-middle-right {
                    width: 32%;
                }
                
                /* Tables Formatting */
                .data-table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 8.5px;
                    margin-bottom: 8px;
                    background-color: rgba(255, 255, 255, 0.85);
                }
                .data-table th {
                    border: 1.2px solid #000;
                    background-color: #ededed;
                    color: #000;
                    font-weight: bold;
                    text-align: center;
                    padding: 3px;
                    text-transform: uppercase;
                    vertical-align: middle;
                    font-size: 8px;
                }
                .data-table td {
                    border: 1px solid #000;
                    padding: 3px 4px;
                    vertical-align: middle;
                }
                .row-num-indicator th {
                    background-color: #f7f7f7 !important;
                    font-size: 7px !important;
                    font-weight: normal !important;
                    padding: 1px !important;
                    color: #444 !important;
                }
                .text-center {
                    text-align: center;
                }
                .text-strong {
                    font-weight: bold;
                    text-transform: uppercase;
                }
                .monospace {
                    font-family: 'Courier New', Courier, monospace;
                    font-weight: bold;
                    letter-spacing: 0.5px;
                }
                
                /* Bottom Footer Panel */
                .footer-box {
                    width: 100%;
                    margin-top: 8px;
                    page-break-inside: avoid;
                }
                .footer-columns {
                    width: 100%;
                    border-collapse: collapse;
                }
                .footer-columns td {
                    vertical-align: top;
                }
                .notices-panel {
                    width: 40%;
                    font-size: 7.5px;
                }
                .notices-content {
                    border: 1px solid #777;
                    padding: 5px;
                    background: rgba(255, 255, 255, 0.85);
                    line-height: 1.3;
                    border-radius: 3px;
                }
                .sig-box-holder {
                    width: 30%;
                    text-align: center;
                    font-size: 9px;
                }
                .sig-title {
                    margin-bottom: 40px;
                    text-transform: uppercase;
                    line-height: 1.3;
                }
                .sig-name {
                    font-weight: bold;
                    text-decoration: underline;
                    text-transform: uppercase;
                    font-size: 9.5px;
                }
                .sig-subtext {
                    font-size: 8px;
                    color: #333;
                    margin-top: 2px;
                }
                .tte-panel {
                    width: 30%;
                    text-align: center;
                    font-size: 9px;
                }
                .tte-border {
                    border: 1px solid #000;
                    background: rgba(255, 255, 255, 0.95);
                    padding: 5px;
                    display: inline-block;
                    border-radius: 4px;
                    text-align: center;
                }
                
                /* Legal Footnote */
                .legal-footnote {
                    margin-top: 6px;
                    border: 1px solid #000;
                    padding: 4px 10px;
                    font-size: 7.5px;
                    text-align: center;
                    background-color: rgba(245, 245, 245, 0.95);
                    line-height: 1.3;
                    border-radius: 2px;
                    page-break-inside: avoid;
                    font-weight: bold;
                }
            </style>
        </head>
        <body>
            <!-- Header elements with logo, title and code -->
            <div class='top-bar'>
                <div class='logo-left'>
                    <svg viewBox='0 0 100 100' width='100%' height='100%'>
                        <path d='M 50,15 C 49,15 48,12 50,8 C 52,12 51,15 50,15 Z' fill='#000'/>
                        <path d='M 50,14 C 47,15 45,19 46,24 C 48,22 49,21 50,21 C 51,21 52,22 54,24 C 55,19 53,15 50,14 Z' fill='#000'/>
                        <path d='M 46,24 C 45,26 44,28 44,30 C 47,28 49,27 50,27 C 51,27 53,28 56,30 C 56,28 55,26 54,24 Z' fill='#000'/>
                        <path d='M 46,24 C 44,23 43,24 42,26 C 44,27 45,26 46,24 Z' fill='#000'/>
                        <path d='M 50,21 C 41,25 40,38 41,50 C 44,52 47,53 50,53 C 53,53 56,52 59,50 C 60,38 59,25 50,21 Z' fill='#000'/>
                        <path d='M 41,35 C 33,26 15,28 5,45 C 10,48 18,48 24,45 C 18,52 11,56 8,62 C 14,62 25,58 31,52 C 24,62 18,70 16,80 C 24,76 34,68 38,58 C 39,52 40,43 41,35 Z' fill='#000'/>
                        <path d='M 59,35 C 67,26 85,28 95,45 C 90,48 82,48 76,45 C 82,52 89,56 92,62 C 86,62 75,58 69,52 C 76,62 82,70 84,80 C 76,76 66,68 62,58 C 61,52 60,43 59,35 Z' fill='#000'/>
                        <path d='M 43,62 C 40,75 35,85 30,92 C 34,92 41,85 43,80 C 43,85 45,90 44,95 C 47,91 49,85 49,80 C 49,85 51,91 54,95 C 53,90 55,85 55,80 C 57,85 64,92 68,92 C 63,85 58,75 55,62 Z' fill='#000'/>
                        <path d='M 46,55 C 43,58 40,62 39,66 C 42,65 44,63 45,61 C 44,65 43,70 41,74 C 45,71 47,67 48,63 C 48,68 49,72 50,75 C 51,72 52,68 52,63 C 53,67 55,71 59,74 C 57,70 56,65 55,61 C 56,63 58,65 61,66 Q 60,62 57,58 Q 54,55 54,55 Z' fill='#000'/>
                        <path d='M 44,38 Q 44,48 50,51 Q 56,48 56,38 Z' fill='#fff' stroke='#000' stroke-width='1.5'/>
                        <line x1='50' y1='38' x2='50' y2='51' stroke='#000' stroke-width='1'/>
                        <line x1='44' y1='44.5' x2='56' y2='44.5' stroke='#000' stroke-width='1'/>
                        <polygon points='50,42 51,44 53,44 51.5,45 52,47 50,45.8 48,47 48.5,45 47,44 49,44' fill='#000'/>
                        <path d='M 38,58 Q 50,62 62,58' fill='none' stroke='#000' stroke-width='3'/>
                        <path d='M 32,58 H 68 V 61 H 32 Z' fill='#fff' stroke='#000' stroke-width='1'/>
                        <text x='50' y='60.2' font-size='2.5' font-weight='900' text-anchor='middle' fill='#000' font-family='sans-serif'>BHINNEKA TUNGGAL IKA</text>
                    </svg>
                </div>
                
                <div class='title-center'>
                    <h1>KARTU KELUARGA</h1>
                    <h2>No. $kkNo</h2>
                </div>
                
                <div class='code-right'>
                    <div class='code-box'>FORMULIR F-1.01</div>
                </div>
            </div>
            
            <!-- Dual-column Identity Section -->
            <table class='identity-table'>
                <tr>
                    <td class='col-left'>Nama Kepala Keluarga</td>
                    <td class='col-middle-left'>: ${headOfFamily?.namaLengkap?.uppercase() ?: "-"}</td>
                    <td class='col-right'>Kecamatan</td>
                    <td class='col-middle-right'>: ${settings.kecamatan.uppercase()}</td>
                </tr>
                <tr>
                    <td class='col-left'>Alamat</td>
                    <td class='col-middle-left'>: ${settings.alamat.uppercase()}</td>
                    <td class='col-right'>Kabupaten/Kota</td>
                    <td class='col-middle-right'>: ${settings.kabupaten.uppercase()}</td>
                </tr>
                <tr>
                    <td class='col-left'>RT/RW</td>
                    <td class='col-middle-left'>: ${firstMember?.rt ?: "01"}/${firstMember?.rw ?: "01"}</td>
                    <td class='col-right'>Kode Pos</td>
                    <td class='col-middle-right'>: ${settings.kodePos}</td>
                </tr>
                <tr>
                    <td class='col-left'>Kelurahan/Desa</td>
                    <td class='col-middle-left'>: ${settings.namaDesa.uppercase()}</td>
                    <td class='col-right'>Provinsi</td>
                    <td class='col-middle-right'>: ${settings.provinsi.uppercase()}</td>
                </tr>
            </table>

            <!-- Table 1: Data Anggota Keluarga -->
            <table class='data-table'>
                <thead>
                    <tr>
                        <th style='width: 3%;' rowspan='2'>No</th>
                        <th style='width: 25%;'>Nama Lengkap</th>
                        <th style='width: 15%;'>NIK</th>
                        <th style='width: 8%;'>Jenis Kelamin</th>
                        <th style='width: 12%;'>Tempat Lahir</th>
                        <th style='width: 10%;'>Tanggal Lahir</th>
                        <th style='width: 7%;'>Agama</th>
                        <th style='width: 10%;'>Pendidikan Terakhir</th>
                        <th style='width: 11%;'>Jenis Pekerjaan</th>
                        <th style='width: 4%;'>Gol. Darah</th>
                    </tr>
                    <tr class='row-num-indicator'>
                        <th>(1)</th>
                        <th>(2)</th>
                        <th>(3)</th>
                        <th>(4)</th>
                        <th>(5)</th>
                        <th>(6)</th>
                        <th>(7)</th>
                        <th>(8)</th>
                        <th>(9)</th>
                    </tr>
                </thead>
                <tbody>
                    ${members.mapIndexed { index, m -> """
                    <tr>
                        <td class='text-center'>${index + 1}</td>
                        <td class='text-strong'>${m.namaLengkap.uppercase()}</td>
                        <td class='text-center monospace'>${m.nik}</td>
                        <td class='text-center'>${m.jenisKelamin.uppercase()}</td>
                        <td class='text-strong'>${m.tempatLahir.uppercase()}</td>
                        <td class='text-center'>${m.tanggalLahir}</td>
                        <td class='text-center'>${m.agama.uppercase()}</td>
                        <td>${m.pendidikan.uppercase()}</td>
                        <td>${m.pekerjaan.uppercase()}</td>
                        <td class='text-center'>-</td>
                    </tr>
                    """ }.joinToString("")}
                </tbody>
            </table>

            <!-- Table 2: Status & Hubungan Keluarga -->
            <table class='data-table'>
                <thead>
                    <tr>
                        <th style='width: 3%;' rowspan='2'>No</th>
                        <th style='width: 14%;'>Status Perkawinan</th>
                        <th style='width: 12%;'>Tanggal Perkawinan</th>
                        <th style='width: 16%;'>Status Hubungan Dlm Keluarga</th>
                        <th style='width: 10%;'>Kewarganegaraan</th>
                        <th style='width: 12%;'>No. Paspor</th>
                        <th style='width: 11%;'>No. KITAP/KITAS</th>
                        <th style='width: 11%;'>Nama Ayah</th>
                        <th style='width: 11%;'>Nama Ibu</th>
                    </tr>
                    <tr class='row-num-indicator'>
                        <th>(10)</th>
                        <th>(11)</th>
                        <th>(12)</th>
                        <th>(13)</th>
                        <th>(14)</th>
                        <th>(15)</th>
                        <th>(16)</th>
                        <th>(17)</th>
                    </tr>
                </thead>
                <tbody>
                    ${members.mapIndexed { index, m -> """
                    <tr>
                        <td class='text-center'>${index + 1}</td>
                        <td class='text-center'>${m.statusPerkawinan.uppercase()}</td>
                        <td class='text-center'>-</td>
                        <td class='text-center text-strong'>${m.hubunganKeluarga.uppercase()}</td>
                        <td class='text-center'>${m.kewarganegaraan.uppercase()}</td>
                        <td class='text-center'>-</td>
                        <td class='text-center'>-</td>
                        <td class='text-strong'>${m.namaAyah.uppercase()}</td>
                        <td class='text-strong'>${m.namaIbu.uppercase()}</td>
                    </tr>
                    """ }.joinToString("")}
                </tbody>
            </table>

            <!-- Footer block with notices, signature & TTE -->
            <div class='footer-box'>
                <table class='footer-columns'>
                    <tr>
                        <!-- Dukcapil Legal Instruction Notes -->
                        <td class='notices-panel'>
                            <div class='notices-content'>
                                <strong>KETERANGAN RESMI DUKCAPIL:</strong><br>
                                1. Kepala Keluarga wajib melaporkan setiap kejadian kependudukan (kelahiran, kematian, pernikahan, perceraian) atau perubahan data selambat-lambatnya dalam 14 hari.<br>
                                2. Penduduk wajib memelihara keamanan Kartu Keluarga dan menghindari kerusakan fisik maupun manipulasi data kependudukan.<br>
                                3. Lembar ini dicetak mandiri menggunakan format standardisasi kependudukan nasional dan bersertifikasi keabsahan digital.<br>
                                4. Verifikasi dan keabsahan dokumen otentik dapat dilacak secara real-time dengan memindai kode QR penandatangan di sebelah kanan bawah.
                            </div>
                        </td>
                        
                        <!-- Kepala Keluarga Column -->
                        <td class='sig-box-holder' style='vertical-align: bottom; padding-bottom: 5px;'>
                            KEPALA KELUARGA,<br><br><br><br>
                            <div class='sig-name'>${headOfFamily?.namaLengkap?.uppercase() ?: "-"}</div>
                            <div class='sig-subtext'>Tanda tangan manual / Jempol</div>
                        </td>
                        
                        <!-- Electronic Signatures TTE Column -->
                        <td class='tte-panel'>
                            <div style='line-height:1.3;'>
                                ${settings.kabupaten.uppercase()}, ${java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())}<br>
                                <strong>KEPALA DINAS KEPENDUDUKAN DAN</strong><br>
                                <strong>PENCATATAN SIPIL KABUPATEN REMBANG</strong>
                            </div>
                            
                            <!-- Large verification QR Code and Electronic Signature sign -->
                            <div style='margin: 4px auto 2px auto;'>
                                <div class='tte-border'>
                                    <div style='font-size: 7px; font-weight: bold; color: #1565c0; margin-bottom: 2px;'>DITANDATANGANI SECARA ELEKTRONIK</div>
                                    <img src='https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https://rembangkab.go.id/kk-verify/$kkNo&color=0d47a1' style='width: 65px; height: 65px; outline: 1px solid #ddd; padding: 2px; background: %23fff; display: inline-block;' alt='QR Verification' />
                                    <div style='font-size: 6px; color: #666; margin-top: 2px; letter-spacing: 0.3px;'>Balai Sertifikasi Elektronik (BSrE) BSSN</div>
                                </div>
                            </div>
                            
                            <div class='sig-name'>Drs. SUPARMIN, M.M.</div>
                            <div class='sig-subtext'>Pembina Utama Muda</div>
                            <div class='sig-subtext'>NIP. 19680324 199303 1 005</div>
                        </td>
                    </tr>
                </table>
            </div>

            <!-- Centralized horizontal verification legal box -->
            <div class='legal-footnote'>
                Dokumen ini telah ditandatangani secara elektronik menggunakan sertifikat elektronik yang diterbitkan oleh Balai Sertifikasi Elektronik (BSrE), Badan Siber dan Sandi Negara. Berdasarkan Undang-Undang RI Nomor 11 Tahun 2008 tentang Informasi dan Transaksi Elektronik, lembaran cetak ini memiliki kekuatan pembuktian hukum yang sah sama seperti dokumen aslinya.
            </div>
        </body>
        </html>
    """.trimIndent()
 
    val webView = android.webkit.WebView(context)
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "eDraftKK_$kkNo"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            val printAttributes = android.print.PrintAttributes.Builder()
                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}
