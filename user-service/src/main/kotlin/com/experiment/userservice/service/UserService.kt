package com.experiment.userservice.service

import com.experiment.userservice.dto.CreateUserRequest
import com.experiment.userservice.dto.PageResponse
import com.experiment.userservice.dto.UserResponse
import com.experiment.userservice.entity.User
import com.experiment.userservice.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {

    companion object {
        const val MAX_PAGE_SIZE = 50
    }

    fun createUser(request: CreateUserRequest): UserResponse {

        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("User email already exists")
        }

        val user = User(
            name = request.name,
            email = request.email,
            role = request.role,
            phone = request.phone,
            address = request.address
        )

        val savedUser = userRepository.save(user)

        return savedUser.toResponse()
    }

    fun getUserById(id: String): UserResponse {

        val user = userRepository.findById(id)
            .orElseThrow {
                NoSuchElementException("User not found with id: $id")
            }

        return user.toResponse()
    }

    fun getAllUsers(
        page: Int,
        size: Int
    ): PageResponse<UserResponse> {

        require(page >= 0) {
            "Page must be greater than or equal to 0"
        }

        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)

        val pageable = PageRequest.of(
            page,
            pageSize,
            Sort.by(
                Sort.Direction.DESC,
                "createdAt"
            )
        )

        val userPage = userRepository.findAll(pageable)

        return PageResponse(
            content = userPage.content.map { it.toResponse() },
            page = userPage.number,
            size = userPage.size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages,
            hasNext = userPage.hasNext(),
            hasPrevious = userPage.hasPrevious()
        )
    }

    private fun User.toResponse(): UserResponse {
        return UserResponse(
            id = id,
            name = name,
            email = email,
            role = role,
            phone = phone,
            address = address,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}