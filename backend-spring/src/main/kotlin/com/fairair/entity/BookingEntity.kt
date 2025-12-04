package com.fairair.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * Booking entity for database persistence.
 * Stores all booking confirmations.
 */
@Table("bookings")
data class BookingEntity(
    @Id
    @Column("pnr")
    val pnr: String,
    
    @Column("booking_reference")
    val bookingReference: String,
    
    @Column("user_id")
    val userId: String? = null,
    
    @Column("flight_number")
    val flightNumber: String,
    
    @Column("origin")
    val origin: String,
    
    @Column("destination")
    val destination: String,
    
    @Column("departure_time")
    val departureTime: Instant,
    
    @Column("fare_family")
    val fareFamily: String,
    
    @Column("passengers_json")
    val passengersJson: String,
    
    @Column("total_amount")
    val totalAmount: Double,
    
    @Column("currency")
    val currency: String,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
