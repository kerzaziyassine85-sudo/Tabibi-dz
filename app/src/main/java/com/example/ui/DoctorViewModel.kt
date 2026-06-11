package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlgeriaData
import com.example.data.AppDatabase
import com.example.data.Doctor
import com.example.data.DoctorRepository
import com.example.data.Wilaya
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DoctorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DoctorRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DoctorRepository(database.doctorDao)

        // Pre-populate database with default demo doctors if database is empty
        viewModelScope.launch {
            repository.allDoctors.collect { list ->
                if (list.isEmpty()) {
                    repository.insertAll(AlgeriaData.defaultDoctors)
                }
            }
        }
    }

    // UI Panel Modes: false = Patient Mode (User), true = Programmer Mode (Developer)
    val isDeveloperMode = MutableStateFlow(false)

    // Filter states
    val nameSearchQuery = MutableStateFlow("")
    val selectedWilaya = MutableStateFlow<Wilaya?>(null)
    val selectedMunicipality = MutableStateFlow<String?>(null)
    val selectedSpecialty = MutableStateFlow<String?>(null)

    // DB backed flows
    val allDoctors: StateFlow<List<Doctor>> = repository.allDoctors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDoctors: StateFlow<List<Doctor>> = repository.favoriteDoctors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered doctor list for searching
    val filteredDoctors: StateFlow<List<Doctor>> = combine(
        allDoctors,
        nameSearchQuery,
        selectedWilaya,
        selectedMunicipality,
        selectedSpecialty
    ) { doctors, query, wilaya, municipality, specialty ->
        doctors.filter { doc ->
            val matchesName = query.isEmpty() || doc.name.contains(query, ignoreCase = true)
            val matchesWilaya = wilaya == null || doc.wilayaNameAr == wilaya.nameAr || doc.wilayaNameEn == wilaya.nameEn
            val matchesMunicipality = municipality == null || doc.municipalityNameAr == municipality
            val matchesSpecialty = specialty == null || doc.specialty == specialty

            matchesName && matchesWilaya && matchesMunicipality && matchesSpecialty
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDeveloperMode() {
        isDeveloperMode.value = !isDeveloperMode.value
    }

    fun searchByName(query: String) {
        nameSearchQuery.value = query
    }

    fun selectWilaya(wilaya: Wilaya?) {
        selectedWilaya.value = wilaya
        selectedMunicipality.value = null // reset municipality when wilaya changes
    }

    fun selectMunicipality(municipality: String?) {
        selectedMunicipality.value = municipality
    }

    fun selectSpecialty(specialty: String?) {
        selectedSpecialty.value = specialty
    }

    fun clearFilters() {
        nameSearchQuery.value = ""
        selectedWilaya.value = null
        selectedMunicipality.value = null
        selectedSpecialty.value = null
    }

    // Database operations
    fun addDoctor(
        name: String,
        specialty: String,
        wilaya: Wilaya,
        municipality: String,
        phone: String,
        address: String,
        price: Int,
        workingHours: String = "08:00 - 16:00"
    ) {
        viewModelScope.launch {
            val doctor = Doctor(
                name = name,
                specialty = specialty,
                wilayaNameAr = wilaya.nameAr,
                wilayaNameEn = wilaya.nameEn,
                municipalityNameAr = municipality,
                phone = phone,
                address = address,
                consultationPrice = price,
                workingHours = workingHours,
                isCustom = true
            )
            repository.insertDoctor(doctor)
        }
    }

    fun deleteDoctor(doctor: Doctor) {
        viewModelScope.launch {
            repository.deleteDoctor(doctor)
        }
    }

    fun toggleFavorite(doctor: Doctor) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(doctor.id, !doctor.isFavorite)
        }
    }

    fun reloadDemoData() {
        viewModelScope.launch {
            repository.clearDefaultDoctors()
            repository.insertAll(AlgeriaData.defaultDoctors)
        }
    }
}
