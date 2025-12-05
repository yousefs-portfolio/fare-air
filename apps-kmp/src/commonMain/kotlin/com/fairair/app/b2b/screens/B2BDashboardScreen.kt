package com.fairair.app.b2b.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.b2b.api.formatPrice
import com.fairair.app.b2b.state.B2BState
import org.koin.compose.koinInject

/**
 * B2B Agency Dashboard Screen.
 * Provides navigation to all agency portal features with overview stats.
 */
class B2BDashboardScreen : Screen {

    @Composable
    override fun Content() {
        val b2bState = koinInject<B2BState>()
        val navigator = LocalNavigator.currentOrThrow

        val isLoggedIn by b2bState.isLoggedIn.collectAsState()

        // Redirect to login if not authenticated
        LaunchedEffect(isLoggedIn) {
            if (!isLoggedIn) {
                navigator.replaceAll(B2BLoginScreen())
            }
        }

        // Load initial data
        LaunchedEffect(Unit) {
            b2bState.loadAgencyBookings()
            b2bState.loadGroupBookingRequests()
            b2bState.loadCharterRequests()
            b2bState.loadAgencyProfile()
        }

        if (isLoggedIn) {
            B2BDashboardContent(
                b2bState = b2bState,
                onLogout = {
                    b2bState.logout()
                    navigator.replaceAll(B2BLoginScreen())
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun B2BDashboardContent(
    b2bState: B2BState,
    onLogout: () -> Unit
) {
    val currentUser by b2bState.currentUser.collectAsState()
    val currentAgency by b2bState.currentAgency.collectAsState()
    val agencyBookings by b2bState.agencyBookings.collectAsState()
    val groupRequests by b2bState.groupBookingRequests.collectAsState()
    val charterRequests by b2bState.charterRequests.collectAsState()

    var selectedSection by remember { mutableStateOf(DashboardSection.OVERVIEW) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // B2B brand colors
    val primaryColor = Color(0xFF1E40AF)
    val accentColor = Color(0xFF06B6D4)
    val backgroundColor = Color(0xFF0F172A)
    val surfaceColor = Color(0xFF1E293B)

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Logout", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar Navigation
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .background(backgroundColor),
            containerColor = backgroundColor,
            contentColor = Color.White
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FA",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation items
            DashboardSection.entries.forEach { section ->
                NavigationRailItem(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    icon = {
                        Icon(
                            section.icon,
                            contentDescription = section.title,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(section.title, fontSize = 10.sp) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = accentColor.copy(alpha = 0.2f)
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout button
            IconButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Logout",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // Main content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(surfaceColor, backgroundColor)
                    )
                )
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedSection.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    currentAgency?.let { agency ->
                        Text(
                            text = agency.name,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // User info and credit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Available credit
                    currentAgency?.let { agency ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF22C55E).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Available Credit",
                                    fontSize = 10.sp,
                                    color = Color(0xFF22C55E)
                                )
                                Text(
                                    text = agency.availableCredit.formatPrice(agency.currency),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22C55E)
                                )
                            }
                        }
                    }

                    // User avatar and name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentUser?.let { user ->
                            Text(
                                text = "${user.firstName} ${user.lastName}",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${user.firstName.firstOrNull() ?: ""}${user.lastName.firstOrNull() ?: ""}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Content based on selected section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                when (selectedSection) {
                    DashboardSection.OVERVIEW -> OverviewContent(
                        b2bState = b2bState,
                        accentColor = accentColor,
                        onNavigate = { selectedSection = it }
                    )
                    DashboardSection.SEARCH -> FlightSearchContent(b2bState = b2bState, accentColor = accentColor)
                    DashboardSection.BOOKINGS -> BookingsContent(b2bState = b2bState, accentColor = accentColor)
                    DashboardSection.GROUPS -> GroupBookingsContent(b2bState = b2bState, accentColor = accentColor)
                    DashboardSection.CHARTERS -> CharterRequestsContent(b2bState = b2bState, accentColor = accentColor)
                    DashboardSection.REPORTS -> ReportsContent(b2bState = b2bState, accentColor = accentColor)
                    DashboardSection.SETTINGS -> SettingsContent(b2bState = b2bState, accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
private fun OverviewContent(
    b2bState: B2BState,
    accentColor: Color,
    onNavigate: (DashboardSection) -> Unit
) {
    val bookings by b2bState.agencyBookings.collectAsState()
    val groupRequests by b2bState.groupBookingRequests.collectAsState()
    val charterRequests by b2bState.charterRequests.collectAsState()
    val agency by b2bState.currentAgency.collectAsState()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Quick stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickStatCard(
                    title = "Total Bookings",
                    value = "${bookings.size}",
                    icon = Icons.Default.Star,
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Pending Groups",
                    value = "${groupRequests.count { it.status == "PENDING" || it.status == "QUOTED" }}",
                    icon = Icons.Default.Person,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Charter Requests",
                    value = "${charterRequests.size}",
                    icon = Icons.Default.Star,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                agency?.let {
                    QuickStatCard(
                        title = "Commission Rate",
                        value = "${(it.commissionRate * 100).toInt()}%",
                        icon = Icons.Default.Star,
                        color = Color(0xFF22C55E),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick actions
        item {
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionCard(
                    title = "Search Flights",
                    description = "Find and book flights for your clients",
                    icon = Icons.Default.Search,
                    color = accentColor,
                    onClick = { onNavigate(DashboardSection.SEARCH) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Group Booking",
                    description = "Request quotes for 10+ passengers",
                    icon = Icons.Default.Person,
                    color = Color(0xFFF59E0B),
                    onClick = { onNavigate(DashboardSection.GROUPS) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Charter Flight",
                    description = "Request a private charter flight",
                    icon = Icons.Default.Star,
                    color = Color(0xFF8B5CF6),
                    onClick = { onNavigate(DashboardSection.CHARTERS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent bookings
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Bookings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                TextButton(onClick = { onNavigate(DashboardSection.BOOKINGS) }) {
                    Text("View All", color = accentColor)
                }
            }
        }

        if (bookings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No bookings yet",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onNavigate(DashboardSection.SEARCH) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Search Flights")
                            }
                        }
                    }
                }
            }
        } else {
            items(bookings.take(5)) { booking ->
                RecentBookingCard(booking = booking, accentColor = accentColor)
            }
        }

        // Pending group quotes
        val pendingQuotes = groupRequests.filter { it.status == "QUOTED" }
        if (pendingQuotes.isNotEmpty()) {
            item {
                Text(
                    text = "Quotes Awaiting Response",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(pendingQuotes.take(3)) { quote ->
                PendingQuoteCard(
                    request = quote,
                    onAccept = { b2bState.acceptGroupQuote(quote.id) {} },
                    onReject = { b2bState.rejectGroupQuote(quote.id) {} }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    icon: ImageVector,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun RecentBookingCard(
    booking: com.fairair.app.b2b.api.BookingSummaryDto,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                val statusColor = when (booking.status) {
                    "CONFIRMED", "TICKETED" -> Color(0xFF22C55E)
                    "PENDING" -> Color(0xFFF59E0B)
                    "CANCELLED" -> Color(0xFFEF4444)
                    else -> Color.Gray
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )

                Column {
                    Text(
                        text = booking.pnr,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = booking.passengerNames,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = booking.route,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = booking.departureDate,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Text(
                text = booking.totalAmount.formatPrice(booking.currency),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PendingQuoteCard(
    request: com.fairair.app.b2b.api.GroupBookingRequestDto,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
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
                Column {
                    Text(
                        text = "${request.originName} → ${request.destinationName}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${request.passengerCount} passengers • ${request.departureDate}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                request.quotedPrice?.let { price ->
                    Text(
                        text = price.formatPrice(request.quoteCurrency ?: "SAR"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                request.quoteValidUntil?.let {
                    Text(
                        text = "Valid until: $it",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Reject")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E)
                    )
                ) {
                    Text("Accept Quote")
                }
            }
        }
    }
}

// Placeholder content components for other sections
@Composable
private fun FlightSearchContent(b2bState: B2BState, accentColor: Color) {
    B2BFlightSearch(b2bState = b2bState, accentColor = accentColor)
}

@Composable
private fun BookingsContent(b2bState: B2BState, accentColor: Color) {
    B2BBookingsManager(b2bState = b2bState, accentColor = accentColor)
}

@Composable
private fun GroupBookingsContent(b2bState: B2BState, accentColor: Color) {
    B2BGroupBookingsScreen(b2bState = b2bState, accentColor = accentColor)
}

@Composable
private fun CharterRequestsContent(b2bState: B2BState, accentColor: Color) {
    B2BCharterRequestsScreen(b2bState = b2bState, accentColor = accentColor)
}

@Composable
private fun ReportsContent(b2bState: B2BState, accentColor: Color) {
    B2BReportsScreen(b2bState = b2bState, accentColor = accentColor)
}

@Composable
private fun SettingsContent(b2bState: B2BState, accentColor: Color) {
    B2BSettingsScreen(b2bState = b2bState, accentColor = accentColor)
}

/**
 * Dashboard navigation sections.
 */
enum class DashboardSection(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    BOOKINGS("Bookings", Icons.Default.Star),
    GROUPS("Groups", Icons.Default.Person),
    CHARTERS("Charters", Icons.Default.Star),
    REPORTS("Reports", Icons.Default.Star),
    SETTINGS("Settings", Icons.Default.Settings)
}
