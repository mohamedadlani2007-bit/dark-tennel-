package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionState
import com.example.ui.MainViewModel
import com.example.ui.theme.DarkTunnelCardBg
import com.example.ui.theme.DarkTunnelCardBorder
import com.example.ui.theme.DarkTunnelDivider
import com.example.ui.theme.DarkTunnelPurple
import com.example.ui.theme.DarkTunnelTextDim
import com.example.ui.theme.DarkTunnelTextLight
import com.example.ui.theme.DarkVoid
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPrepareVpn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val connectionState by viewModel.connectionState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val targetConfig by viewModel.targetConfig.collectAsState()
    val proxyConfig by viewModel.proxyConfig.collectAsState()
    val payloadConfigText by viewModel.payloadConfigText.collectAsState()
    val tunnelModeSubtitle by viewModel.tunnelModeSubtitle.collectAsState()
    val snackMessage by viewModel.snackMessage.collectAsState()
    val activeConfigFile by viewModel.activeConfigFile.collectAsState()
    val isConfigLocked by viewModel.isConfigLocked.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showEditTargetDialog by remember { mutableStateOf(false) }
    var showEditProxyDialog by remember { mutableStateOf(false) }
    var showEditPayloadDialog by remember { mutableStateOf(false) }

    var showSaveConfigDialog by remember { mutableStateOf(false) }
    var saveConfigName by remember { mutableStateOf("Dark_Config") }
    var saveConfigNote by remember { mutableStateOf("") }
    var saveConfigLocked by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val name = if (saveConfigName.isNotBlank()) saveConfigName else "Dark_Config"
            viewModel.saveConfigToUri(context, uri, name, saveConfigNote, saveConfigLocked)
            showSaveConfigDialog = false
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromUri(context, uri)
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll logs to bottom whenever new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
    ) {
        // Subtle cyber background texture
        Image(
            painter = painterResource(id = R.drawable.img_cyber_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.08f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Top Bar (Exact layout from user screenshot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left action icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = Color(0xFFC7C7CC),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkTunnelCardBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("استيراد من الحافظة (Paste)", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = DarkTunnelPurple)
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.importFromClipboard(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حفظ وتصدير كونفيغ (.dark)", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF06B6D4))
                                },
                                onClick = {
                                    showMenu = false
                                    showSaveConfigDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("فتح ملف كونفيغ من الهاتف", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF10B981))
                                },
                                onClick = {
                                    showMenu = false
                                    openDocumentLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            HorizontalDivider(color = DarkTunnelDivider)
                            DropdownMenuItem(
                                text = { Text("استعادة الإعدادات الافتراضية", color = DarkTunnelTextDim) },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkTunnelTextDim)
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.resetToScreenshotDefaults()
                                }
                            )
                        }
                    }

                    // Open / Import config button
                    IconButton(
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.testTag("open_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Open Config",
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Save / Export config button
                    IconButton(
                        onClick = { showSaveConfigDialog = true },
                        modifier = Modifier.testTag("save_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Config",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Clear logs button
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.testTag("trash_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = Color(0xFFC7C7CC),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // App Title
                Text(
                    text = "DarkTunnel",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (activeConfigFile != null) {
                // Active Loaded Config File Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isConfigLocked) Color(0xFF241530) else Color(0xFF152A20))
                        .border(1.dp, if (isConfigLocked) Color(0xFF8B5CF6) else Color(0xFF10B981), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isConfigLocked) Color(0xFF8B5CF6) else Color(0xFF10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isConfigLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${activeConfigFile?.name}.dark",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isConfigLocked) "🔒 ملف محمي ومغلق بواسطة الصانع" else "🔓 ملف مفتوح وقابل للتعديل",
                                        fontSize = 11.sp,
                                        color = if (isConfigLocked) Color(0xFFA78BFA) else Color(0xFF6EE7B7)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.clearActiveConfig() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إزالة الكونفيغ",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (!activeConfigFile?.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x2E000000))
                                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "💬 رسالة ووصف الصانع:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD166)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeConfigFile?.description ?: "",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Quick Action Bar: Save & Open Config Files
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showSaveConfigDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E38))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ كونفيغ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E38))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فتح ملف .dark", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DarkTunnel Main Configuration Card (Screenshot layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkTunnelCardBg)
                    .border(1.dp, DarkTunnelCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Card Subtitle Header
                    Text(
                        text = tunnelModeSubtitle,
                        color = DarkTunnelTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        textAlign = TextAlign.End
                    )

                    // 1. Target section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isConfigLocked) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("🔒 هذا الملف مغلق ومحمي بواسطة الصانع ولا يمكن تعديل السيرفر!")
                                    }
                                } else {
                                    showEditTargetDialog = true
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "target",
                            color = DarkTunnelTextDim,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = targetConfig,
                            color = DarkTunnelTextLight,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 0.6.dp,
                        color = DarkTunnelDivider
                    )

                    // 2. Proxy section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isConfigLocked) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("🔒 هذا الملف مغلق ومحمي بواسطة الصانع ولا يمكن تعديل البروكسي!")
                                    }
                                } else {
                                    showEditProxyDialog = true
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "proxy",
                            color = DarkTunnelTextDim,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = proxyConfig,
                            color = DarkTunnelTextLight,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 0.6.dp,
                        color = DarkTunnelDivider
                    )

                    // 3. Payload section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isConfigLocked) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("🔒 هذا الملف مغلق ومحمي بواسطة الصانع ولا يمكن تعديل البايلود!")
                                    }
                                } else {
                                    showEditPayloadDialog = true
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "payload",
                            color = DarkTunnelTextDim,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = payloadConfigText,
                            color = Color(0xFFA5A5AC),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 17.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Action Button (Purple CONNECT / DISCONNECT button)
            val isConnected = connectionState.isOnline
            val isConnecting = connectionState.isProgress

            Button(
                onClick = {
                    viewModel.toggleConnection(context, onPrepareVpn)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("tunnel_action_button"),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkTunnelPurple
                )
            ) {
                Text(
                    text = when {
                        isConnected -> "DISCONNECT"
                        isConnecting -> "CONNECTING..."
                        else -> "CONNECT"
                    },
                    color = Color(0xFF100B1B),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Terminal Log Console (Below button, exact screenshot layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF000000))
                    .border(0.6.dp, Color(0xFF1C1C20), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No logs available. Tap CONNECT to start.",
                            color = DarkTunnelTextDim,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = entry.message,
                                        color = Color(0xFFEDEDED),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "[${entry.formattedTime}]",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 0.5.dp,
                                    color = Color(0xFF222226)
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Dialog: Edit Target
    if (showEditTargetDialog) {
        var tempTarget by remember { mutableStateOf(targetConfig) }
        AlertDialog(
            onDismissRequest = { showEditTargetDialog = false },
            title = { Text("تعديل السيرفر الهدف (Target)", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "صيغة: host:port@user:pass أو domain.com",
                        color = DarkTunnelTextDim,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempTarget,
                        onValueChange = { tempTarget = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DarkTunnelPurple,
                            unfocusedBorderColor = DarkTunnelCardBorder
                        ),
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateTargetConfig(tempTarget.trim())
                        showEditTargetDialog = false
                    }
                ) {
                    Text("حفظ", color = DarkTunnelPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTargetDialog = false }) {
                    Text("إلغاء", color = DarkTunnelTextDim)
                }
            },
            containerColor = DarkTunnelCardBg
        )
    }

    // Dialog: Edit Proxy
    if (showEditProxyDialog) {
        var tempProxy by remember { mutableStateOf(proxyConfig) }
        AlertDialog(
            onDismissRequest = { showEditProxyDialog = false },
            title = { Text("تعديل البروكسي (Proxy)", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "صيغة IP:Port (مثال: 34.43.46.91:80 أو 104.16.1.1:80)",
                        color = DarkTunnelTextDim,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempProxy,
                        onValueChange = { tempProxy = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DarkTunnelPurple,
                            unfocusedBorderColor = DarkTunnelCardBorder
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateProxyConfig(tempProxy.trim())
                        showEditProxyDialog = false
                    }
                ) {
                    Text("حفظ", color = DarkTunnelPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProxyDialog = false }) {
                    Text("إلغاء", color = DarkTunnelTextDim)
                }
            },
            containerColor = DarkTunnelCardBg
        )
    }

    // Dialog: Edit Payload
    if (showEditPayloadDialog) {
        var tempPayload by remember { mutableStateOf(payloadConfigText) }
        AlertDialog(
            onDismissRequest = { showEditPayloadDialog = false },
            title = { Text("تعديل الـ Payload", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "نص حقن HTTP Connect / GET",
                        color = DarkTunnelTextDim,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempPayload,
                        onValueChange = { tempPayload = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DarkTunnelPurple,
                            unfocusedBorderColor = DarkTunnelCardBorder
                        ),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updatePayloadConfigText(tempPayload.trim())
                        showEditPayloadDialog = false
                    }
                ) {
                    Text("حفظ", color = DarkTunnelPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPayloadDialog = false }) {
                    Text("إلغاء", color = DarkTunnelTextDim)
                }
            },
            containerColor = DarkTunnelCardBg
        )
    }

    // Dialog: Save and Export Config (.dark)
    if (showSaveConfigDialog) {
        var tempName by remember { mutableStateOf(saveConfigName) }
        var tempNote by remember { mutableStateOf(saveConfigNote) }
        var tempLocked by remember { mutableStateOf(saveConfigLocked) }

        AlertDialog(
            onDismissRequest = { showSaveConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (tempLocked) "🔒" else "💾", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ وتصدير ملف الكونفيغ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "اسم الملف (مثال: Djezzy_VIP):",
                        color = DarkTunnelTextDim,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Djezzy_VIP", color = Color(0xFF55555C)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DarkTunnelPurple,
                            unfocusedBorderColor = DarkTunnelCardBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lock Config Switch Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (tempLocked) Color(0xFF261533) else Color(0xFF1B1B22))
                            .border(1.dp, if (tempLocked) Color(0xFF8B5CF6) else Color(0xFF2E2E38), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (tempLocked) "🔒" else "🔓", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "قفل وتشفير الملف (Lock)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (tempLocked) Color(0xFFA78BFA) else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (tempLocked)
                                        "مشفر: يتم إخفاء وتشفير السيرفر، البروكسي والبايلود ولا يستطيع أحد رؤيتها أو سرقتها!"
                                    else
                                        "مفتوح: يمكن لأي شخص رؤية الإعدادات وتعديلها بحرية.",
                                    fontSize = 10.sp,
                                    color = if (tempLocked) Color(0xFFDDD6FE) else DarkTunnelTextDim,
                                    lineHeight = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = tempLocked,
                                onCheckedChange = { tempLocked = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DarkTunnelPurple
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "رسالة الصانع أو وصف للمستخدمين (Creator Note):",
                        color = DarkTunnelTextDim,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempNote,
                        onValueChange = { tempNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "مثال: سيرفر جيزي مجاني سريع جداً للألعاب واليوتيوب.. اشتركوا في قناتنا!",
                                color = Color(0xFF55555C),
                                fontSize = 11.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DarkTunnelPurple,
                            unfocusedBorderColor = DarkTunnelCardBorder
                        ),
                        minLines = 3,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            saveConfigName = tempName.ifBlank { "Dark_Config" }
                            saveConfigNote = tempNote
                            saveConfigLocked = tempLocked
                            viewModel.shareConfigFile(context, saveConfigName, saveConfigNote, saveConfigLocked)
                            showSaveConfigDialog = false
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF06B6D4))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة", color = Color(0xFF06B6D4))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            saveConfigName = tempName.ifBlank { "Dark_Config" }
                            saveConfigNote = tempNote
                            saveConfigLocked = tempLocked
                            val fileName = if (saveConfigName.endsWith(".dark")) saveConfigName else "$saveConfigName.dark"
                            createDocumentLauncher.launch(fileName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTunnelPurple)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ بالهاتف", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfigDialog = false }) {
                    Text("إلغاء", color = DarkTunnelTextDim)
                }
            },
            containerColor = DarkTunnelCardBg
        )
    }
}
