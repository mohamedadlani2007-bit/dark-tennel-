package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InjectionMethod
import com.example.ui.MainViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkVoid
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun PayloadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val payloadConfig by viewModel.payloadConfig.collectAsState()
    val bugHostInput by viewModel.bugHostInput.collectAsState()
    val bugHostChecking by viewModel.bugHostChecking.collectAsState()
    val bugHostResult by viewModel.bugHostResult.collectAsState()

    val scrollState = rememberScrollState()

    val injectionModes = listOf(
        InjectionMethod.NORMAL,
        InjectionMethod.FRONT,
        InjectionMethod.BACK,
        InjectionMethod.WEBSOCKET
    )

    val methods = listOf("CONNECT", "GET", "POST", "HEAD")

    val presets = listOf(
        "wap.mobilis.dz" to "Mobilis DZ",
        "internet.djezzy.dz" to "Djezzy DZ",
        "m.youtube.com" to "YouTube",
        "cdnjs.cloudflare.com" to "Cloudflare",
        "speedtest.net" to "Speedtest"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkVoid)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyanPrimary.copy(alpha = 0.2f))
                    .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Payload",
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "PAYLOAD & SNI STUDIO",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "صانع الـ Payload وفاحص الـ Bug Host",
                    fontSize = 12.sp,
                    color = CyanPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: Bug Host Checker & SNI Inspector
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = DarkCardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUG HOST & SNI CHECKER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "HTTP/HTTPS Status",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Presets chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(3).forEach { (host, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .clickable {
                                    viewModel.updateBugHostInput(host)
                                    viewModel.updatePayloadBugHost(host)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = TextCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = bugHostInput,
                        onValueChange = {
                            viewModel.updateBugHostInput(it)
                            viewModel.updatePayloadBugHost(it)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bug_host_input"),
                        placeholder = { Text("e.g. wap.mobilis.dz", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkVoid,
                            unfocusedContainerColor = DarkVoid
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.checkBugHost() },
                        enabled = !bugHostChecking,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("check_bug_host_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple,
                            contentColor = TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (bugHostChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TextWhite
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = "Check",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Check Result
                AnimatedVisibility(visible = bugHostResult != null) {
                    val res = bugHostResult ?: return@AnimatedVisibility
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (res.isWorkingBugHost) NeonGreen.copy(alpha = 0.12f) else NeonRed.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    if (res.isWorkingBugHost) NeonGreen.copy(alpha = 0.4f) else NeonRed.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Status: ${res.statusCode} (${res.statusMessage})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (res.isWorkingBugHost) NeonGreen else NeonRed,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (res.isWorkingBugHost) NeonGreen else NeonRed)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (res.isWorkingBugHost) "VALID BUG HOST" else "BLOCKED / FAILED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DarkVoid,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Apply as SNI to Server >",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanPrimary,
                                        modifier = Modifier.clickable {
                                            viewModel.applyBugHostToCurrentServer(res.host)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Payload Generator Settings
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = DarkCardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PAYLOAD GENERATOR SETTINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Injection Mode selector
                Text(
                    text = "Injection Method",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    injectionModes.forEach { mode ->
                        val isSelected = payloadConfig.injectionMethod == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                .border(1.dp, if (isSelected) CyanPrimary else DarkCardBorder, RoundedCornerShape(6.dp))
                                .clickable { viewModel.updatePayloadInjectionMethod(mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.title.split(" ").first(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CyanPrimary else TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Request Method selector
                Text(
                    text = "Request Method",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { method ->
                        val isSelected = payloadConfig.requestMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonPurple.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                .border(1.dp, if (isSelected) NeonPurple else DarkCardBorder, RoundedCornerShape(6.dp))
                                .clickable { viewModel.updatePayloadRequestMethod(method) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeonPurple else TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Online Host (X-Online-Host)",
                        fontSize = 12.sp,
                        color = TextWhite
                    )
                    Switch(
                        checked = payloadConfig.enableOnlineHost,
                        onCheckedChange = { viewModel.updatePayloadToggleOnlineHost(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keep-Alive Connection",
                        fontSize = 12.sp,
                        color = TextWhite
                    )
                    Switch(
                        checked = payloadConfig.enableKeepAlive,
                        onCheckedChange = { viewModel.updatePayloadToggleKeepAlive(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Live Generated Payload Preview
        val generatedPayload = payloadConfig.generate()

        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = DarkCardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GENERATED PAYLOAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Row {
                        IconButton(
                            onClick = {
                                viewModel.copyToClipboard(context, generatedPayload, "Payload")
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkVoid)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = generatedPayload,
                        color = TerminalCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.applyPayloadToCurrentServer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_payload_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = DarkVoid
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Apply",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "APPLY PAYLOAD TO CURRENT SERVER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
