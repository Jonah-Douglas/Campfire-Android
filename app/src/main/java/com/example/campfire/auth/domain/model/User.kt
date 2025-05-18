package com.example.campfire.auth.domain.model


@Suppress("unused")
// User model defined by Campfire API
data class User(
    val email: String,
    val isActive: Boolean,
    val isSuperuser: Boolean,
    val fullName: String,
    val id: Int
)
