package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuizPoint
import com.example.data.QuizTopic
import com.example.ui.theme.MarkerCorrectGreen
import com.example.ui.theme.MarkerDefaultYellow
import com.example.ui.theme.MarkerHintBlue
import com.example.ui.theme.MarkerWrongRed
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InteractiveImageBoard(
    quiz: QuizTopic,
    mode: InteractionMode,
    solvedPointIds: Set<String>,
    wrongTappedPointId: String?,
    hintPointId: String?,
    selectedStudyPoint: QuizPoint?,
    showBilingual: Boolean,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onPointClicked: (QuizPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(quiz.imageAsset) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(quiz.imageAsset) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open(quiz.imageAsset).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    var zoom by remember(quiz.id) { mutableFloatStateOf(1f) }
    var panOffset by remember(quiz.id) { mutableStateOf(Offset.Zero) }
    var hideMarkers by remember(quiz.id) { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "markerPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(if (isFullScreen) 0.dp else 16.dp))
            .background(Color(0xFF10181B))
            .pointerInput(quiz.id) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(0.85f, 5.0f)
                    zoom = newZoom
                    val maxPanX = (size.width * (newZoom - 1f) / 2f).coerceAtLeast(140f)
                    val maxPanY = (size.height * (newZoom - 1f) / 2f).coerceAtLeast(140f)
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                    )
                }
            }
            .testTag("interactive_image_board")
    ) {
        val density = LocalDensity.current
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()
        val origW = quiz.originalWidth.coerceAtLeast(1).toFloat()
        val origH = quiz.originalHeight.coerceAtLeast(1).toFloat()

        // Fit-center scale so the entire diagram fits cleanly on screen by default
        val baseScale = minOf(containerW / origW, containerH / origH)
        val fittedW = origW * baseScale
        val fittedH = origH * baseScale

        val fittedWDp = with(density) { fittedW.toDp() }
        val fittedHDp = with(density) { fittedH.toDp() }

        // 1) Scaled Image Layer ONLY — markers are rendered in a separate unscaled overlay
        // so that zooming the map NEVER inflates the marker circles!
        Box(
            modifier = Modifier
                .size(fittedWDp, fittedHDp)
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        ) {
            val loadedBitmap = bitmap
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap.asImageBitmap(),
                    contentDescription = quiz.titleHi,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3EFEA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "चित्र लोड हो रहा है...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.DarkGray
                    )
                }
            }
        }

        // 2) Unscaled Constant-Screen-Size Marker Overlay
        // Screen position of any image point (x, y) after center-pivot scale & pan:
        // screenX = containerW / 2 + (basePosX - fittedW / 2) * zoom + panOffset.x
        // screenY = containerH / 2 + (basePosY - fittedH / 2) * zoom + panOffset.y
        if (!hideMarkers) {
            val touchBoxSizeDp = 28.dp
            val touchBoxSizePx = with(density) { touchBoxSizeDp.toPx() }
            // Compact 13.dp pin circle (like the 15px website dot) so map boundaries and details remain clearly visible
            val compactDotSizeDp = 13.dp

            Box(modifier = Modifier.fillMaxSize()) {
                quiz.points.forEachIndexed { index, point ->
                    val isSolved = solvedPointIds.contains(point.id)
                    val isWrong = wrongTappedPointId == point.id
                    val isHint = hintPointId == point.id
                    val isSelectedStudy = mode != InteractionMode.QUIZ && selectedStudyPoint?.id == point.id
                    val isHighlighted = isWrong || isHint || isSelectedStudy

                    // In learnbyimage.com, createMarker places a 15px circle with style.left = xCord, style.top = yCord
                    // so its center on the original canvas is (point.x + 7.5f, point.y + 7.5f)
                    val basePosX = (point.x + 7.5f) * baseScale
                    val basePosY = (point.y + 7.5f) * baseScale

                    val screenX = containerW / 2f + (basePosX - fittedW / 2f) * zoom + panOffset.x
                    val screenY = containerH / 2f + (basePosY - fittedH / 2f) * zoom + panOffset.y

                    // Only draw markers that are within the visible board bounds
                    if (screenX in -touchBoxSizePx..(containerW + touchBoxSizePx) &&
                        screenY in -touchBoxSizePx..(containerH + touchBoxSizePx)
                    ) {
                        val markerColor = when {
                            isWrong -> MarkerWrongRed
                            isHint -> MarkerHintBlue
                            isSolved -> MarkerCorrectGreen
                            isSelectedStudy -> Color(0xFFD95D14)
                            else -> MarkerDefaultYellow
                        }

                        val activePulse = if (isHighlighted) pulseScale else 1f
                        val currentDotDp = if (isHighlighted) {
                            (16f * activePulse).dp
                        } else {
                            compactDotSizeDp
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (screenX - touchBoxSizePx / 2f).roundToInt(),
                                        y = (screenY - touchBoxSizePx / 2f).roundToInt()
                                    )
                                }
                                .size(touchBoxSizeDp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onPointClicked(point) }
                                .testTag("quiz_marker_${index + 1}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(currentDotDp)
                                    .alpha(if (isHighlighted) 1f else 0.88f)
                                    .shadow(if (isHighlighted) 4.dp else 1.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(markerColor)
                                    .border(
                                        width = if (isHighlighted) 1.5.dp else 1.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSolved && mode == InteractionMode.QUIZ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "सही",
                                        tint = Color.White,
                                        modifier = Modifier.size(9.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = TextStyle(
                                            color = if (markerColor == MarkerDefaultYellow) Color.Black else Color.White,
                                            fontSize = if (index + 1 >= 10) 6.5.sp else 7.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                            }
                        }

                        // Floating Devanagari callout tag when active in Study mode or Hint/Solved
                        val shouldShowCallout = (mode == InteractionMode.STUDY && isSelectedStudy) ||
                            (mode == InteractionMode.QUIZ && isHint)

                        if (shouldShowCallout) {
                            val labelOffsetY = with(density) { 12.dp.toPx() }
                            val boxHalfW = with(density) { 68.dp.toPx() }
                            val maxLeft = (containerW - with(density) { 136.dp.toPx() }).coerceAtLeast(6f)
                            val maxTop = (containerH - with(density) { 42.dp.toPx() }).coerceAtLeast(6f)

                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (screenX - boxHalfW).coerceIn(6f, maxLeft).roundToInt(),
                                            y = (screenY + labelOffsetY).coerceIn(6f, maxTop).roundToInt()
                                        )
                                    }
                                    .widthIn(max = 145.dp)
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelectedStudy) Color(0xFFD95D14)
                                        else Color(0xEE0A3640)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "#${index + 1} ${point.labelHi}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 12.sp
                                    )
                                    if (showBilingual && mode == InteractionMode.STUDY) {
                                        Text(
                                            text = point.labelEn,
                                            color = Color.White.copy(alpha = 0.88f),
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top-Right Floating Controls: Hide/Show Pins (Eye) + Full-Screen Toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            // Eye button: temporarily hide/show pins to inspect fine map details
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { hideMarkers = !hideMarkers }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("toggle_markers_button")
                ) {
                    Icon(
                        imageVector = if (hideMarkers) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (hideMarkers) "बिंदु दिखाएं" else "बिंदु छुपाएं",
                        tint = if (hideMarkers) Color(0xFFFFD54F) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = if (hideMarkers) "बिंदु दिखाएं" else "साफ़ नक्शा",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Full-Screen Map Toggle Button
            FilledTonalIconButton(
                onClick = onToggleFullScreen,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isFullScreen) Color(0xFFD95D14) else Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(36.dp)
                    .testTag("fullscreen_map_button")
            ) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullScreen) "फुल स्क्रीन से बाहर निकलें" else "फुल स्क्रीन मैप",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom-Right Floating Zoom Controls Overlay
        Surface(
            color = Color.Black.copy(alpha = 0.68f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        zoom = (zoom - 0.35f).coerceAtLeast(0.85f)
                        if (zoom <= 1f) panOffset = Offset.Zero
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "ज़ूम कम करें",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                FilledTonalIconButton(
                    onClick = {
                        zoom = (zoom + 0.35f).coerceAtMost(5.0f)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "ज़ूम बढ़ाएं",
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (zoom != 1f || panOffset != Offset.Zero) {
                    Spacer(modifier = Modifier.size(2.dp))
                    FilledTonalIconButton(
                        onClick = {
                            zoom = 1f
                            panOffset = Offset.Zero
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("zoom_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "रीसेट ज़ूम",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
