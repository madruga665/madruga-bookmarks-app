package com.madruga665.bookmarks.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class CardGestureDetectorTest {

    private class FakeViewConfiguration(
        override val touchSlop: Float = 18f,
        override val longPressTimeoutMillis: Long = 350L,
        override val doubleTapTimeoutMillis: Long = 300L,
        override val doubleTapMinTimeMillis: Long = 40L,
        override val minimumTouchTargetSize: DpSize = DpSize.Zero
    ) : ViewConfiguration

    private class FakePointerInputScope(
        private val events: List<PointerEvent>,
        override val viewConfiguration: ViewConfiguration = FakeViewConfiguration()
    ) : PointerInputScope {
        override val density: Float = 1f
        override val fontScale: Float = 1f
        override val size: IntSize = IntSize(100, 100)
        override val extendedTouchPadding: Size = Size.Zero

        override suspend fun <R> awaitPointerEventScope(block: suspend AwaitPointerEventScope.() -> R): R {
            val scope = FakeAwaitPointerEventScope(events, viewConfiguration, size)
            return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val coroutine = block.createCoroutineUnintercepted(scope, Continuation(EmptyCoroutineContext) { result ->
                    continuation.resumeWith(result)
                })
                coroutine.resume(Unit)
            }
        }
    }

    private class FakeAwaitPointerEventScope(
        private val events: List<PointerEvent>,
        override val viewConfiguration: ViewConfiguration,
        override val size: IntSize
    ) : AwaitPointerEventScope {
        override val density: Float = 1f
        override val fontScale: Float = 1f
        override val extendedTouchPadding: Size = Size.Zero
        override val currentEvent: PointerEvent get() = events.getOrNull(eventIndex.coerceAtLeast(0)) ?: events.first()

        private var eventIndex = 0

        override suspend fun awaitPointerEvent(pass: PointerEventPass): PointerEvent {
            if (eventIndex < events.size) {
                val event = events[eventIndex]
                eventIndex++
                return event
            }
            kotlinx.coroutines.awaitCancellation()
        }

        override suspend fun <T> withTimeout(timeMillis: Long, block: suspend AwaitPointerEventScope.() -> T): T {
            return block()
        }

        override suspend fun <T> withTimeoutOrNull(timeMillis: Long, block: suspend AwaitPointerEventScope.() -> T): T? {
            return block()
        }
    }

    private fun createChange(
        id: Long = 1L,
        position: Offset,
        pressed: Boolean,
        previousPosition: Offset = position,
        previousPressed: Boolean = !pressed,
        isConsumed: Boolean = false
    ): PointerInputChange {
        return PointerInputChange(
            id = PointerId(id),
            uptimeMillis = 0L,
            position = position,
            pressed = pressed,
            pressure = 1.0f,
            previousUptimeMillis = 0L,
            previousPosition = previousPosition,
            previousPressed = previousPressed,
            isInitiallyConsumed = isConsumed,
            type = PointerType.Touch
        )
    }

    @Test
    fun tapGesture_triggersOnTap() = runTest {
        val down = createChange(position = Offset(10f, 10f), pressed = true, previousPressed = false)
        val up = createChange(position = Offset(10f, 10f), pressed = false, previousPressed = true)

        val scope = FakePointerInputScope(
            listOf(
                PointerEvent(listOf(down)),
                PointerEvent(listOf(up))
            )
        )

        var tapped = false
        var longPressStarted = false

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            scope.detectTapAndLongPressDrag(
                onTap = { tapped = true },
                onLongPressStart = { longPressStarted = true },
                onLongPressDrag = {},
                onLongPressRelease = {}
            )
        }

        assertTrue(tapped)
        assertFalse(longPressStarted)
        job.cancel()
    }

    @Test
    fun scrollDrag_exceedingTouchSlop_cancelsGestureWithoutTapOrLongPress() = runTest {
        val down = createChange(position = Offset(10f, 10f), pressed = true, previousPressed = false)
        // Moved by 30px (exceeding touchSlop of 18px)
        val move = createChange(position = Offset(10f, 40f), pressed = true, previousPressed = true)
        val up = createChange(position = Offset(10f, 40f), pressed = false, previousPressed = true)

        val scope = FakePointerInputScope(
            listOf(
                PointerEvent(listOf(down)),
                PointerEvent(listOf(move)),
                PointerEvent(listOf(up))
            )
        )

        var tapped = false
        var longPressStarted = false

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            scope.detectTapAndLongPressDrag(
                onTap = { tapped = true },
                onLongPressStart = { longPressStarted = true },
                onLongPressDrag = {},
                onLongPressRelease = {}
            )
        }

        assertFalse("Tapped should not be called on scroll", tapped)
        assertFalse("Long press should not be called on scroll", longPressStarted)
        job.cancel()
    }

    @Test
    fun consumedEvent_cancelsGestureWithoutTapOrLongPress() = runTest {
        val down = createChange(position = Offset(10f, 10f), pressed = true, previousPressed = false)
        // Consumed by parent scroll container
        val moveConsumed = createChange(position = Offset(10f, 15f), pressed = true, previousPressed = true, isConsumed = true)
        val up = createChange(position = Offset(10f, 15f), pressed = false, previousPressed = true)

        val scope = FakePointerInputScope(
            listOf(
                PointerEvent(listOf(down)),
                PointerEvent(listOf(moveConsumed)),
                PointerEvent(listOf(up))
            )
        )

        var tapped = false
        var longPressStarted = false

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            scope.detectTapAndLongPressDrag(
                onTap = { tapped = true },
                onLongPressStart = { longPressStarted = true },
                onLongPressDrag = {},
                onLongPressRelease = {}
            )
        }

        assertFalse("Tapped should not be called when consumed by scroll", tapped)
        assertFalse("Long press should not be called when consumed by scroll", longPressStarted)
        job.cancel()
    }

    @Test
    fun longPress_triggersStartDragAndRelease() = runTest {
        val down = createChange(position = Offset(10f, 10f), pressed = true, previousPressed = false)
        val dragMove = createChange(position = Offset(50f, 60f), pressed = true, previousPressed = true)
        val up = createChange(position = Offset(50f, 60f), pressed = false, previousPressed = true)

        val timeoutScope = object : PointerInputScope {
            override val density: Float = 1f
            override val fontScale: Float = 1f
            override val size: IntSize = IntSize(100, 100)
            override val extendedTouchPadding: Size = Size.Zero
            override val viewConfiguration: ViewConfiguration = FakeViewConfiguration()

            override suspend fun <R> awaitPointerEventScope(block: suspend AwaitPointerEventScope.() -> R): R {
                val scope = object : AwaitPointerEventScope {
                    override val density: Float = 1f
                    override val fontScale: Float = 1f
                    override val extendedTouchPadding: Size = Size.Zero
                    override val size: IntSize = IntSize(100, 100)
                    override val viewConfiguration: ViewConfiguration = FakeViewConfiguration()
                    override val currentEvent: PointerEvent = PointerEvent(listOf(down))

                    private val eventsList = listOf(
                        PointerEvent(listOf(down)),
                        PointerEvent(listOf(dragMove)),
                        PointerEvent(listOf(up))
                    )
                    private var index = 0

                    override suspend fun awaitPointerEvent(pass: PointerEventPass): PointerEvent {
                        if (index < eventsList.size) {
                            val ev = eventsList[index]
                            index++
                            return ev
                        }
                        kotlinx.coroutines.awaitCancellation()
                    }

                    override suspend fun <T> withTimeout(timeMillis: Long, block: suspend AwaitPointerEventScope.() -> T): T {
                        // Simulate timeout exception
                        throw androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException(timeMillis)
                    }

                    override suspend fun <T> withTimeoutOrNull(timeMillis: Long, block: suspend AwaitPointerEventScope.() -> T): T? {
                        return null
                    }
                }
                return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                    val coroutine = block.createCoroutineUnintercepted(scope, Continuation(EmptyCoroutineContext) { result ->
                        continuation.resumeWith(result)
                    })
                    coroutine.resume(Unit)
                }
            }
        }

        var tapped = false
        var longPressStarted = false
        var dragOffset: Offset? = null
        var released = false

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            timeoutScope.detectTapAndLongPressDrag(
                onTap = { tapped = true },
                onLongPressStart = { longPressStarted = true },
                onLongPressDrag = { dragOffset = it },
                onLongPressRelease = { released = true }
            )
        }

        assertFalse(tapped)
        assertTrue(longPressStarted)
        assertEquals(Offset(50f, 60f), dragOffset)
        assertTrue(released)
        job.cancel()
    }
}
