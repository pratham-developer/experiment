package com.experiment.userservice.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class User(

    @Id
    val id: String? = null,

    val name: String,

    @Indexed(unique = true)
    val email: String,

    val role: Role = Role.CUSTOMER,

    val phone: String? = null,

    val address: String? = null,

    @CreatedDate
    val createdAt: Instant? = null,

    @LastModifiedDate
    val updatedAt: Instant? = null
)