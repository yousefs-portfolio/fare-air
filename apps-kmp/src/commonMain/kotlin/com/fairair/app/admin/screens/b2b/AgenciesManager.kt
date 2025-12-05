package com.fairair.app.admin.screens.b2b

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.admin.api.*
import com.fairair.app.admin.state.AdminState
import com.fairair.app.ui.theme.FairairColors
import kotlinx.coroutines.launch

/**
 * Agencies management component for B2B portal.
 */
@Composable
fun AgenciesManager(
    adminState: AdminState,
    adminApiClient: AdminApiClient
) {
    val scope = rememberCoroutineScope()
    val agencies by adminState.agencies.collectAsState()
    val pendingAgencies by adminState.pendingAgencies.collectAsState()
    val isLoading by adminState.agenciesLoading.collectAsState()
    val currentAdmin by adminState.currentAdmin.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Pending
    var selectedAgency by remember { mutableStateOf<AgencyDto?>(null) }
    var approveDialogAgency by remember { mutableStateOf<AgencyDto?>(null) }
    var rejectDialogAgency by remember { mutableStateOf<AgencyDto?>(null) }

    LaunchedEffect(Unit) {
        loadAgencies(adminState, adminApiClient)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Agencies",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = FairairColors.Gray900
        )
        Text(
            text = "Review and manage B2B agency applications and accounts.",
            fontSize = 16.sp,
            color = FairairColors.Gray600,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = FairairColors.White,
            contentColor = FairairColors.Purple
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("All Agencies (${agencies.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pending Approval")
                        if (pendingAgencies.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = FairairColors.Warning,
                                contentColor = FairairColors.White
                            ) {
                                Text(pendingAgencies.size.toString())
                            }
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FairairColors.White)
        ) {
            val displayedAgencies = if (selectedTab == 0) agencies else pendingAgencies

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FairairColors.Purple)
                }
            } else if (displayedAgencies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = FairairColors.Gray400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 1) "No pending agencies" else "No agencies registered",
                            fontSize = 16.sp,
                            color = FairairColors.Gray500
                        )
                    }
                }
            } else {
                LazyColumn {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FairairColors.Gray100)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Code", Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Name", Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Type", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Contact", Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Status", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Actions", Modifier.width(150.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                        }
                    }

                    items(displayedAgencies) { agency ->
                        AgencyRow(
                            agency = agency,
                            onView = { selectedAgency = agency },
                            onApprove = { approveDialogAgency = agency },
                            onReject = { rejectDialogAgency = agency },
                            onSuspend = {
                                scope.launch {
                                    adminApiClient.suspendAgency(agency.id, "Suspended by admin")
                                    loadAgencies(adminState, adminApiClient)
                                }
                            }
                        )
                        HorizontalDivider(color = FairairColors.Gray200)
                    }
                }
            }
        }
    }

    // View Agency Details Dialog
    selectedAgency?.let { agency ->
        AgencyDetailsDialog(
            agency = agency,
            onDismiss = { selectedAgency = null }
        )
    }

    // Approve Dialog
    approveDialogAgency?.let { agency ->
        ApproveAgencyDialog(
            agency = agency,
            adminId = currentAdmin?.id ?: "",
            onDismiss = { approveDialogAgency = null },
            onApprove = { request ->
                scope.launch {
                    adminApiClient.approveAgency(agency.id, request)
                    loadAgencies(adminState, adminApiClient)
                    approveDialogAgency = null
                }
            }
        )
    }

    // Reject Dialog
    rejectDialogAgency?.let { agency ->
        var notes by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { rejectDialogAgency = null },
            title = { Text("Reject Agency Application") },
            text = {
                Column {
                    Text("Are you sure you want to reject \"${agency.name}\"?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Reason for rejection") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            adminApiClient.rejectAgency(agency.id, notes.ifBlank { null })
                            loadAgencies(adminState, adminApiClient)
                            rejectDialogAgency = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Error)
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectDialogAgency = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AgencyRow(
    agency: AgencyDto,
    onView: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSuspend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = agency.agencyCode,
            modifier = Modifier.weight(0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FairairColors.Purple
        )
        
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = agency.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FairairColors.Gray900
            )
            agency.city?.let {
                Text(
                    text = "$it, ${agency.country ?: ""}",
                    fontSize = 12.sp,
                    color = FairairColors.Gray600
                )
            }
        }
        
        Text(
            text = agency.typeDisplayName,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = FairairColors.Gray700
        )
        
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = agency.contactName,
                fontSize = 14.sp,
                color = FairairColors.Gray900
            )
            Text(
                text = agency.contactEmail,
                fontSize = 12.sp,
                color = FairairColors.Gray600
            )
        }
        
        Box(modifier = Modifier.weight(1f)) {
            val (statusColor, statusBg) = when (agency.status) {
                "APPROVED" -> FairairColors.Success to FairairColors.Success.copy(alpha = 0.1f)
                "PENDING" -> FairairColors.Warning to FairairColors.Warning.copy(alpha = 0.1f)
                "SUSPENDED" -> FairairColors.Error to FairairColors.Error.copy(alpha = 0.1f)
                "REJECTED" -> FairairColors.Gray600 to FairairColors.Gray200
                else -> FairairColors.Gray600 to FairairColors.Gray200
            }
            Text(
                text = agency.statusDisplayName,
                fontSize = 12.sp,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Row(modifier = Modifier.width(150.dp)) {
            IconButton(onClick = onView) {
                Icon(Icons.Default.Info, "View", tint = FairairColors.Gray600)
            }
            
            when (agency.status) {
                "PENDING" -> {
                    IconButton(onClick = onApprove) {
                        Icon(Icons.Default.Check, "Approve", tint = FairairColors.Success)
                    }
                    IconButton(onClick = onReject) {
                        Icon(Icons.Default.Close, "Reject", tint = FairairColors.Error)
                    }
                }
                "APPROVED" -> {
                    IconButton(onClick = onSuspend) {
                        Icon(Icons.Default.Clear, "Suspend", tint = FairairColors.Warning)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun AgencyDetailsDialog(
    agency: AgencyDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 500.dp),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = FairairColors.Purple)
                Spacer(modifier = Modifier.width(12.dp))
                Text(agency.name, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DetailRow("Agency Code", agency.agencyCode)
                DetailRow("Type", agency.typeDisplayName)
                DetailRow("Status", agency.statusDisplayName)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Contact Information", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                Spacer(modifier = Modifier.height(8.dp))
                
                DetailRow("Name", agency.contactName)
                DetailRow("Email", agency.contactEmail)
                agency.contactPhone?.let { DetailRow("Phone", it) }
                
                agency.address?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Address", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = FairairColors.Gray700)
                    agency.city?.let { city ->
                        Text("$city, ${agency.country ?: ""}", color = FairairColors.Gray600)
                    }
                }
                
                if (agency.status == "APPROVED") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Business Terms", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Commission Rate", "${agency.commissionRate ?: "0"}%")
                    DetailRow("Credit Limit", "${agency.creditLimit ?: "0"} SAR")
                    DetailRow("Current Balance", "${agency.currentBalance ?: "0"} SAR")
                }
                
                agency.taxId?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Legal Information", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Tax ID", it)
                    agency.licenseNumber?.let { lic -> DetailRow("License Number", lic) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ApproveAgencyDialog(
    agency: AgencyDto,
    adminId: String,
    onDismiss: () -> Unit,
    onApprove: (ApproveAgencyRequest) -> Unit
) {
    var commissionRate by remember { mutableStateOf("5.0") }
    var creditLimit by remember { mutableStateOf("10000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve Agency", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Approve \"${agency.name}\" and set their business terms:")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = commissionRate,
                    onValueChange = { commissionRate = it },
                    label = { Text("Commission Rate (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it },
                    label = { Text("Credit Limit (SAR)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApprove(ApproveAgencyRequest(
                        approvedBy = adminId,
                        commissionRate = commissionRate,
                        creditLimit = creditLimit
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Success)
            ) {
                Text("Approve")
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
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(120.dp),
            fontSize = 13.sp,
            color = FairairColors.Gray600
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = FairairColors.Gray900
        )
    }
}

private suspend fun loadAgencies(adminState: AdminState, adminApiClient: AdminApiClient) {
    adminState.setAgenciesLoading(true)
    
    when (val result = adminApiClient.getAllAgencies()) {
        is AdminApiResult.Success -> adminState.setAgencies(result.data)
        is AdminApiResult.Error -> adminState.setError(result.message)
    }
    
    when (val result = adminApiClient.getPendingAgencies()) {
        is AdminApiResult.Success -> adminState.setPendingAgencies(result.data)
        is AdminApiResult.Error -> {} // Ignore, we already have all agencies
    }
    
    adminState.setAgenciesLoading(false)
}
