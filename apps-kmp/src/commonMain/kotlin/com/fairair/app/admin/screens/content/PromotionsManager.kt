package com.fairair.app.admin.screens.content

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
 * Promotions management component with full CRUD operations.
 */
@Composable
fun PromotionsManager(
    adminState: AdminState,
    adminApiClient: AdminApiClient
) {
    val scope = rememberCoroutineScope()
    val promotions by adminState.promotions.collectAsState()
    val isLoading by adminState.promotionsLoading.collectAsState()
    val currentAdmin by adminState.currentAdmin.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPromotion by remember { mutableStateOf<PromotionDto?>(null) }
    var deactivateConfirm by remember { mutableStateOf<PromotionDto?>(null) }
    var filterActive by remember { mutableStateOf<Boolean?>(null) } // null = all, true = active, false = inactive

    LaunchedEffect(Unit) {
        loadPromotions(adminState, adminApiClient)
    }

    val filteredPromotions = remember(promotions, filterActive) {
        when (filterActive) {
            true -> promotions.filter { it.isActive }
            false -> promotions.filter { !it.isActive }
            null -> promotions
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Promotions",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = FairairColors.Gray900
                )
                Text(
                    text = "Create and manage promotional campaigns with discount codes.",
                    fontSize = 16.sp,
                    color = FairairColors.Gray600,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FairairColors.Purple,
                    contentColor = FairairColors.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Promotion")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            FilterChip(
                onClick = { filterActive = null },
                label = { Text("All") },
                selected = filterActive == null
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                onClick = { filterActive = true },
                label = { Text("Active") },
                selected = filterActive == true
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                onClick = { filterActive = false },
                label = { Text("Inactive") },
                selected = filterActive == false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Promotions List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FairairColors.White)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FairairColors.Purple)
                }
            } else if (filteredPromotions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = FairairColors.Gray400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No promotions found",
                            fontSize = 16.sp,
                            color = FairairColors.Gray500
                        )
                    }
                }
            } else {
                LazyColumn {
                    // Header Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FairairColors.Gray100)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Code", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Title", Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Discount", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Period", Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Usage", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Status", Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Actions", Modifier.width(100.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                        }
                    }

                    items(filteredPromotions) { promo ->
                        PromotionRow(
                            promotion = promo,
                            onEdit = { editingPromotion = promo },
                            onDeactivate = { deactivateConfirm = promo }
                        )
                        HorizontalDivider(color = FairairColors.Gray200)
                    }
                }
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        PromotionEditDialog(
            promotion = null,
            adminId = currentAdmin?.id ?: "",
            onDismiss = { showCreateDialog = false },
            onSave = { request ->
                scope.launch {
                    adminApiClient.createPromotion(request)
                    loadPromotions(adminState, adminApiClient)
                    showCreateDialog = false
                }
            }
        )
    }

    // Edit Dialog
    editingPromotion?.let { promo ->
        PromotionEditDialog(
            promotion = promo,
            adminId = currentAdmin?.id ?: "",
            onDismiss = { editingPromotion = null },
            onSave = { request ->
                scope.launch {
                    adminApiClient.updatePromotion(promo.id, UpdatePromotionRequest(
                        title = request.title,
                        titleAr = request.titleAr,
                        description = request.description,
                        descriptionAr = request.descriptionAr,
                        discountValue = request.discountValue,
                        minPurchaseAmount = request.minPurchaseAmount,
                        maxDiscountAmount = request.maxDiscountAmount,
                        imageUrl = request.imageUrl,
                        endDate = request.endDate,
                        maxUses = request.maxUses,
                        updatedBy = currentAdmin?.id ?: ""
                    ))
                    loadPromotions(adminState, adminApiClient)
                    editingPromotion = null
                }
            }
        )
    }

    // Deactivate Confirmation
    deactivateConfirm?.let { promo ->
        AlertDialog(
            onDismissRequest = { deactivateConfirm = null },
            title = { Text("Deactivate Promotion") },
            text = { Text("Are you sure you want to deactivate promotion \"${promo.code}\"? Users will no longer be able to use this code.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            adminApiClient.deactivatePromotion(promo.id)
                            loadPromotions(adminState, adminApiClient)
                            deactivateConfirm = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Warning)
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = { deactivateConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PromotionRow(
    promotion: PromotionDto,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = promotion.code,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FairairColors.Purple
        )
        
        Text(
            text = promotion.title,
            modifier = Modifier.weight(1.5f),
            fontSize = 14.sp,
            color = FairairColors.Gray900
        )
        
        Text(
            text = promotion.discountDisplayText,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = FairairColors.Success
        )
        
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = "From: ${formatDate(promotion.startDate)}",
                fontSize = 12.sp,
                color = FairairColors.Gray600
            )
            Text(
                text = "To: ${formatDate(promotion.endDate)}",
                fontSize = 12.sp,
                color = FairairColors.Gray600
            )
        }
        
        Text(
            text = "${promotion.currentUses}${promotion.maxUses?.let { "/$it" } ?: ""}",
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = FairairColors.Gray600
        )
        
        Box(modifier = Modifier.weight(0.8f)) {
            val statusColor = if (promotion.isActive) FairairColors.Success else FairairColors.Gray500
            Text(
                text = if (promotion.isActive) "Active" else "Inactive",
                fontSize = 12.sp,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Row(modifier = Modifier.width(100.dp)) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = FairairColors.Purple
                )
            }
            if (promotion.isActive) {
                IconButton(onClick = onDeactivate) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Deactivate",
                        tint = FairairColors.Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun PromotionEditDialog(
    promotion: PromotionDto?,
    adminId: String,
    onDismiss: () -> Unit,
    onSave: (CreatePromotionRequest) -> Unit
) {
    var code by remember { mutableStateOf(promotion?.code ?: "") }
    var title by remember { mutableStateOf(promotion?.title ?: "") }
    var titleAr by remember { mutableStateOf(promotion?.titleAr ?: "") }
    var description by remember { mutableStateOf(promotion?.description ?: "") }
    var descriptionAr by remember { mutableStateOf(promotion?.descriptionAr ?: "") }
    var discountType by remember { mutableStateOf(promotion?.discountType ?: "PERCENTAGE") }
    var discountValue by remember { mutableStateOf(promotion?.discountValue ?: "") }
    var currency by remember { mutableStateOf(promotion?.currency ?: "SAR") }
    var minPurchaseAmount by remember { mutableStateOf(promotion?.minPurchaseAmount ?: "") }
    var maxDiscountAmount by remember { mutableStateOf(promotion?.maxDiscountAmount ?: "") }
    var originCode by remember { mutableStateOf(promotion?.originCode ?: "") }
    var destinationCode by remember { mutableStateOf(promotion?.destinationCode ?: "") }
    var startDate by remember { mutableStateOf(promotion?.startDate?.take(10) ?: "") }
    var endDate by remember { mutableStateOf(promotion?.endDate?.take(10) ?: "") }
    var maxUses by remember { mutableStateOf(promotion?.maxUses?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 600.dp),
        title = {
            Text(
                text = if (promotion == null) "Create Promotion" else "Edit Promotion",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Code (only for new)
                if (promotion == null) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Promotion Code") },
                        placeholder = { Text("e.g., SUMMER25") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Discount Type & Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Discount Type", fontSize = 12.sp, color = FairairColors.Gray600)
                        Row {
                            FilterChip(
                                onClick = { discountType = "PERCENTAGE" },
                                label = { Text("%") },
                                selected = discountType == "PERCENTAGE"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                onClick = { discountType = "FIXED_AMOUNT" },
                                label = { Text("Fixed") },
                                selected = discountType == "FIXED_AMOUNT"
                            )
                        }
                    }
                    OutlinedTextField(
                        value = discountValue,
                        onValueChange = { discountValue = it },
                        label = { Text(if (discountType == "PERCENTAGE") "Discount %" else "Discount Amount") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Date Range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = promotion == null // Can't change start date after creation
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Optional Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = minPurchaseAmount,
                        onValueChange = { minPurchaseAmount = it },
                        label = { Text("Min Purchase (SAR)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxDiscountAmount,
                        onValueChange = { maxDiscountAmount = it },
                        label = { Text("Max Discount (SAR)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxUses,
                        onValueChange = { maxUses = it },
                        label = { Text("Max Uses") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Route Restrictions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = originCode,
                        onValueChange = { originCode = it.uppercase() },
                        label = { Text("Origin (optional)") },
                        placeholder = { Text("e.g., RUH") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = promotion == null
                    )
                    OutlinedTextField(
                        value = destinationCode,
                        onValueChange = { destinationCode = it.uppercase() },
                        label = { Text("Destination (optional)") },
                        placeholder = { Text("e.g., JED") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = promotion == null
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(CreatePromotionRequest(
                        code = code,
                        title = title,
                        discountType = discountType,
                        discountValue = discountValue,
                        startDate = startDate,
                        endDate = endDate,
                        createdBy = adminId,
                        titleAr = titleAr.ifBlank { null },
                        description = description.ifBlank { null },
                        descriptionAr = descriptionAr.ifBlank { null },
                        currency = currency,
                        minPurchaseAmount = minPurchaseAmount.ifBlank { null },
                        maxDiscountAmount = maxDiscountAmount.ifBlank { null },
                        originCode = originCode.ifBlank { null },
                        destinationCode = destinationCode.ifBlank { null },
                        maxUses = maxUses.toIntOrNull()
                    ))
                },
                enabled = code.isNotBlank() && title.isNotBlank() && 
                         discountValue.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Purple)
            ) {
                Text(if (promotion == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private suspend fun loadPromotions(adminState: AdminState, adminApiClient: AdminApiClient) {
    adminState.setPromotionsLoading(true)
    when (val result = adminApiClient.getAllPromotions()) {
        is AdminApiResult.Success -> adminState.setPromotions(result.data)
        is AdminApiResult.Error -> adminState.setError(result.message)
    }
    adminState.setPromotionsLoading(false)
}

private fun formatDate(isoDate: String): String {
    return isoDate.take(10)
}
