package com.madruga665.bookmarks.ui.settings.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.ui.components.NeobrutalistButton
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow

private val ErrorRedColor = Color(0xFFFF4B4B)

@Composable
fun PairVerificationDialog(
    isVisible: Boolean,
    targetDeviceName: String,
    verificationCode: String,
    onCodeChange: (String) -> Unit,
    onConfirmPair: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isPairing: Boolean = false,
    errorMessage: String? = null
) {
    if (!isVisible) return

    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = {
            if (!isPairing) onDismiss()
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .testTag("tag_pair_dialog")
                .neobrutalistShadow(
                    shadowColor = NeobrutalismTheme.colors.shadow,
                    borderColor = NeobrutalismTheme.colors.border,
                    borderWidth = 2.5.dp,
                    shadowOffset = 6.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(NeobrutalismTheme.colors.surface, RoundedCornerShape(16.dp))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dialog Title
                Text(
                    text = stringResource(R.string.sync_verification_code_title),
                    style = NeobrutalismTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    ),
                    color = NeobrutalismTheme.colors.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Target Device & Instructions
                if (targetDeviceName.isNotBlank()) {
                    Text(
                        text = targetDeviceName,
                        style = NeobrutalismTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = NeobrutalismTheme.colors.accentPurple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = stringResource(R.string.sync_pair_dialog_description),
                    style = NeobrutalismTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = NeobrutalismTheme.colors.subtext
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 6-digit PIN Display and Invisible Input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Hidden input capturing keystrokes
                    BasicTextField(
                        value = verificationCode,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isLetterOrDigit() }.take(6).uppercase()
                            onCodeChange(filtered)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = if (verificationCode.length == 6) ImeAction.Done else ImeAction.Default
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (verificationCode.length == 6 && !isPairing) {
                                    onConfirmPair()
                                }
                            }
                        ),
                        cursorBrush = SolidColor(Color.Transparent),
                        singleLine = true,
                        modifier = Modifier
                            .size(1.dp)
                            .focusRequester(focusRequester)
                            .testTag("tag_pair_code_input")
                    )

                    // Visual 6-Digit Cells
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val char = verificationCode.getOrNull(i)?.toString() ?: ""
                            val isFocused = verificationCode.length == i

                            Box(
                                modifier = Modifier
                                    .size(width = 42.dp, height = 50.dp)
                                    .neobrutalistShadow(
                                        shadowColor = if (isFocused) NeobrutalismTheme.colors.shadow else NeobrutalismTheme.colors.shadow.copy(alpha = 0.5f),
                                        borderColor = if (isFocused) NeobrutalismTheme.colors.accentPurple else NeobrutalismTheme.colors.border,
                                        borderWidth = if (isFocused) 2.5.dp else 2.dp,
                                        shadowOffset = if (isFocused) 3.dp else 2.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        if (isFocused) NeobrutalismTheme.colors.surface else NeobrutalismTheme.colors.background,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    style = NeobrutalismTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 20.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = NeobrutalismTheme.colors.onSurface
                                )
                            }
                        }
                    }
                }

                // Error Message if any
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = NeobrutalismTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = ErrorRedColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalistButton(
                        text = stringResource(R.string.dialog_cancel),
                        onClick = onDismiss,
                        enabled = !isPairing,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("tag_pair_cancel_btn"),
                        containerColor = NeobrutalismTheme.colors.surface,
                        shape = RoundedCornerShape(8.dp),
                        borderWidth = 2.dp,
                        shadowOffset = 2.dp
                    )

                    NeobrutalistButton(
                        onClick = onConfirmPair,
                        enabled = verificationCode.length == 6 && !isPairing,
                        containerColor = NeobrutalismTheme.colors.accentYellow,
                        shape = RoundedCornerShape(8.dp),
                        borderWidth = 2.dp,
                        shadowOffset = 2.dp,
                        modifier = Modifier.testTag("tag_pair_confirm_btn")
                    ) {
                        if (isPairing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeobrutalismTheme.colors.border,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sync_pair_confirm),
                                style = NeobrutalismTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = NeobrutalismTheme.colors.border
                            )
                        }
                    }
                }
            }
        }
    }
}
