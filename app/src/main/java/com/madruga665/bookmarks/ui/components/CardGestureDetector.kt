package com.madruga665.bookmarks.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Detects discrete taps, long-press gestures with drag-to-select, and cancels cleanly
 * during parent scrolling or movement exceeding touch slop.
 *
 * @param onTap Callback fired on a quick tap without movement exceeding touch slop.
 * @param onLongPressStart Callback fired when the pointer is held stationary beyond the threshold.
 * @param onLongPressDrag Callback fired continuously as the pointer moves while long-press is active.
 * @param onLongPressRelease Callback fired when the pointer is lifted after long-press activation.
 * @param longPressTimeoutMillis Threshold duration to trigger long-press (defaults to 350ms).
 */
suspend fun PointerInputScope.detectTapAndLongPressDrag(
    onTap: () -> Unit,
    onLongPressStart: (Offset) -> Unit,
    onLongPressDrag: (Offset) -> Unit,
    onLongPressRelease: () -> Unit,
    longPressTimeoutMillis: Long = 350L
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pointerId = down.id
        val startPos = down.position
        val touchSlop = viewConfiguration.touchSlop

        var wasTapped = false
        var wasCancelled = false
        var lastChange: PointerInputChange = down

        try {
            withTimeout(longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == pointerId }

                    if (change == null) {
                        wasCancelled = true
                        return@withTimeout
                    }

                    if (!change.pressed) {
                        if (!wasCancelled && !change.isConsumed) {
                            val distance = (change.position - startPos).getDistance()
                            if (distance <= touchSlop) {
                                wasTapped = true
                            }
                        }
                        return@withTimeout
                    }

                    if (change.isConsumed) {
                        wasCancelled = true
                        return@withTimeout
                    }

                    val distance = (change.position - startPos).getDistance()
                    if (distance > touchSlop) {
                        wasCancelled = true
                        return@withTimeout
                    }

                    lastChange = change
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            // Long-press threshold reached without movement or external consumption
        }

        if (wasTapped) {
            onTap()
            return@awaitEachGesture
        }

        if (wasCancelled) {
            return@awaitEachGesture
        }

        // Long-press confirmed!
        lastChange.consume()
        onLongPressStart(lastChange.position)

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == pointerId }

            if (change == null || !change.pressed) {
                onLongPressRelease()
                break
            }

            change.consume()
            onLongPressDrag(change.position)
        }
    }
}
