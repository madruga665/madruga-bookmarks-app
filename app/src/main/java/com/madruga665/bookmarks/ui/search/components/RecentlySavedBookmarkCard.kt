package com.madruga665.bookmarks.ui.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.madruga665.bookmarks.ui.components.detectTapAndLongPressDrag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.local.BookmarkEntity
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow
import com.madruga665.bookmarks.ui.utils.BookmarkDisplayUtils

/**
 * Neobrutalist card representing a recently saved bookmark in the discovery carousel.
 * Displays preview thumbnail, overlaid collection pill tag, title, and source platform indicator.
 */
@Composable
fun RecentlySavedBookmarkCard(
    bookmark: BookmarkEntity,
    collectionName: String?,
    collectionColor: String?,
    onClick: () -> Unit,
    onLongPressStart: ((BookmarkEntity, Offset, Offset, IntSize) -> Unit)? = null,
    onLongPressDrag: ((Offset) -> Unit)? = null,
    onLongPressRelease: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var cardWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressDrag by rememberUpdatedState(onLongPressDrag)
    val currentOnLongPressRelease by rememberUpdatedState(onLongPressRelease)

    val gestureModifier = if (onLongPressStart != null) {
        Modifier.pointerInput(bookmark.id) {
            detectTapAndLongPressDrag(
                onTap = { currentOnClick() },
                onLongPressStart = { localOffset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val touchInWindow = cardWindowOffset + localOffset
                    currentOnLongPressStart?.invoke(bookmark, touchInWindow, cardWindowOffset, cardSize)
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
    } else {
        Modifier.clickable(onClick = onClick)
    }

    val displayTitle = BookmarkDisplayUtils.getDisplayTitle(bookmark.title, bookmark.url)
    val displayThumbnail = BookmarkDisplayUtils.getThumbnailUrl(bookmark.thumbnailUrl, bookmark.url)
    val sourceLabel = BookmarkDisplayUtils.getSourceLabel(bookmark.sourcePlatform, bookmark.url)
    val faviconModel = BookmarkDisplayUtils.getFaviconUrl(bookmark.faviconUrl, bookmark.url)
    val badgeColor = BookmarkDisplayUtils.getCollectionAccentColor(collectionColor)
    val displayCollectionName = (collectionName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.save_unsorted)).uppercase()

    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .width(190.dp)
            .testTag("tag_recently_saved_card_${bookmark.id}")
            .onGloballyPositioned { coordinates ->
                cardWindowOffset = coordinates.positionInWindow()
                cardSize = coordinates.size
            }
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.dp,
                shadowOffset = 3.dp,
                shape = cardShape
            )
            .background(
                color = NeobrutalismTheme.colors.surface,
                shape = cardShape
            )
            .clip(cardShape)
            .then(gestureModifier)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Preview Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(NeobrutalismTheme.colors.accentYellow.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (displayThumbnail.isNotBlank()) {
                    AsyncImage(
                        model = displayThumbnail,
                        contentDescription = "Bookmark thumbnail preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .neobrutalistShadow(
                                shadowColor = NeobrutalismTheme.colors.shadow,
                                borderColor = NeobrutalismTheme.colors.border,
                                borderWidth = 2.dp,
                                shadowOffset = 2.dp,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(NeobrutalismTheme.colors.surface, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = "Bookmark preview",
                            tint = NeobrutalismTheme.colors.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Overlaid Collection Pill Badge on bottom-left of thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 8.dp)
                        .neobrutalistShadow(
                            shadowColor = NeobrutalismTheme.colors.shadow,
                            borderColor = NeobrutalismTheme.colors.border,
                            borderWidth = 1.5.dp,
                            shadowOffset = 1.5.dp,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .background(badgeColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = displayCollectionName,
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = NeobrutalismTheme.colors.border
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card Body: Title + Platform Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Title (Bold, max 2 lines)
                Text(
                    text = displayTitle,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp,
                        color = NeobrutalismTheme.colors.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Platform Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!faviconModel.isNullOrBlank()) {
                            AsyncImage(
                                model = faviconModel,
                                contentDescription = "Platform Icon",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = "Platform",
                                tint = NeobrutalismTheme.colors.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = sourceLabel,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeobrutalismTheme.colors.subtext
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
