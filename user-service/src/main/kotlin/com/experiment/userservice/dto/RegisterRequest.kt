package com.experiment.userservice.dto

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val phone: String? = null,
    val address: String? = null
)