package com.madruga665.bookmarks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow

@Composable
fun NeobrutalistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    onPasteClick: (() -> Unit)? = null
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.5.dp,
                shadowOffset = 4.dp,
                shape = RoundedCornerShape(14.dp)
            )
            .background(NeobrutalismTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left `#` Prefix Icon
            Text(
                text = "#",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = NeobrutalismTheme.colors.onSurface,
                modifier = Modifier.padding(end = 10.dp)
            )

            // Text Input Field
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholderText,
                        color = NeobrutalismTheme.colors.subtext,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = NeobrutalismTheme.typography.bodyMedium.copy(
                        color = NeobrutalismTheme.colors.onSurface,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(NeobrutalismTheme.colors.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_quick_save_input")
                )
            }

            // Clipboard Paste Icon Button
            if (onPasteClick != null) {
                IconButton(
                    onClick = onPasteClick,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("tag_quick_save_paste_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = stringResource(R.string.paste_from_clipboard),
                        tint = NeobrutalismTheme.colors.onSurface
                    )
                }
            }
        }
    }
}

