package com.experiment.userservice.service

import com.experiment.userservice.dto.LoginRequest
import com.experiment.userservice.dto.LoginResponse
import com.experiment.userservice.dto.RegisterRequest
import com.experiment.userservice.entity.Role
import com.experiment.userservice.entity.User
import com.experiment.userservice.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService
) {
    fun registerUser(request: RegisterRequest){
        if(userRepository.existsByEmail(request.email)){
            throw IllegalArgumentException("User email already exists")
        }

        val user = User(
            name = request.name,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
                ?: throw IllegalStateException("Password encoding failed"),
            role = Role.from(request.role),
            phone = request.phone,
            address = request.address
        )

        userRepository.save(user)
    }

    fun loginUser(request: LoginRequest): LoginResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.email,
                request.password
            )
        )

        val user = authentication.principal as User

        return LoginResponse(
            accessToken = jwtService.generateAccessToken(user),
            userId = user.id!!,
            email = user.email,
            role = user.role.name
        )
    }
}