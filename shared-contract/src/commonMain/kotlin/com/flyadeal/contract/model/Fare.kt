package com.flyadeal.contract.model

import kotlinx.serialization.Serializable

/**
 * Represents a fare option with pricing and inclusions.
 * Each flight offers exactly 3 fare families: Fly, Fly+, and FlyMax.
 */
@Serializable
data class FareFamily(
    val code: FareFamilyCode,
    val name: String,
    val price: Money,
    val inclusions: FareInclusions
)

/**
 * Fare family identifier codes.
 * flyadeal offers three tiers of service.
 */
@Serializable
enum class FareFamilyCode(val displayName: String) {
    /**
     * Basic fare with minimal inclusions.
     */
    FLY("Fly"),

    /**
     * Standard fare with extras (baggage, seat selection).
     */
    FLY_PLUS("Fly+"),

    /**
     * Premium fare with maximum flexibility and inclusions.
     */
    FLY_MAX("FlyMax")
}

/**
 * Fare inclusions and policies for a fare family.
 * Defines what is included in the ticket price.
 */
@Serializable
data class FareInclusions(
    val carryOnBag: BagAllowance,
    val checkedBag: BagAllowance?,
    val seatSelection: SeatSelectionType,
    val changePolicy: ChangePolicy,
    val cancellationPolicy: CancellationPolicy,
    val priorityBoarding: Boolean,
    val loungeAccess: Boolean
) {
    companion object {
        /**
         * Standard inclusions for the Fly (basic) fare family.
         */
        val FLY_INCLUSIONS = FareInclusions(
            carryOnBag = BagAllowance(pieces = 1, weightKg = 7),
            checkedBag = null,
            seatSelection = SeatSelectionType.NONE,
            changePolicy = ChangePolicy(allowed = true, feeAmount = Money.sar(150.0)),
            cancellationPolicy = CancellationPolicy(allowed = false, refundPercentage = 0),
            priorityBoarding = false,
            loungeAccess = false
        )

        /**
         * Standard inclusions for the Fly+ fare family.
         */
        val FLY_PLUS_INCLUSIONS = FareInclusions(
            carryOnBag = BagAllowance(pieces = 1, weightKg = 7),
            checkedBag = BagAllowance(pieces = 1, weightKg = 20),
            seatSelection = SeatSelectionType.STANDARD,
            changePolicy = ChangePolicy(allowed = true, feeAmount = Money.sar(100.0)),
            cancellationPolicy = CancellationPolicy(allowed = true, refundPercentage = 50),
            priorityBoarding = false,
            loungeAccess = false
        )

        /**
         * Standard inclusions for the FlyMax (premium) fare family.
         */
        val FLY_MAX_INCLUSIONS = FareInclusions(
            carryOnBag = BagAllowance(pieces = 1, weightKg = 7),
            checkedBag = BagAllowance(pieces = 1, weightKg = 30),
            seatSelection = SeatSelectionType.PREMIUM,
            changePolicy = ChangePolicy(allowed = true, feeAmount = null),
            cancellationPolicy = CancellationPolicy(allowed = true, refundPercentage = 100),
            priorityBoarding = true,
            loungeAccess = true
        )

        /**
         * Returns the standard inclusions for a given fare family code.
         * @param code The fare family code
         * @return The standard FareInclusions for that fare family
         */
        fun forFareFamily(code: FareFamilyCode): FareInclusions = when (code) {
            FareFamilyCode.FLY -> FLY_INCLUSIONS
            FareFamilyCode.FLY_PLUS -> FLY_PLUS_INCLUSIONS
            FareFamilyCode.FLY_MAX -> FLY_MAX_INCLUSIONS
        }
    }
}

/**
 * Baggage allowance specification.
 */
@Serializable
data class BagAllowance(
    val pieces: Int,
    val weightKg: Int
) {
    init {
        require(pieces > 0) { "Bag pieces must be positive: $pieces" }
        require(weightKg > 0) { "Weight must be positive: $weightKg" }
    }

    /**
     * Returns a display string like "1 x 20kg".
     */
    fun formatDisplay(): String = "$pieces x ${weightKg}kg"
}

/**
 * Type of seat selection available.
 */
@Serializable
enum class SeatSelectionType(val displayName: String) {
    /**
     * No seat selection - assigned at check-in.
     */
    NONE("None"),

    /**
     * Standard seat selection (non-premium seats).
     */
    STANDARD("Standard"),

    /**
     * Premium seat selection (extra legroom, front rows).
     */
    PREMIUM("Premium (incl. extra legroom)")
}

/**
 * Flight change policy for a fare.
 */
@Serializable
data class ChangePolicy(
    val allowed: Boolean,
    val feeAmount: Money?
) {
    /**
     * Returns a display string describing the change policy.
     */
    fun formatDisplay(): String = when {
        !allowed -> "Not allowed"
        feeAmount == null -> "Free changes"
        else -> "Fee: ${feeAmount.formatDisplay()}"
    }
}

/**
 * Cancellation/refund policy for a fare.
 */
@Serializable
data class CancellationPolicy(
    val allowed: Boolean,
    val refundPercentage: Int
) {
    init {
        require(refundPercentage in 0..100) {
            "Refund percentage must be between 0 and 100: $refundPercentage"
        }
    }

    /**
     * Returns a display string describing the cancellation policy.
     */
    fun formatDisplay(): String = when {
        !allowed -> "Non-refundable"
        refundPercentage == 100 -> "Full refund"
        refundPercentage == 0 -> "No refund"
        else -> "$refundPercentage% refund"
    }
}
