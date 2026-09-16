package com.experiment.userservice.dto

import com.experiment.userservice.entity.Role
import java.time.Instant

data class UserResponse(
    val id: String?,
    val name: String,
    val email: String,
    val role: Role,
    val phone: String?,
    val address: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)