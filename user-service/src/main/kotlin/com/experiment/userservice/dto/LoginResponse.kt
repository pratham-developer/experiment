package com.experiment.userservice.dto

data class LoginResponse(
    val accessToken: String,
    val userId: String,
    val email: String,
    val role: String
)