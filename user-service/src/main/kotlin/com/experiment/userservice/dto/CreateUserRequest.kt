package com.experiment.userservice.dto

import com.experiment.userservice.entity.Role

data class CreateUserRequest(
    val name: String,
    val email: String,
    val role: Role = Role.CUSTOMER,
    val phone: String? = null,
    val address: String? = null
)