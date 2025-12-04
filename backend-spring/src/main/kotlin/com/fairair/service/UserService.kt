package com.fairair.service

import com.fairair.entity.UserEntity
import com.fairair.repository.UserRepository
import kotlinx.coroutines.flow.toList
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

/**
 * User service for managing users.
 * Uses database-backed storage via Spring Data R2DBC.
 * 
 * Demo users are seeded via data.sql on startup.
 */
@Service
class UserService(
    private val userRepository: UserRepository
) {

    private val passwordEncoder = BCryptPasswordEncoder()

    suspend fun findByEmail(email: String): DemoUser? {
        return userRepository.findByEmailIgnoreCase(email)?.toDemoUser()
    }

    suspend fun validateCredentials(email: String, password: String): DemoUser? {
        val user = userRepository.findByEmailIgnoreCase(email) ?: return null
        return if (passwordEncoder.matches(password, user.passwordHash)) {
            user.toDemoUser()
        } else {
            null
        }
    }

    suspend fun getAllUsers(): List<DemoUser> {
        return userRepository.findAll().toList().map { it.toDemoUser() }
    }
    
    private fun UserEntity.toDemoUser() = DemoUser(
        id = id,
        email = email,
        password = passwordHash,
        firstName = firstName,
        lastName = lastName,
        role = UserRole.valueOf(role)
    )
}

/**
 * Demo user data class.
 */
data class DemoUser(
    val id: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole
)

/**
 * User roles.
 */
enum class UserRole {
    USER,
    EMPLOYEE,
    ADMIN
}
