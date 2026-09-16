package com.experiment.userservice.controller

import com.experiment.userservice.dto.CreateUserRequest
import com.experiment.userservice.dto.PageResponse
import com.experiment.userservice.dto.UserResponse
import com.experiment.userservice.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping
    fun createUser(
        @RequestBody request: CreateUserRequest
    ): ResponseEntity<UserResponse> {

        val user = userService.createUser(request)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(user)
    }

    @GetMapping("/{id}")
    fun getUserById(
        @PathVariable id: String
    ): ResponseEntity<UserResponse> {

        return ResponseEntity.ok(
            userService.getUserById(id)
        )
    }

    @GetMapping
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PageResponse<UserResponse>> {

        return ResponseEntity.ok(
            userService.getAllUsers(page, size)
        )
    }
}