package com.experiment.userservice.entity

enum class Role {
    CUSTOMER,
    SELLER;

    companion object {
        fun from(value: String): Role =
            try {
                valueOf(value.trim().uppercase())
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid role: $value")
            }
    }
}