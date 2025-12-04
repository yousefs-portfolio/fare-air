package com.fairair.repository

import com.fairair.entity.UserEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/**
 * Repository for User entities.
 * Uses Spring Data R2DBC with Kotlin coroutines for reactive database access.
 */
@Repository
interface UserRepository : CoroutineCrudRepository<UserEntity, String> {
    
    /**
     * Find user by email address (case-insensitive).
     */
    suspend fun findByEmailIgnoreCase(email: String): UserEntity?
}
