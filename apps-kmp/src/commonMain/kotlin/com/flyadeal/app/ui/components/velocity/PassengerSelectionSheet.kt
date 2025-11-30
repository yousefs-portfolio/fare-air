package com.flyadeal.app.ui.components.velocity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flyadeal.app.localization.AppStrings
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * Data class for passenger counts.
 */
data class PassengerCounts(
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0
) {
    val total: Int get() = adults + children + infants
}

/**
 * Passenger count selection sheet.
 *
 * Allows selection of:
 * - Adults: 1-9 passengers (12+ years)
 * - Children: 0-8 passengers (2-11 years)
 * - Infants: 0-4 passengers (under 2 years, max = adults count)
 *
 * Uses increment/decrement controls with visual feedback.
 *
 * @param title The title to display at the top
 * @param currentAdults The currently selected adult count
 * @param currentChildren The currently selected children count
 * @param currentInfants The currently selected infant count
 * @param strings Localized strings for labels
 * @param onSelect Callback when counts are confirmed
 * @param onDismiss Callback when the sheet is dismissed
 */
@Composable
fun PassengerSelectionSheet(
    title: String,
    currentAdults: Int,
    currentChildren: Int,
    currentInfants: Int,
    strings: AppStrings,
    onSelect: (PassengerCounts) -> Unit,
    onDismiss: () -> Unit
) {
    val typography = VelocityTheme.typography
    var adultsCount by remember(currentAdults) { mutableStateOf(currentAdults) }
    var childrenCount by remember(currentChildren) { mutableStateOf(currentChildren) }
    var infantsCount by remember(currentInfants) { mutableStateOf(currentInfants) }

    // Infants cannot exceed adults count
    val maxInfants = adultsCount.coerceAtMost(4)

    // Adjust infants if adults decreased below current infants
    LaunchedEffect(adultsCount) {
        if (infantsCount > maxInfants) {
            infantsCount = maxInfants
        }
    }

    // Total passengers (excluding infants as they sit on laps)
    val totalSeatedPassengers = adultsCount + childrenCount
    val maxTotalSeated = 9

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = VelocityColors.BackgroundDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = typography.timeBig
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = VelocityColors.TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Adults counter
            PassengerCounter(
                label = strings.adults,
                description = "12+ years",
                count = adultsCount,
                minValue = 1,
                maxValue = (maxTotalSeated - childrenCount).coerceAtLeast(1),
                onIncrement = {
                    if (totalSeatedPassengers < maxTotalSeated) {
                        adultsCount = (adultsCount + 1).coerceAtMost(9)
                    }
                },
                onDecrement = { adultsCount = (adultsCount - 1).coerceAtLeast(1) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Children counter
            PassengerCounter(
                label = strings.children,
                description = "2-11 years",
                count = childrenCount,
                minValue = 0,
                maxValue = (maxTotalSeated - adultsCount).coerceAtLeast(0),
                onIncrement = {
                    if (totalSeatedPassengers < maxTotalSeated) {
                        childrenCount = (childrenCount + 1).coerceAtMost(8)
                    }
                },
                onDecrement = { childrenCount = (childrenCount - 1).coerceAtLeast(0) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Infants counter
            PassengerCounter(
                label = strings.infants,
                description = "Under 2 years (on lap)",
                count = infantsCount,
                minValue = 0,
                maxValue = maxInfants,
                onIncrement = { infantsCount = (infantsCount + 1).coerceAtMost(maxInfants) },
                onDecrement = { infantsCount = (infantsCount - 1).coerceAtLeast(0) }
            )

            // Info text about infant limit
            if (adultsCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Max ${adultsCount} infant${if (adultsCount > 1) "s" else ""} (1 per adult)",
                    style = typography.duration,
                    color = VelocityColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm button
            Button(
                onClick = {
                    onSelect(PassengerCounts(adultsCount, childrenCount, infantsCount))
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VelocityColors.Accent,
                    contentColor = VelocityColors.BackgroundDeep
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = strings.confirm,
                    style = typography.button
                )
            }
        }
    }
}

@Composable
private fun PassengerCounter(
    label: String,
    description: String,
    count: Int,
    minValue: Int,
    maxValue: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val typography = VelocityTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = typography.body
            )
            Text(
                text = description,
                style = typography.duration
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Decrement button
            CounterButton(
                enabled = count > minValue,
                onClick = onDecrement,
                isIncrement = false
            )

            // Count display
            Text(
                text = count.toString(),
                style = typography.timeBig,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Increment button
            CounterButton(
                enabled = count < maxValue,
                onClick = onIncrement,
                isIncrement = true
            )
        }
    }
}

@Composable
private fun CounterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    isIncrement: Boolean
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) VelocityColors.GlassBg else VelocityColors.GlassBg.copy(alpha = 0.5f),
        onClick = onClick,
        enabled = enabled
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (isIncrement) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (enabled) VelocityColors.Accent else VelocityColors.Disabled,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                // Minus icon (using a simple line since there's no built-in minus)
                Surface(
                    modifier = Modifier.size(16.dp, 2.dp),
                    color = if (enabled) VelocityColors.Accent else VelocityColors.Disabled,
                    shape = RoundedCornerShape(1.dp)
                ) {}
            }
        }
    }
}

/**
 * Displays the passenger selection as a modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerSelectionBottomSheet(
    isVisible: Boolean,
    title: String,
    currentAdults: Int,
    currentChildren: Int,
    currentInfants: Int,
    strings: AppStrings,
    onSelect: (PassengerCounts) -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = VelocityColors.BackgroundDeep,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = VelocityColors.GlassBorder
                ) {}
            }
        ) {
            PassengerSelectionSheet(
                title = title,
                currentAdults = currentAdults,
                currentChildren = currentChildren,
                currentInfants = currentInfants,
                strings = strings,
                onSelect = onSelect,
                onDismiss = onDismiss
            )
        }
    }
}
