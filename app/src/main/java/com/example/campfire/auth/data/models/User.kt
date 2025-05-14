package com.example.campfire.auth.data.models

data class User(
    val email: String,
    val isActive: Boolean,
    val isSuperuser: Boolean,
    val fullName: String,
    val id: Int
)