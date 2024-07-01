package com.example.campfire


data class User(
    val email: String,
    val isActive: Boolean,
    val isSuperuser: Boolean,
    val fullName: String,
    val id: Int
)
