package com.example.campfire.auth.domain.model

import com.example.campfire.core.domain.model.PhoneNumber
import java.time.LocalDate
import java.time.LocalDateTime


data class User(
    val id: Long,
    val phone: PhoneNumber,
    val firstName: String,
    val lastName: String,
    val email: String,
    val dateOfBirth: LocalDate,
    val enableNotifications: Boolean,
    val isProfileComplete: Boolean,
    val isActive: Boolean,
    val isSuperuser: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)