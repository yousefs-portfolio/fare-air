package com.fairair.repository

import com.fairair.entity.BookingEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/**
 * Repository for Booking entities.
 * Uses Spring Data R2DBC with Kotlin coroutines for reactive database access.
 */
@Repository
interface BookingRepository : CoroutineCrudRepository<BookingEntity, String> {
    
    /**
     * Find all bookings for a specific user.
     */
    fun findByUserId(userId: String): Flow<BookingEntity>
    
    /**
     * Find booking by PNR code.
     */
    suspend fun findByPnr(pnr: String): BookingEntity?
}
