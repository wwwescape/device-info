package com.wwwescape.deviceinfo.ui.tv

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * TV (Android TV / Google TV) support. A TV is driven entirely by a D-pad remote: there's no
 * touch, so every piece of content has to be reachable by moving focus, and the focused element
 * has to be obvious from across the room. Everything here is a no-op on phones and tablets.
 */

/** Provided once at the root (see MainActivity) so composables don't each query the system. */
val LocalIsTv = staticCompositionLocalOf { false }

/** How a focused element is drawn — a framed card, or a tinted list row. */
enum class FocusStyle { Card, Row }

/**
 * Draws a clear focus indicator whenever this element (or anything inside it, e.g. a Card's own
 * clickable) holds focus *and* the user is navigating with keys — a TV remote, or a keyboard on
 * a phone/tablet/Chromebook. Touch users never see it.
 *
 * With [makeFocusable], the element itself also becomes focusable — but only on TV, where static
 * content (a stat row, a spec tile) must be focusable for the D-pad to reach it and for the
 * screen to scroll it into view. On touch devices static content stays non-focusable so it
 * doesn't add stops to keyboard/TalkBack traversal.
 */
@Composable
fun Modifier.focusHighlight(
    shape: Shape,
    style: FocusStyle = FocusStyle.Card,
    makeFocusable: Boolean = false,
    focusedScale: Float = 1f,
): Modifier {
    val isTv = LocalIsTv.current
    val keyboardMode = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    var hasFocus by remember { mutableStateOf(false) }
    val showFocus = hasFocus && (keyboardMode || isTv)
    val scale by animateFloatAsState(if (showFocus) focusedScale else 1f, label = "focusScale")
    val frameColor = MaterialTheme.colorScheme.primary
    val tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val density = LocalDensity.current
    val rowBleedPx = with(density) { 8.dp.toPx() }
    val strokePx = with(density) { 3.dp.toPx() }
    val rowStrokePx = with(density) { 2.dp.toPx() }

    val indicator = Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            if (showFocus && style == FocusStyle.Row) {
                // Bleeds slightly past the row's own bounds so the tint frames the text instead of
                // touching it — rows sit inside a card's padding, so there's room for it.
                val topLeft = Offset(-rowBleedPx, 0f)
                val rowSize = Size(size.width + rowBleedPx * 2, size.height)
                val corner = CornerRadius(12.dp.toPx())
                drawRoundRect(color = tintColor, topLeft = topLeft, size = rowSize, cornerRadius = corner)
                drawRoundRect(
                    color = frameColor,
                    topLeft = topLeft,
                    size = rowSize,
                    cornerRadius = corner,
                    style = Stroke(width = rowStrokePx),
                )
            }
            drawContent()
            if (showFocus && style == FocusStyle.Card) {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline, color = frameColor, style = Stroke(width = strokePx))
            }
        }
        .onFocusChanged { hasFocus = it.hasFocus }

    return this.then(indicator).then(if (makeFocusable && isTv) Modifier.focusable() else Modifier)
}

/**
 * Lets a scrolling screen be read with a D-pad. Key presses first move focus to the next
 * focusable element as usual; when there's nothing further to move to (static text below the
 * fold, or a screen with no focusable content at all) Up/Down scroll the content instead, so
 * nothing is ever out of reach. The container itself is focusable on TV so it can take focus
 * when entered from the top bar even if it contains no focusable children.
 */
@Composable
fun Modifier.dpadScrollable(state: ScrollableState, focusOnEnter: Boolean = true): Modifier {
    if (!LocalIsTv.current) return this
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var containerFocused by remember { mutableStateOf(false) }

    // Land on the screen's first focusable item as soon as it opens, so focus is visible
    // immediately instead of only after a first "wasted" key press.
    if (focusOnEnter) {
        LaunchedEffect(Unit) {
            if (runCatching { requester.requestFocus() }.isSuccess) {
                focusManager.moveFocus(FocusDirection.Enter)
            }
        }
    }

    return this
        .onSizeChanged { viewportHeight = it.height }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val forward = when (event.key) {
                Key.DirectionDown -> true
                Key.DirectionUp -> false
                else -> return@onPreviewKeyEvent false
            }
            // Focus on the container itself (just entered the screen): step into its content.
            if (containerFocused && forward && focusManager.moveFocus(FocusDirection.Enter)) {
                return@onPreviewKeyEvent true
            }
            if (!containerFocused && focusManager.moveFocus(if (forward) FocusDirection.Down else FocusDirection.Up)) {
                return@onPreviewKeyEvent true
            }
            val canScroll = if (forward) state.canScrollForward else state.canScrollBackward
            // At the very top, let Up fall through so focus can leave for the top bar.
            if (!canScroll) return@onPreviewKeyEvent false
            val step = viewportHeight * 0.4f
            scope.launch { state.animateScrollBy(if (forward) step else -step) }
            true
        }
        .onFocusChanged { containerFocused = it.isFocused }
        .focusRequester(requester)
        .focusProperties {
            // Moving Up out of the content (to the top bar) only once it's scrolled to the top;
            // until then Up scrolls back through any non-focusable content above (e.g. a hero).
            onExit = {
                if (requestedFocusDirection == FocusDirection.Up && state.canScrollBackward) cancelFocusChange()
            }
        }
        .focusable()
}

/** [verticalScroll] plus [dpadScrollable] — the drop-in for a detail screen's root Column. */
@Composable
fun Modifier.tvVerticalScroll(state: ScrollState = rememberScrollState()): Modifier =
    this.dpadScrollable(state).verticalScroll(state)
