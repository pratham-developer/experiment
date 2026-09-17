package com.experiment.userservice.entity

enum class Role {
    CUSTOMER,
    SELLER;

    companion object {
        fun from(value: String): Role =
            entries.firstOrNull {
                it.name.equals(value.trim(), ignoreCase = true)
            } ?: throw IllegalArgumentException("Invalid role: $value")
    }
}