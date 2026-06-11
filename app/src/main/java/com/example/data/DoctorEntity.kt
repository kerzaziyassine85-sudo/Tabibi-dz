package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val specialty: String,
    val wilayaNameAr: String,
    val wilayaNameEn: String,
    val municipalityNameAr: String,
    val phone: String,
    val address: String,
    val consultationPrice: Int,
    val rating: Float = 4.0f,
    val workingHours: String = "08:00 - 16:00",
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false // True if added by programmer via control panel
)
