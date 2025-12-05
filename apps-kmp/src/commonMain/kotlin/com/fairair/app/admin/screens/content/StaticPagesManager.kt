package com.fairair.app.admin.screens.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.admin.api.*
import com.fairair.app.admin.state.AdminState
import com.fairair.app.ui.theme.FairairColors
import kotlinx.coroutines.launch

/**
 * Static Pages management component with full CRUD operations.
 */
@Composable
fun StaticPagesManager(
    adminState: AdminState,
    adminApiClient: AdminApiClient
) {
    val scope = rememberCoroutineScope()
    val pages by adminState.staticPages.collectAsState()
    val isLoading by adminState.staticPagesLoading.collectAsState()
    val currentAdmin by adminState.currentAdmin.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPage by remember { mutableStateOf<StaticPageDto?>(null) }
    var deleteConfirmPage by remember { mutableStateOf<StaticPageDto?>(null) }

    // Load pages on first render
    LaunchedEffect(Unit) {
        loadPages(adminState, adminApiClient)
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
                    text = "Static Pages",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = FairairColors.Gray900
                )
                Text(
                    text = "Manage content pages like About Us, Aircraft, Media Centre, etc.",
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
                Text("Create Page")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pages List
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
            } else if (pages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = FairairColors.Gray400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No pages yet",
                            fontSize = 16.sp,
                            color = FairairColors.Gray500
                        )
                        Text(
                            text = "Create your first static page",
                            fontSize = 14.sp,
                            color = FairairColors.Gray400
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
                            Text(
                                text = "Title",
                                modifier = Modifier.weight(2f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = FairairColors.Gray700
                            )
                            Text(
                                text = "Slug",
                                modifier = Modifier.weight(1.5f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = FairairColors.Gray700
                            )
                            Text(
                                text = "Status",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = FairairColors.Gray700
                            )
                            Text(
                                text = "Last Updated",
                                modifier = Modifier.weight(1.5f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = FairairColors.Gray700
                            )
                            Text(
                                text = "Actions",
                                modifier = Modifier.width(120.dp),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = FairairColors.Gray700
                            )
                        }
                    }

                    items(pages) { page ->
                        PageRow(
                            page = page,
                            onEdit = { editingPage = page },
                            onDelete = { deleteConfirmPage = page },
                            onTogglePublish = {
                                scope.launch {
                                    if (page.isPublished) {
                                        adminApiClient.unpublishPage(page.id)
                                    } else {
                                        adminApiClient.publishPage(page.id)
                                    }
                                    loadPages(adminState, adminApiClient)
                                }
                            }
                        )
                        HorizontalDivider(color = FairairColors.Gray200)
                    }
                }
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        PageEditDialog(
            page = null,
            adminId = currentAdmin?.id ?: "",
            onDismiss = { showCreateDialog = false },
            onSave = { request ->
                scope.launch {
                    adminApiClient.createPage(request)
                    loadPages(adminState, adminApiClient)
                    showCreateDialog = false
                }
            }
        )
    }

    // Edit Dialog
    editingPage?.let { page ->
        PageEditDialog(
            page = page,
            adminId = currentAdmin?.id ?: "",
            onDismiss = { editingPage = null },
            onSave = { request ->
                scope.launch {
                    adminApiClient.updatePage(page.id, UpdatePageRequest(
                        title = request.title,
                        content = request.content,
                        titleAr = request.titleAr,
                        contentAr = request.contentAr,
                        metaDescription = request.metaDescription,
                        metaDescriptionAr = request.metaDescriptionAr,
                        updatedBy = currentAdmin?.id ?: ""
                    ))
                    loadPages(adminState, adminApiClient)
                    editingPage = null
                }
            }
        )
    }

    // Delete Confirmation
    deleteConfirmPage?.let { page ->
        AlertDialog(
            onDismissRequest = { deleteConfirmPage = null },
            title = { Text("Delete Page") },
            text = { Text("Are you sure you want to delete \"${page.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            adminApiClient.deletePage(page.id)
                            loadPages(adminState, adminApiClient)
                            deleteConfirmPage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PageRow(
    page: StaticPageDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePublish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = page.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FairairColors.Gray900
            )
            page.titleAr?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = FairairColors.Gray600
                )
            }
        }
        
        Text(
            text = page.slug,
            modifier = Modifier.weight(1.5f),
            fontSize = 14.sp,
            color = FairairColors.Gray600
        )
        
        Box(modifier = Modifier.weight(1f)) {
            val statusColor = if (page.isPublished) FairairColors.Success else FairairColors.Warning
            Text(
                text = if (page.isPublished) "Published" else "Draft",
                fontSize = 12.sp,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Text(
            text = formatDate(page.updatedAt),
            modifier = Modifier.weight(1.5f),
            fontSize = 14.sp,
            color = FairairColors.Gray600
        )
        
        Row(modifier = Modifier.width(120.dp)) {
            IconButton(onClick = onTogglePublish) {
                Icon(
                    imageVector = if (page.isPublished) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = if (page.isPublished) "Unpublish" else "Publish",
                    tint = FairairColors.Gray600
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = FairairColors.Purple
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = FairairColors.Error
                )
            }
        }
    }
}

@Composable
private fun PageEditDialog(
    page: StaticPageDto?,
    adminId: String,
    onDismiss: () -> Unit,
    onSave: (CreatePageRequest) -> Unit
) {
    var slug by remember { mutableStateOf(page?.slug ?: "") }
    var title by remember { mutableStateOf(page?.title ?: "") }
    var titleAr by remember { mutableStateOf(page?.titleAr ?: "") }
    var content by remember { mutableStateOf(page?.content ?: "") }
    var contentAr by remember { mutableStateOf(page?.contentAr ?: "") }
    var metaDescription by remember { mutableStateOf(page?.metaDescription ?: "") }
    var metaDescriptionAr by remember { mutableStateOf(page?.metaDescriptionAr ?: "") }
    var showArabic by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 800.dp),
        title = {
            Text(
                text = if (page == null) "Create New Page" else "Edit Page",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Slug (only for new pages)
                if (page == null) {
                    OutlinedTextField(
                        value = slug,
                        onValueChange = { slug = it.lowercase().replace(" ", "-") },
                        label = { Text("Slug (URL path)") },
                        placeholder = { Text("e.g., about-us, aircraft, media-centre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Toggle English/Arabic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilterChip(
                        onClick = { showArabic = false },
                        label = { Text("English") },
                        selected = !showArabic
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        onClick = { showArabic = true },
                        label = { Text("Arabic") },
                        selected = showArabic
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!showArabic) {
                    // English Fields
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content (English)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = metaDescription,
                        onValueChange = { metaDescription = it },
                        label = { Text("Meta Description (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                } else {
                    // Arabic Fields
                    OutlinedTextField(
                        value = titleAr,
                        onValueChange = { titleAr = it },
                        label = { Text("Title (Arabic)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = contentAr,
                        onValueChange = { contentAr = it },
                        label = { Text("Content (Arabic)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = metaDescriptionAr,
                        onValueChange = { metaDescriptionAr = it },
                        label = { Text("Meta Description (Arabic)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(CreatePageRequest(
                        slug = slug,
                        title = title,
                        content = content,
                        titleAr = titleAr.ifBlank { null },
                        contentAr = contentAr.ifBlank { null },
                        metaDescription = metaDescription.ifBlank { null },
                        metaDescriptionAr = metaDescriptionAr.ifBlank { null },
                        createdBy = adminId
                    ))
                },
                enabled = slug.isNotBlank() && title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Purple)
            ) {
                Text(if (page == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private suspend fun loadPages(adminState: AdminState, adminApiClient: AdminApiClient) {
    adminState.setStaticPagesLoading(true)
    when (val result = adminApiClient.getAllPages()) {
        is AdminApiResult.Success -> adminState.setStaticPages(result.data)
        is AdminApiResult.Error -> adminState.setError(result.message)
    }
    adminState.setStaticPagesLoading(false)
}

private fun formatDate(isoDate: String): String {
    // Simple date formatting - in production use kotlinx-datetime
    return isoDate.take(10)
}
