package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InstrumentRuleEntity
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InstrumentEditorSheet(
    initialRule: InstrumentRuleEntity,
    onSave: (InstrumentRuleEntity) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var bankName by remember { mutableStateOf(initialRule.bankName) }
    var cardOrAccountName by remember { mutableStateOf(initialRule.cardOrAccountName) }
    var last4 by remember { mutableStateOf(initialRule.last4) }
    val markerList = remember { mutableStateListOf<String>().apply { addAll(initialRule.getMarkerList()) } }
    var newMarkerText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun addCurrentMarker() {
        val trimmed = newMarkerText.trim()
        if (trimmed.isNotBlank()) {
            if (!markerList.any { it.equals(trimmed, ignoreCase = true) }) {
                markerList.add(trimmed)
            }
            newMarkerText = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GlassCardBg,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("instrument_editor_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SavioEmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = SavioEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (initialRule.id != 0L) "Edit Instrument" else "Configure Instrument",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                    Text(
                        text = "Customize bank, card, and regex markers for transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SavioSlateMuted)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bank Name
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it; errorMessage = null },
                label = { Text("Bank / Issuer Name") },
                placeholder = { Text("e.g. HDFC Bank, SBI, ICICI") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("instrument_bank_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavioEmerald,
                    unfocusedBorderColor = GlassCardBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Card or Account Name
            OutlinedTextField(
                value = cardOrAccountName,
                onValueChange = { cardOrAccountName = it; errorMessage = null },
                label = { Text("Card / Account Name") },
                placeholder = { Text("e.g. Millennia Card, Regalia, Salary Account") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("instrument_card_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavioEmerald,
                    unfocusedBorderColor = GlassCardBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Last 4 Digits
            OutlinedTextField(
                value = last4,
                onValueChange = {
                    if (it.length <= 6) {
                        last4 = it.uppercase(Locale.US)
                        errorMessage = null
                    }
                },
                label = { Text("Last 4 Digits / Suffix") },
                placeholder = { Text("e.g. 4821 or XXXX") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("instrument_last4_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavioEmerald,
                    unfocusedBorderColor = GlassCardBorder
                ),
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    Text(
                        text = "ℹ️ Instruments with the same last 4 digits are automatically clubbed together.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SavioSlateMuted
                    )
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Markers Section Header
            Text(
                text = "Regex Markers (Matching Rules)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SavioSlateDark
            )
            Text(
                text = "Specify regex patterns or unique text found in SMSs from this card/account (e.g. (?i)millennia, VM-HDFCBK, ending\\s+4821).",
                style = MaterialTheme.typography.bodySmall,
                color = SavioSlateMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Markers Chip List
            if (markerList.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GlassBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No custom regex markers added yet. Add one below.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = SavioSlateMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    markerList.forEachIndexed { index, marker ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SavioEmeraldContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = marker,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = SavioEmerald
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { markerList.removeAt(index) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove marker",
                                        tint = SavioEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add Marker Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMarkerText,
                    onValueChange = { newMarkerText = it },
                    placeholder = { Text("New regex marker (e.g. (?i)hdfc.*millennia)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("instrument_new_marker_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SavioEmerald,
                        unfocusedBorderColor = GlassCardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addCurrentMarker() })
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { addCurrentMarker() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald),
                    modifier = Modifier.testTag("instrument_add_marker_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (initialRule.id != 0L && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(initialRule.id)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("instrument_delete_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val cleanBank = bankName.trim()
                        val cleanCard = cardOrAccountName.trim()
                        val cleanLast4 = last4.trim().uppercase(Locale.US)

                        if (cleanBank.isBlank() && cleanCard.isBlank() && cleanLast4.isBlank() && markerList.isEmpty()) {
                            errorMessage = "Please provide at least a bank name, card name, last 4 digits, or marker."
                            return@Button
                        }

                        // Auto-add any typed text in the new marker field before saving
                        if (newMarkerText.trim().isNotBlank()) {
                            markerList.add(newMarkerText.trim())
                        }

                        val updatedRule = initialRule.copy(
                            bankName = cleanBank,
                            cardOrAccountName = cleanCard,
                            last4 = cleanLast4,
                            markers = InstrumentRuleEntity.markersToString(markerList)
                        )
                        onSave(updatedRule)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("instrument_save_button")
                ) {
                    Text(
                        text = if (initialRule.id != 0L) "Update Instrument" else "Save Instrument",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
