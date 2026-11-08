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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
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

    val pulseTransition = rememberInfiniteTransition(label = "markerPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF10181B))
            .pointerInput(quiz.id) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(0.85f, 3.5f)
                    zoom = newZoom
                    val maxPanX = (size.width * (newZoom - 1f) / 2f).coerceAtLeast(120f)
                    val maxPanY = (size.height * (newZoom - 1f) / 2f).coerceAtLeast(120f)
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

        // Fit-center scale so the entire diagram and all markers fit cleanly on screen by default
        val baseScale = minOf(containerW / origW, containerH / origH)
        val fittedW = origW * baseScale
        val fittedH = origH * baseScale

        val fittedWDp = with(density) { fittedW.toDp() }
        val fittedHDp = with(density) { fittedH.toDp() }

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

            // Render interactive coordinate pins
            quiz.points.forEachIndexed { index, point ->
                val isSolved = solvedPointIds.contains(point.id)
                val isWrong = wrongTappedPointId == point.id
                val isHint = hintPointId == point.id
                val isSelectedStudy = mode != InteractionMode.QUIZ && selectedStudyPoint?.id == point.id

                // In learnbyimage.com, createMarker places a 15px circle with style.left = xCord, style.top = yCord
                // So the visual center of that dot on the original canvas is (x + 7.5, y + 7.5)
                val centerXPx = (point.x + 7.5f) * baseScale
                val centerYPx = (point.y + 7.5f) * baseScale
                val touchSizePx = with(density) { 48.dp.toPx() }

                val markerColor = when {
                    isWrong -> MarkerWrongRed
                    isHint -> MarkerHintBlue
                    isSolved -> MarkerCorrectGreen
                    isSelectedStudy -> Color(0xFFD95D14)
                    else -> MarkerDefaultYellow
                }

                val activePulse = if (isHint || isWrong || isSelectedStudy) pulseScale else 1f

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (centerXPx - touchSizePx / 2f).roundToInt(),
                                y = (centerYPx - touchSizePx / 2f).roundToInt()
                            )
                        }
                        .size(48.dp)
                        .clickable { onPointClicked(point) }
                        .testTag("quiz_marker_${index + 1}"),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer halo for visibility over any map color
                    Box(
                        modifier = Modifier
                            .size((24 * activePulse).dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(markerColor)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSolved && mode == InteractionMode.QUIZ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "सही",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = if (markerColor == MarkerDefaultYellow) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Floating Devanagari callout tag when solved in Quiz mode or active in Study mode
                val shouldShowCallout = (mode == InteractionMode.STUDY && isSelectedStudy) ||
                    (mode == InteractionMode.QUIZ && (isSolved || isHint))

                if (shouldShowCallout) {
                    val labelOffsetY = with(density) { 18.dp.toPx() }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (centerXPx - with(density) { 65.dp.toPx() }).roundToInt()
                                        .coerceIn(4, (fittedW - with(density) { 130.dp.toPx() }).roundToInt().coerceAtLeast(4)),
                                    y = (centerYPx + labelOffsetY).roundToInt()
                                        .coerceIn(4, (fittedH - with(density) { 36.dp.toPx() }).roundToInt().coerceAtLeast(4))
                                )
                            }
                            .widthIn(max = 150.dp)
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelectedStudy) Color(0xFFD95D14)
                                else Color(0xEE0A3640)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Column {
                            Text(
                                text = point.labelHi,
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
                                    color = Color.White.copy(alpha = 0.85f),
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

        // Floating Zoom Controls Overlay (Bottom-Right)
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
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
                        zoom = (zoom - 0.25f).coerceAtLeast(0.85f)
                        if (zoom <= 1f) panOffset = Offset.Zero
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "ज़ूम कम करें",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                FilledTonalIconButton(
                    onClick = {
                        zoom = (zoom + 0.25f).coerceAtMost(3.5f)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "ज़ूम बढ़ाएं",
                        modifier = Modifier.size(18.dp)
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
                            .size(36.dp)
                            .testTag("zoom_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "रीसेट ज़ूम",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
