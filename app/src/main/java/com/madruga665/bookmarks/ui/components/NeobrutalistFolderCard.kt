package com.madruga665.bookmarks.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.local.CollectionEntity
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow
import com.madruga665.bookmarks.ui.utils.CollectionIconRegistry
import com.madruga665.bookmarks.ui.utils.CollectionPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeobrutalistFolderCard(
    collection: CollectionEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isActiveMenu: Boolean = false,
    touchPositionInWindow: Offset? = null,
    onHoveredOptionChange: (CollectionOption?) -> Unit = {},
    onLongPressStart: ((CollectionEntity, Offset, Offset, IntSize) -> Unit)? = null,
    onLongPressDrag: ((Offset) -> Unit)? = null,
    onLongPressRelease: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var cardWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    val cardRotation by animateFloatAsState(
        targetValue = if (isActiveMenu) -3.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardRotation"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isActiveMenu) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    val tabColor = CollectionPalette.getColor(collection.colorAccent)
    val iconVector: ImageVector = CollectionIconRegistry.getIcon(collection.iconKey)

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressDrag by rememberUpdatedState(onLongPressDrag)
    val currentOnLongPressRelease by rememberUpdatedState(onLongPressRelease)

    val gestureModifier = if (onLongPressStart != null) {
        Modifier.pointerInput(collection.id) {
            detectTapAndLongPressDrag(
                onTap = { currentOnClick() },
                onLongPressStart = { localOffset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val touchInWindow = cardWindowOffset + localOffset
                    currentOnLongPressStart?.invoke(collection, touchInWindow, cardWindowOffset, cardSize)
                },
                onLongPressDrag = { localOffset ->
                    val touchInWindow = cardWindowOffset + localOffset
                    currentOnLongPressDrag?.invoke(touchInWindow)
                },
                onLongPressRelease = {
                    currentOnLongPressRelease?.invoke()
                }
            )
        }
    } else if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                cardWindowOffset = coordinates.positionInWindow()
                cardSize = coordinates.size
            }
            .graphicsLayer(
                rotationZ = cardRotation,
                scaleX = cardScale,
                scaleY = cardScale
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(gestureModifier)
        ) {
            // Colored Top Tab Header
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(18.dp)
                    .neobrutalistShadow(
                        shadowColor = NeobrutalismTheme.colors.shadow,
                        borderColor = NeobrutalismTheme.colors.border,
                        borderWidth = 2.5.dp,
                        shadowOffset = 2.dp,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
                    .background(tabColor, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )

            // Main Folder Card Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neobrutalistShadow(
                        shadowColor = NeobrutalismTheme.colors.shadow,
                        borderColor = NeobrutalismTheme.colors.border,
                        borderWidth = 2.5.dp,
                        shadowOffset = 4.dp,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
                    .background(NeobrutalismTheme.colors.surface, RoundedCornerShape(topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Internal Colored Icon Box
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .neobrutalistShadow(
                                    shadowColor = NeobrutalismTheme.colors.shadow,
                                    borderColor = NeobrutalismTheme.colors.border,
                                    borderWidth = 2.dp,
                                    shadowOffset = 2.dp,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(tabColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = collection.name,
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Link Count Subtext
                        Text(
                            text = stringResource(R.string.collection_links_count, collection.linkCount),
                            fontSize = 12.sp,
                            color = NeobrutalismTheme.colors.subtext,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Collection Title
                    Text(
                        text = collection.name,
                        style = NeobrutalismTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = NeobrutalismTheme.colors.onSurface
                    )
                }
            }
        }
    }
}
