package com.experiment.userservice.controller

import com.experiment.userservice.dto.LoginRequest
import com.experiment.userservice.dto.LoginResponse
import com.experiment.userservice.dto.RegisterRequest
import com.experiment.userservice.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest) {
        authService.registerUser(request)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        return authService.loginUser(request)
    }
}