package com.fairair.app.b2b.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.b2b.api.*
import com.fairair.app.b2b.state.B2BState
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * B2B Settings Screen.
 * Agency profile management and settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun B2BSettingsScreen(
    b2bState: B2BState,
    accentColor: Color
) {
    val agencyProfile by b2bState.agencyProfile.collectAsState()
    val agencyUsers by b2bState.agencyUsers.collectAsState()
    val isLoadingProfile by b2bState.isLoadingProfile.collectAsState()
    val currentAgency by b2bState.currentAgency.collectAsState()
    val currentUser by b2bState.currentUser.collectAsState()

    var showEditProfile by remember { mutableStateOf(false) }
    var showInviteUser by remember { mutableStateOf(false) }

    // Load profile on first render
    LaunchedEffect(Unit) {
        b2bState.loadAgencyProfile()
        b2bState.loadAgencyUsers()
    }

    // Edit profile dialog
    if (showEditProfile && agencyProfile != null) {
        EditProfileDialog(
            profile = agencyProfile!!,
            accentColor = accentColor,
            onDismiss = { showEditProfile = false },
            onSave = { request ->
                b2bState.updateAgencyProfile(request) {
                    showEditProfile = false
                }
            }
        )
    }

    // Invite user dialog
    if (showInviteUser) {
        InviteUserDialog(
            accentColor = accentColor,
            onDismiss = { showInviteUser = false },
            onInvite = { request ->
                b2bState.inviteUser(request) {
                    showInviteUser = false
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Loading
        if (isLoadingProfile) {
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

        // Agency Profile Section
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = accentColor
                            )
                            Text(
                                text = "Agency Profile",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showEditProfile = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileField("Agency Name", agencyProfile?.name ?: currentAgency?.name ?: "-")
                    ProfileField("IATA Code", agencyProfile?.iataCode ?: currentAgency?.iataCode ?: "-")
                    ProfileField("Business License", agencyProfile?.businessLicenseNumber ?: "-")
                    ProfileField("Contact Person", agencyProfile?.contactPersonName ?: "-")
                    ProfileField("Email", agencyProfile?.email ?: "-")
                    ProfileField("Phone", agencyProfile?.phone ?: "-")
                    ProfileField("Address", "${agencyProfile?.address ?: "-"}, ${agencyProfile?.city ?: ""}")
                    ProfileField("Country", agencyProfile?.country ?: "-")
                    ProfileField("Status", agencyProfile?.status?.toDisplayStatus() ?: currentAgency?.status?.toDisplayStatus() ?: "-")
                }
            }
        }

        // Credit & Commission Section
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF22C55E)
                        )
                        Text(
                            text = "Credit & Commission",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CreditCard(
                            title = "Credit Limit",
                            amount = agencyProfile?.creditLimit ?: currentAgency?.creditLimit ?: 0,
                            currency = agencyProfile?.currency ?: currentAgency?.currency ?: "SAR",
                            color = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                        CreditCard(
                            title = "Available Credit",
                            amount = agencyProfile?.availableCredit ?: currentAgency?.availableCredit ?: 0,
                            currency = agencyProfile?.currency ?: currentAgency?.currency ?: "SAR",
                            color = Color(0xFF22C55E),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileField(
                        "Commission Rate", 
                        "${((agencyProfile?.commissionRate ?: currentAgency?.commissionRate ?: 0.0) * 100).toInt()}%"
                    )
                }
            }
        }

        // Users Section
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6)
                            )
                            Text(
                                text = "Team Members",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (currentUser?.role == "ADMIN") {
                            Button(
                                onClick = { showInviteUser = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invite", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (agencyUsers.isEmpty()) {
                        Text(
                            text = "No team members",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    } else {
                        agencyUsers.forEach { user ->
                            UserCard(
                                user = user,
                                isCurrentUser = user.id == currentUser?.id,
                                accentColor = accentColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Current User Info
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Text(
                            text = "Logged in as",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileField("Name", "${currentUser?.firstName ?: ""} ${currentUser?.lastName ?: ""}")
                    ProfileField("Email", currentUser?.email ?: "-")
                    ProfileField("Role", currentUser?.role ?: "-")
                    ProfileField("Last Login", currentUser?.lastLoginAt?.take(10) ?: "-")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CreditCard(
    title: String,
    amount: Long,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = amount.formatPrice(currency),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun UserCard(
    user: AgencyUserDto,
    isCurrentUser: Boolean,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentUser) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${user.firstName.firstOrNull() ?: ""}${user.lastName.firstOrNull() ?: ""}",
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = user.role,
                fontSize = 12.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            if (!user.isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inactive",
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: AgencyProfileDto,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (UpdateAgencyProfileRequest) -> Unit
) {
    var contactPerson by remember { mutableStateOf(profile.contactPersonName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var address by remember { mutableStateOf(profile.address) }
    var city by remember { mutableStateOf(profile.city) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        UpdateAgencyProfileRequest(
                            contactPersonName = contactPerson,
                            phone = phone,
                            address = address,
                            city = city
                        )
                    )
                },
                enabled = contactPerson.isNotBlank() && phone.isNotBlank() && address.isNotBlank() && city.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun InviteUserDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onInvite: (InviteUserRequest) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("AGENT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Team Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ADMIN", "AGENT", "VIEWER").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onInvite(
                        InviteUserRequest(
                            email = email,
                            firstName = firstName,
                            lastName = lastName,
                            role = role
                        )
                    )
                },
                enabled = email.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
