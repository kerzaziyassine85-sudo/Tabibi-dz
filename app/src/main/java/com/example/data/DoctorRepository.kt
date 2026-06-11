package com.example.data

import kotlinx.coroutines.flow.Flow

class DoctorRepository(private val doctorDao: DoctorDao) {
    val allDoctors: Flow<List<Doctor>> = doctorDao.getAllDoctors()
    val favoriteDoctors: Flow<List<Doctor>> = doctorDao.getFavoriteDoctors()

    suspend fun insertDoctor(doctor: Doctor) {
        doctorDao.insertDoctor(doctor)
    }

    suspend fun insertAll(doctors: List<Doctor>) {
        doctorDao.insertAll(doctors)
    }

    suspend fun updateDoctor(doctor: Doctor) {
        doctorDao.updateDoctor(doctor)
    }

    suspend fun deleteDoctor(doctor: Doctor) {
        doctorDao.deleteDoctor(doctor)
    }

    suspend fun deleteDoctorById(id: Int) {
        doctorDao.deleteDoctorById(id)
    }

    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) {
        doctorDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun clearDefaultDoctors() {
        doctorDao.clearDefaultDoctors()
    }
}
