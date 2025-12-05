package com.fairair.app.b2b.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.b2b.api.*
import com.fairair.app.b2b.state.B2BState

/**
 * B2B Reports Screen.
 * Displays agency performance metrics, booking statistics, and commission reports.
 */
@Composable
fun B2BReportsScreen(
    b2bState: B2BState,
    accentColor: Color
) {
    val agencyStats by b2bState.agencyStats.collectAsState()
    val isLoadingStats by b2bState.isLoadingStats.collectAsState()
    val commissionReport by b2bState.commissionReport.collectAsState()

    var selectedPeriod by remember { mutableStateOf("THIS_MONTH") }

    // Load stats on first render
    LaunchedEffect(Unit) {
        b2bState.loadAgencyStats()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reports & Analytics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Track your agency's performance",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Period selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("THIS_WEEK", "THIS_MONTH", "THIS_YEAR").forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { 
                                Text(
                                    when (period) {
                                        "THIS_WEEK" -> "Week"
                                        "THIS_MONTH" -> "Month"
                                        "THIS_YEAR" -> "Year"
                                        else -> period
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Loading
        if (isLoadingStats) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        }

        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatSummaryCard(
                    title = "Total Bookings",
                    value = agencyStats?.totalBookings?.toString() ?: "0",
                    icon = Icons.Default.Star,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                StatSummaryCard(
                    title = "Confirmed",
                    value = agencyStats?.confirmedBookings?.toString() ?: "0",
                    icon = Icons.Default.Check,
                    color = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f)
                )
                StatSummaryCard(
                    title = "Cancelled",
                    value = agencyStats?.cancelledBookings?.toString() ?: "0",
                    icon = Icons.Default.Close,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Revenue & Commission
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RevenueCard(
                    title = "Total Revenue",
                    amount = agencyStats?.totalRevenue ?: 0,
                    currency = agencyStats?.currency ?: "SAR",
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )
                RevenueCard(
                    title = "Commission Earned",
                    amount = agencyStats?.totalCommission ?: 0,
                    currency = agencyStats?.currency ?: "SAR",
                    color = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Routes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Top Routes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (agencyStats?.topRoutes?.isEmpty() != false) {
                        Text(
                            text = "No booking data available",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    } else {
                        agencyStats?.topRoutes?.take(5)?.forEachIndexed { index, route ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(
                                                accentColor.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${route.origin} → ${route.destination}",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${route.bookingCount} bookings",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Text(
                                    text = route.revenue.formatPrice(agencyStats?.currency ?: "SAR"),
                                    color = Color(0xFF22C55E),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (index < (agencyStats?.topRoutes?.size ?: 0) - 1) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }

        // Commission Report Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Commission Summary",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        TextButton(
                            onClick = { b2bState.loadCommissionReport() }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", color = accentColor, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CommissionItem(
                            label = "Earned",
                            amount = commissionReport?.earnedCommission ?: 0,
                            currency = commissionReport?.currency ?: "SAR",
                            color = Color(0xFF22C55E),
                            modifier = Modifier.weight(1f)
                        )
                        CommissionItem(
                            label = "Paid",
                            amount = commissionReport?.paidCommission ?: 0,
                            currency = commissionReport?.currency ?: "SAR",
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        CommissionItem(
                            label = "Pending",
                            amount = commissionReport?.pendingCommission ?: 0,
                            currency = commissionReport?.currency ?: "SAR",
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun RevenueCard(
    title: String,
    amount: Long,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount.formatPrice(currency),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun CommissionItem(
    label: String,
    amount: Long,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = amount.formatPrice(currency),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
