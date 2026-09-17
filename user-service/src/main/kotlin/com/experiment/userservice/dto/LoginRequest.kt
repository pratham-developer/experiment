package com.experiment.userservice.dto

data class LoginRequest(
    val email: String,
    val password: String
)