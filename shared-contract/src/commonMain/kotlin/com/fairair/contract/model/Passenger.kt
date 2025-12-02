package com.fairair.contract.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Request payload specifying the number of passengers by type.
 * Used in flight search to determine pricing and availability.
 */
@Serializable
data class PassengerCounts(
    val adults: Int,
    val children: Int,
    val infants: Int
) {
    init {
        require(adults in 1..9) {
            "Adults must be between 1 and 9, got $adults"
        }
        require(children in 0..8) {
            "Children must be between 0 and 8, got $children"
        }
        require(infants in 0..adults) {
            "Infants ($infants) cannot exceed number of adults ($adults)"
        }
        require(adults + children + infants <= 9) {
            "Total passengers cannot exceed 9, got ${adults + children + infants}"
        }
    }

    /**
     * Total number of passengers across all types.
     */
    val total: Int
        get() = adults + children + infants

    companion object {
        /**
         * Creates a PassengerCounts with only adult passengers.
         * @param count Number of adults (1-9)
         * @return A new PassengerCounts instance
         */
        fun adultsOnly(count: Int): PassengerCounts =
            PassengerCounts(adults = count, children = 0, infants = 0)

        /**
         * Single adult passenger - common default.
         */
        val SINGLE_ADULT = PassengerCounts(adults = 1, children = 0, infants = 0)
    }
}

/**
 * Complete passenger information for booking.
 * All fields must match the traveler's official travel document.
 */
@Serializable
data class Passenger(
    val type: PassengerType,
    val title: Title,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val dateOfBirth: LocalDate,
    val documentId: String
) {
    init {
        require(firstName.length in 2..50) {
            "First name must be between 2 and 50 characters, got ${firstName.length}"
        }
        require(firstName.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' }) {
            "First name must contain only letters, spaces, hyphens, or apostrophes: '$firstName'"
        }
        require(lastName.length in 2..50) {
            "Last name must be between 2 and 50 characters, got ${lastName.length}"
        }
        require(lastName.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' }) {
            "Last name must contain only letters, spaces, hyphens, or apostrophes: '$lastName'"
        }
        require(nationality.length == 2 && nationality.all { it.isUpperCase() }) {
            "Nationality must be a 2-letter ISO 3166-1 alpha-2 code: '$nationality'"
        }
        require(documentId.length in 5..20) {
            "Document ID must be between 5 and 20 characters, got ${documentId.length}"
        }
        require(documentId.all { it.isLetterOrDigit() }) {
            "Document ID must be alphanumeric: '$documentId'"
        }
    }

    /**
     * Returns the passenger's full name (first + last).
     */
    val fullName: String
        get() = "$firstName $lastName"

    /**
     * Returns the full name with title prefix.
     */
    val displayName: String
        get() = "${title.displayText} $firstName $lastName"
}

/**
 * Passenger type based on age at time of travel.
 * Age boundaries follow industry standard:
 * - Adult: 12+ years
 * - Child: 2-11 years
 * - Infant: 0-1 years (under 2)
 */
@Serializable
enum class PassengerType {
    ADULT,
    CHILD,
    INFANT;

    companion object {
        /**
         * Determines passenger type based on date of birth relative to travel date.
         * @param dateOfBirth The passenger's birth date
         * @param travelDate The date of travel
         * @return The appropriate PassengerType
         */
        fun fromDateOfBirth(dateOfBirth: LocalDate, travelDate: LocalDate): PassengerType {
            val ageInYears = calculateAge(dateOfBirth, travelDate)
            return when {
                ageInYears >= 12 -> ADULT
                ageInYears >= 2 -> CHILD
                else -> INFANT
            }
        }

        private fun calculateAge(birthDate: LocalDate, referenceDate: LocalDate): Int {
            var age = referenceDate.year - birthDate.year
            if (referenceDate.monthNumber < birthDate.monthNumber ||
                (referenceDate.monthNumber == birthDate.monthNumber &&
                    referenceDate.dayOfMonth < birthDate.dayOfMonth)
            ) {
                age--
            }
            return age
        }
    }
}

/**
 * Salutation/title for passenger.
 */
@Serializable
enum class Title(val displayText: String) {
    MR("Mr"),
    MRS("Mrs"),
    MS("Ms"),
    MISS("Miss"),
    MSTR("Master");

    companion object {
        /**
         * Returns appropriate titles for a given passenger type.
         * @param type The passenger type
         * @return List of valid titles for that passenger type
         */
        fun forPassengerType(type: PassengerType): List<Title> = when (type) {
            PassengerType.ADULT -> listOf(MR, MRS, MS, MISS)
            PassengerType.CHILD -> listOf(MR, MISS, MSTR)
            PassengerType.INFANT -> listOf(MR, MISS, MSTR)
        }
    }
}

/**
 * Summary of passenger information for confirmation display.
 * Contains only non-sensitive information.
 */
@Serializable
data class PassengerSummary(
    val fullName: String,
    val type: PassengerType
)
