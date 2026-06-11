package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AlgeriaData
import com.example.data.Doctor
import com.example.data.Wilaya
import com.example.ui.DoctorViewModel
import com.example.ui.theme.MyApplicationTheme

// Define Theme Colors Locally for absolute consistency and Emerald medical look
val EmeraldPrimary = Color(0xFF00796B)       // Deep trustworthy teal
val EmeraldLight = Color(0xFFE0F2F1)         // Very soft light mint
val EmeraldAccent = Color(0xFF00BFA5)        // Bright, active mint
val AmberRating = Color(0xFFFFB300)          // Warm yellow for stars
val BackgroundGray = Color(0xFFF2F5F8)       // Elegant background color
val SoftSlate = Color(0xFF455A64)            // Secondary labels text
val DarkSlate = Color(0xFF263238)            // Primary headers text

class MainActivity : ComponentActivity() {
    private val viewModel: DoctorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DoctorFinderApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DoctorFinderApp(
    viewModel: DoctorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDevMode by viewModel.isDeveloperMode.collectAsState()

    // Password Dialog State Variables
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    // Dialog state variables
    var showWilayaDialog by remember { mutableStateOf(false) }
    var showMunicipalityDialog by remember { mutableStateOf(false) }
    var showSpecialtyDialog by remember { mutableStateOf(false) }

    // Selected values for display
    val selectedW by viewModel.selectedWilaya.collectAsState()
    val selectedM by viewModel.selectedMunicipality.collectAsState()
    val selectedS by viewModel.selectedSpecialty.collectAsState()

    var activeDetailDoctor by remember { mutableStateOf<Doctor?>(null) }

    val listAllDoctors by viewModel.allDoctors.collectAsState()

    val allMunicipalitiesForSelectedW = remember(selectedW, listAllDoctors) {
        if (selectedW == null) emptyList()
        else {
            val staticMun = AlgeriaData.getMunicipalities(selectedW!!.id)
            val customMun = listAllDoctors
                .filter { it.wilayaNameAr == selectedW!!.nameAr || it.wilayaNameEn == selectedW!!.nameEn }
                .map { it.municipalityNameAr }
                .filter { it.isNotBlank() }
            (staticMun + customMun).distinct().sorted()
        }
    }

    val allSpecialties = remember(listAllDoctors) {
        val staticSpec = AlgeriaData.specialties
        val customSpec = listAllDoctors.map { it.specialty }.filter { it.isNotBlank() }
        (staticSpec + customSpec).distinct().sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // App header
        HeaderBlock(
            isDevMode = isDevMode,
            onToggleMode = {
                if (isDevMode) {
                    // Turn off developer mode directly
                    viewModel.toggleDeveloperMode()
                } else {
                    // Start authentication verification
                    passwordInput = ""
                    passwordError = false
                    showPasswordDialog = true
                }
            }
        )

        // Switch screens based on programmer or patient mode
        AnimatedContent(
            targetState = isDevMode,
            transitionSpec = {
                slideInHorizontally { width -> if (targetState) width else -width } + fadeIn() with
                        slideOutHorizontally { width -> if (targetState) -width else width } + fadeOut()
            },
            label = "mode_switch_animation"
        ) { devMode ->
            if (devMode) {
                // Developer / Programmer Interface
                ProgrammerPanel(
                    viewModel = viewModel,
                    onShowToast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                )
            } else {
                // Patient / User Interface
                UserDashboard(
                    viewModel = viewModel,
                    onSelectWilayaClick = { showWilayaDialog = true },
                    onSelectMunicipalityClick = {
                        if (selectedW == null) {
                            Toast.makeText(context, "الرجاء اختيار الولاية أولاً", Toast.LENGTH_SHORT).show()
                        } else {
                            showMunicipalityDialog = true
                        }
                    },
                    onSelectSpecialtyClick = { showSpecialtyDialog = true },
                    onViewDoctorDetails = { activeDetailDoctor = it }
                )
            }
        }
    }

    // List selection dialogs
    if (showWilayaDialog) {
        WilayaSelectionDialog(
            wilayas = AlgeriaData.wilayas,
            selectedWilaya = selectedW,
            onDismiss = { showWilayaDialog = false },
            onSelect = {
                viewModel.selectWilaya(it)
                showWilayaDialog = false
            }
        )
    }

    if (showMunicipalityDialog && selectedW != null) {
        MunicipalitySelectionDialog(
            municipalities = allMunicipalitiesForSelectedW,
            selectedMunicipality = selectedM,
            wilayaName = selectedW!!.nameAr,
            onDismiss = { showMunicipalityDialog = false },
            onSelect = {
                viewModel.selectMunicipality(it)
                showMunicipalityDialog = false
            }
        )
    }

    if (showSpecialtyDialog) {
        SpecialtySelectionDialog(
            specialties = allSpecialties,
            selectedSpecialty = selectedS,
            onDismiss = { showSpecialtyDialog = false },
            onSelect = {
                viewModel.selectSpecialty(it)
                showSpecialtyDialog = false
            }
        )
    }

    // Doctor detail bottom modal-like dialog
    if (activeDetailDoctor != null) {
        DoctorDetailDialog(
            doctor = activeDetailDoctor!!,
            isFavorite = activeDetailDoctor!!.isFavorite,
            onDismiss = { activeDetailDoctor = null },
            onToggleFavorite = {
                viewModel.toggleFavorite(activeDetailDoctor!!)
                activeDetailDoctor = activeDetailDoctor!!.copy(isFavorite = !activeDetailDoctor!!.isFavorite)
            }
        )
    }

    // Password Dialog Modal for developer access
    if (showPasswordDialog) {
        PasswordAuthDialog(
            passwordInput = passwordInput,
            onPasswordChange = {
                passwordInput = it
                passwordError = false
            },
            isError = passwordError,
            onDismiss = { showPasswordDialog = false },
            onConfirm = {
                if (passwordInput == "ouassiniadmin1985") {
                    viewModel.toggleDeveloperMode()
                    showPasswordDialog = false
                } else {
                    passwordError = true
                }
            }
        )
    }
}

@Composable
fun HeaderBlock(
    isDevMode: Boolean,
    onToggleMode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_header"),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Developer Mode Switcher Action Tab
                Button(
                    onClick = onToggleMode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDevMode) Color.White else EmeraldPrimary.copy(alpha = 0.3F),
                        contentColor = if (isDevMode) EmeraldPrimary else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier.testTag("mode_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isDevMode) Icons.Default.Person else Icons.Default.Code,
                        contentDescription = "Toggle mode",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDevMode) "دليل المرضى" else "لوحة المبرمج",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // App title text and brand indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "طبيبي دز - Tabibi-dz",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "دليل البحث عن الأطباء في الجزائر",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserDashboard(
    viewModel: DoctorViewModel,
    onSelectWilayaClick: () -> Unit,
    onSelectMunicipalityClick: () -> Unit,
    onSelectSpecialtyClick: () -> Unit,
    onViewDoctorDetails: (Doctor) -> Unit
) {
    val searchName by viewModel.nameSearchQuery.collectAsState()
    val filteredDocs by viewModel.filteredDoctors.collectAsState()

    val selectedW by viewModel.selectedWilaya.collectAsState()
    val selectedM by viewModel.selectedMunicipality.collectAsState()
    val selectedS by viewModel.selectedSpecialty.collectAsState()

    val allDocsCount by viewModel.allDoctors.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome slogan & statistics banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HealthAndSafety,
                        contentDescription = "Care logo",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(52.dp)
                    )
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "ابحث عن طبيبك بكل سهولة",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "ابحث بالولاية، البلدية والتخصص لحجز موعدك وتحميل معلومات الاتصال فوراً.",
                            color = DarkSlate.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Search Filter controls card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_filter_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "خيارات البحث والفلترة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkSlate,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Text Query Search Field
                    OutlinedTextField(
                        value = searchName,
                        onValueChange = { viewModel.searchByName(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_name_input"),
                        placeholder = {
                            Text(
                                "ابحث باسم الطبيب...",
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = EmeraldPrimary)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DarkSlate,
                            unfocusedTextColor = DarkSlate,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row filters for Wilaya, Municipality, Specialty selection buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Wilaya Selector Button
                        FilterSelectItem(
                            tag = "select_wilaya",
                            label = "الولاية",
                            currentValue = selectedW?.let { "${it.id} - ${it.nameAr}" } ?: "كل الولايات",
                            icon = Icons.Outlined.Map,
                            onClick = onSelectWilayaClick
                        )

                        // Municipality Selector Button
                        FilterSelectItem(
                            tag = "select_municipality",
                            label = "البلدية",
                            currentValue = selectedM ?: "كل البلديات",
                            icon = Icons.Outlined.LocationCity,
                            onClick = onSelectMunicipalityClick,
                            isEnabled = selectedW != null
                        )

                        // Specialty Selector Button
                        FilterSelectItem(
                            tag = "select_specialty",
                            label = "التخصص",
                            currentValue = selectedS ?: "كل التخصصات",
                            icon = Icons.Outlined.LocalHospital,
                            onClick = onSelectSpecialtyClick
                        )
                    }

                    // Reset Filters Button if any filters are active
                    if (searchName.isNotEmpty() || selectedW != null || selectedM != null || selectedS != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { viewModel.clearFilters() },
                            modifier = Modifier
                                .align(Alignment.Start)
                                .testTag("btn_clear_filters"),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعادة تعيين الفلاتر", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                        }
                    }
                }
            }
        }

        // Section header indicating search results count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(EmeraldPrimary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${filteredDocs.size} طبيب",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "الأطباء الموجودين في الدليل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkSlate,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Doctors dynamic list mapping
        if (filteredDocs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "Empty",
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لم يتم العثور على نتائج للفلترة الحالية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SoftSlate,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "حاول تغيير الكلمات الدلالية أو اختيار بلدية أخرى أو تصفح الأطباء الآخرين.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.clearFilters() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                        ) {
                            Text("عرض كافة الأطباء (${allDocsCount.size})", fontFamily = FontFamily.SansSerif)
                        }
                    }
                }
            }
        } else {
            items(filteredDocs) { doctor ->
                DoctorSearchResultCard(
                    doctor = doctor,
                    onToggleFavorite = { viewModel.toggleFavorite(doctor) },
                    onCardClick = { onViewDoctorDetails(doctor) }
                )
            }
        }
    }
}

@Composable
fun FilterSelectItem(
    tag: String,
    label: String,
    currentValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .testTag(tag),
        color = if (isEnabled) BackgroundGray else Color.LightGray.copy(alpha = 0.3F),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "More",
                tint = if (isEnabled) EmeraldPrimary else Color.Gray
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentValue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) DarkSlate else Color.Gray,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                VerticalDivider(thickness = 1.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) EmeraldPrimary else Color.Gray,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isEnabled) EmeraldPrimary else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun DoctorSearchResultCard(
    doctor: Doctor,
    onToggleFavorite: () -> Unit,
    onCardClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("doctor_card_${doctor.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            1.dp,
            if (doctor.isCustom) EmeraldAccent.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Favorite heart button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("fav_button_${doctor.id}")
                ) {
                    Icon(
                        imageVector = if (doctor.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (doctor.isFavorite) Color.Red else Color.Gray
                    )
                }

                // Doctor Identity Headers
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (doctor.isCustom) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .background(EmeraldAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "مضاف حديثاً",
                                    fontSize = 9.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }

                        Text(
                            text = doctor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkSlate,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Specialty badge decoration
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .background(EmeraldLight, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = doctor.specialty,
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Meta properties list: Wilaya/Municipality, price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${doctor.wilayaNameAr} - ${doctor.municipalityNameAr}",
                    fontSize = 12.sp,
                    color = SoftSlate,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = EmeraldAccent,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${doctor.consultationPrice} دج",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = "Fees",
                    tint = SoftSlate,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: More Info & Dial Direct
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Phone direct
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${doctor.phone}")
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("call_btn_${doctor.id}")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Dial", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اتصال هاتفي", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                }

                // Details anchor
                OutlinedButton(
                    onClick = { onCardClick() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = QuietSlate)
                ) {
                    Text(
                        "التفاصيل والمواعيد",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

val QuietSlate = Color(0xFF37474F)

@Composable
fun ProgrammerPanel(
    viewModel: DoctorViewModel,
    onShowToast: (String) -> Unit
) {
    val listAllDoctors by viewModel.allDoctors.collectAsState()
    val customDoctorsCount = listAllDoctors.filter { it.isCustom }.size

    // Form inputs
    var docName by remember { mutableStateOf("") }
    var docPhone by remember { mutableStateOf("") }
    var docHours by remember { mutableStateOf("08:00 - 16:00") }
    var docPriceStr by remember { mutableStateOf("2000") }
    var docAddress by remember { mutableStateOf("") }

    var selectedSpecialtyIndex by remember { mutableStateOf(0) }
    var selectedWilayaIndex by remember { mutableStateOf(15) } // Default Algiers
    var selectedMunicipalityIndex by remember { mutableStateOf(0) }

    val currentWilaya = AlgeriaData.wilayas[selectedWilayaIndex]
    val currentMunicipalities = AlgeriaData.getMunicipalities(currentWilaya.id)

    // Manual custom entries
    var isCustomSpecialty by remember { mutableStateOf(false) }
    var customSpecialty by remember { mutableStateOf("") }
    var isCustomMunicipality by remember { mutableStateOf(false) }
    var customMunicipality by remember { mutableStateOf("") }

    // Expand dropdown state properties
    var showSpecialtyDropdown by remember { mutableStateOf(false) }
    var showWilayaDropdown by remember { mutableStateOf(false) }
    var showMunicipalityDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("programmer_panel"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QuietSlate)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Dev",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "لوحة تحكم إدارة البيانات للمبرمج",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DevInfoBadge(label = "أطباء مبرمجين", value = customDoctorsCount.toString())
                        DevInfoBadge(label = "أطباء افتراضيين", value = (listAllDoctors.size - customDoctorsCount).toString())
                        DevInfoBadge(label = "إجمالي الأطباء", value = listAllDoctors.size.toString())
                    }
                }
            }
        }

        // Add Doctor Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "إضافة طبيب جديد إلى قاعدة البيانات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkSlate,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = EmeraldPrimary)
                    }

                    // Fields
                    OutlinedTextField(
                        value = docName,
                        onValueChange = { docName = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_doc_title"),
                        placeholder = { Text("اسم الطبيب (مثال: د. سمير بوعمامة)", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = docPhone,
                        onValueChange = { docPhone = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_doc_phone"),
                        placeholder = { Text("رقم الهاتف (مثال: 0555123456)", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = docHours,
                            onValueChange = { docHours = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("أوقات العمل (مثال: 08:00 - 15:30)", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = docPriceStr,
                            onValueChange = { docPriceStr = it },
                            modifier = Modifier.weight(1f).testTag("add_doc_price"),
                            placeholder = { Text("سعر الفحص دج", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = docAddress,
                        onValueChange = { docAddress = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("العنوان الدقيق للعيادة...", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Specialty Manual vs Dropdown Choice
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isCustomSpecialty,
                                onCheckedChange = { isCustomSpecialty = it }
                            )
                            Text("كتابة التخصص يدوياً", fontSize = 12.sp, fontFamily = FontFamily.SansSerif, color = DarkSlate)
                        }
                        Text("التخصص الطبي", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkSlate)
                    }

                    if (isCustomSpecialty) {
                        OutlinedTextField(
                            value = customSpecialty,
                            onValueChange = { customSpecialty = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_custom_specialty"),
                            placeholder = { Text("اكتب التخصص الطبي هنا (مثال: طبيب أعصاب متميز)", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DarkSlate,
                                unfocusedTextColor = DarkSlate,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    } else {
                        DropdownPickerItem(
                            label = "اختر التخصص من القائمة",
                            currentValue = AlgeriaData.specialties[selectedSpecialtyIndex],
                            onExpandRequest = { showSpecialtyDropdown = true }
                        ) {
                            DropdownMenu(
                                expanded = showSpecialtyDropdown,
                                onDismissRequest = { showSpecialtyDropdown = false }
                            ) {
                                AlgeriaData.specialties.forEachIndexed { idx, spec ->
                                    DropdownMenuItem(
                                        text = { Text(spec, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                        onClick = {
                                            selectedSpecialtyIndex = idx
                                            showSpecialtyDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wilaya Selector for Add Form (Always Dropdown, since Algeria has 58 Wilayas)
                    DropdownPickerItem(
                        label = "الولاية",
                        currentValue = "${currentWilaya.id} - ${currentWilaya.nameAr}",
                        onExpandRequest = { showWilayaDropdown = true }
                    ) {
                        DropdownMenu(
                            expanded = showWilayaDropdown,
                            onDismissRequest = { showWilayaDropdown = false }
                        ) {
                            AlgeriaData.wilayas.forEachIndexed { idx, w ->
                                DropdownMenuItem(
                                    text = { Text("${w.id} - ${w.nameAr}", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        selectedWilayaIndex = idx
                                        selectedMunicipalityIndex = 0 // Reset municipality choice
                                        showWilayaDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Municipality Manual vs Dropdown Choice
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isCustomMunicipality,
                                onCheckedChange = { isCustomMunicipality = it }
                            )
                            Text("كتابة البلدية يدوياً", fontSize = 12.sp, fontFamily = FontFamily.SansSerif, color = DarkSlate)
                        }
                        Text("البلدية", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkSlate)
                    }

                    if (isCustomMunicipality) {
                        OutlinedTextField(
                            value = customMunicipality,
                            onValueChange = { customMunicipality = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_custom_municipality"),
                            placeholder = { Text("اكتب البلدية هنا (مثال: بوزريعة، الشراقة...)", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DarkSlate,
                                unfocusedTextColor = DarkSlate,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    } else {
                        DropdownPickerItem(
                            label = "اختر البلدية من القائمة",
                            currentValue = if (currentMunicipalities.isNotEmpty()) currentMunicipalities[selectedMunicipalityIndex % currentMunicipalities.size] else "وسط الولاية",
                            onExpandRequest = { showMunicipalityDropdown = true }
                        ) {
                            DropdownMenu(
                                expanded = showMunicipalityDropdown,
                                onDismissRequest = { showMunicipalityDropdown = false }
                            ) {
                                currentMunicipalities.forEachIndexed { idx, m ->
                                    DropdownMenuItem(
                                        text = { Text(m, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                        onClick = {
                                            selectedMunicipalityIndex = idx
                                            showMunicipalityDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val finalSpecialty = if (isCustomSpecialty) customSpecialty else AlgeriaData.specialties[selectedSpecialtyIndex]
                            val finalMunicipality = if (isCustomMunicipality) customMunicipality else {
                                if (currentMunicipalities.isNotEmpty()) currentMunicipalities[selectedMunicipalityIndex % currentMunicipalities.size] else "وسط الولاية"
                            }

                            if (docName.isBlank() || docPhone.isBlank() || docAddress.isBlank() || finalSpecialty.isBlank() || finalMunicipality.isBlank()) {
                                onShowToast("الرجاء ملء جميع الحقول المطلوبة بما في ذلك التخصص والبلدية")
                            } else {
                                val priceInt = docPriceStr.toIntOrNull() ?: 2000

                                viewModel.addDoctor(
                                    name = docName,
                                    specialty = finalSpecialty,
                                    wilaya = currentWilaya,
                                    municipality = finalMunicipality,
                                    phone = docPhone,
                                    address = docAddress,
                                    price = priceInt,
                                    workingHours = docHours
                                )

                                onShowToast("تمت إضافة الطبيب بنجاح إلى قاعدة البيانات!")
                                // Reset text fields
                                docName = ""
                                docPhone = ""
                                docAddress = ""
                                customSpecialty = ""
                                customMunicipality = ""
                                isCustomSpecialty = false
                                isCustomMunicipality = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_add_doctor")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الطبيب في قاعدة البيانات", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }

        // Quick Admin Commands
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "خيارات قواعد البيانات والأدوات السريعة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkSlate,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.reloadDemoData()
                            onShowToast("تمت إعادة تهيئة البيانات الافتراضية بنجاح!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = QuietSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reload_demo_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = EmeraldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعادة تحميل وتصفير قاعدة البيانات الافتراضية",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // List of all Doctors in DB with delete control
        item {
            Text(
                text = "قائمة الأطباء الحاليين للإدارة والدراسة (${listAllDoctors.size} طبيب)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DarkSlate,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(listAllDoctors) { doctor ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3F))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Delete Button
                    IconButton(
                        onClick = {
                            viewModel.deleteDoctor(doctor)
                            onShowToast("تم حذف هذا الطبيب.")
                        },
                        modifier = Modifier.testTag("delete_doc_${doctor.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.8F)
                        )
                    }

                    // Doctor Metadata details brief
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = doctor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkSlate,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = doctor.specialty,
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "${doctor.wilayaNameAr} - ${doctor.municipalityNameAr}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DevInfoBadge(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08F)),
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = EmeraldAccent)
            Text(text = label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.SansSerif)
        }
    }
}

@Composable
fun DropdownPickerItem(
    label: String,
    currentValue: String,
    onExpandRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = SoftSlate,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onExpandRequest() }
                .background(BackgroundGray)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                Text(
                    text = currentValue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate,
                    fontFamily = FontFamily.SansSerif
                )
            }
            content()
        }
    }
}

// -------------------- Selection Dialogs --------------------

@Composable
fun WilayaSelectionDialog(
    wilayas: List<Wilaya>,
    selectedWilaya: Wilaya?,
    onDismiss: () -> Unit,
    onSelect: (Wilaya?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = wilayas.filter {
        it.nameAr.contains(searchQuery, ignoreCase = true) ||
                it.nameEn.contains(searchQuery, ignoreCase = true) ||
                it.id.toString() == searchQuery
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Header
                Text(
                    text = "اختر الولاية من القائمة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkSlate,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Search field inside Dialog!
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث عن ولاية (مثلاً: الجزائر، وهران...)", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        ListItem(
                            headlineContent = { Text("كل الولايات", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .clickable { onSelect(null) }
                                .fillMaxWidth()
                        )
                        HorizontalDivider()
                    }

                    items(filtered) { w ->
                        val isSelected = selectedWilaya?.id == w.id
                        ListItem(
                            headlineContent = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = w.nameEn, color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        text = "${w.id} - ${w.nameAr}",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EmeraldPrimary else DarkSlate
                                    )
                                }
                            },
                            modifier = Modifier
                                .clickable { onSelect(w) }
                                .background(if (isSelected) EmeraldLight else Color.Transparent)
                                .fillMaxWidth()
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }

                // Footer Actions
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("إلغاء", color = Color.Red, fontFamily = FontFamily.SansSerif)
                }
            }
        }
    }
}

@Composable
fun MunicipalitySelectionDialog(
    municipalities: List<String>,
    selectedMunicipality: String?,
    wilayaName: String,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = municipalities.filter { it.contains(searchQuery, ignoreCase = true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "بلديات ولاية $wilayaName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkSlate,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث عن بلدية...", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        ListItem(
                            headlineContent = { Text("كل بلديات ولاية $wilayaName", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .clickable { onSelect(null) }
                                .fillMaxWidth()
                        )
                        HorizontalDivider()
                    }

                    items(filtered) { m ->
                        val isSelected = selectedMunicipality == m
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = m,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else DarkSlate
                                )
                            },
                            modifier = Modifier
                                .clickable { onSelect(m) }
                                .background(if (isSelected) EmeraldLight else Color.Transparent)
                                .fillMaxWidth()
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("إلغاء", color = Color.Red, fontFamily = FontFamily.SansSerif)
                }
            }
        }
    }
}

@Composable
fun SpecialtySelectionDialog(
    specialties: List<String>,
    selectedSpecialty: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = specialties.filter { it.contains(searchQuery, ignoreCase = true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "اختر تخصص الطبيب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkSlate,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث عن تخصص...", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        ListItem(
                            headlineContent = { Text("كل التخصصات الطبية", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .clickable { onSelect(null) }
                                .fillMaxWidth()
                        )
                        HorizontalDivider()
                    }

                    items(filtered) { s ->
                        val isSelected = selectedSpecialty == s
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = s,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else DarkSlate
                                )
                            },
                            modifier = Modifier
                                .clickable { onSelect(s) }
                                .background(if (isSelected) EmeraldLight else Color.Transparent)
                                .fillMaxWidth()
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("إلغاء", color = Color.Red, fontFamily = FontFamily.SansSerif)
                }
            }
        }
    }
}

@Composable
fun DoctorDetailDialog(
    doctor: Doctor,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Header brand with close and favorite hearts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Fav",
                                tint = if (isFavorite) Color.Red else Color.Gray
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(EmeraldLight, CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = "Doc", tint = EmeraldPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Doctor Information Text Labels
                Text(
                    text = doctor.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = DarkSlate,
                    fontFamily = FontFamily.SansSerif
                )

                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .background(EmeraldPrimary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = doctor.specialty,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Consultation ratings
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "(${doctor.rating}) تقييم المرضى",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < 4) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Star",
                                tint = AmberRating,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.4f))

                // Location Details Row block
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${doctor.wilayaNameAr}، ${doctor.municipalityNameAr}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkSlate,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                }

                // Exact clinic address
                Text(
                    text = doctor.address,
                    fontSize = 12.sp,
                    color = SoftSlate,
                    textAlign = TextAlign.End,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.fillMaxWidth().padding(end = 28.dp, bottom = 8.dp)
                )

                // Working Hours
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = doctor.workingHours,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = DarkSlate,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.AccessTime, contentDescription = "Hours", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                }

                // Consultation Price / Fees
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${doctor.consultationPrice} دج (سعر فحص تقديري)",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = EmeraldPrimary,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Outlined.Payments, contentDescription = "Price", tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom CTA action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Close button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إغلاق", color = SoftSlate, fontFamily = FontFamily.SansSerif)
                    }

                    // Direct Call Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${doctor.phone}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Phone")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اتصل بالعيادة", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordAuthDialog(
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    isError: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("password_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock Icon with glowing background circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(EmeraldLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "لوحة التحكم محمية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkSlate,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "يرجى إدخال كود المرور الخاص بالمبرمج للوصول لخيارات التحكم وإضافة الأطباء",
                    fontSize = 12.sp,
                    color = SoftSlate,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password OutlinedTextField with visual transformation
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = onPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input_field"),
                    placeholder = {
                        Text(
                            "كود المرور السري...",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = isError,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkSlate,
                        unfocusedTextColor = DarkSlate,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color.LightGray,
                        errorBorderColor = Color.Red
                    )
                )

                if (isError) {
                    Text(
                        text = "كود المرور خاطئ! يرجى المحاولة مجدداً.",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp, end = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftSlate)
                    ) {
                        Text("إلغاء", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
                    }

                    // Confirm
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("تأكيد الدخول", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }
    }
}
