package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPlayerScreen(
    uiState: AppUiState,
    viewModel: LearnByImageViewModel
) {
    val session = uiState.activeSession ?: return
    val quiz = session.quiz
    val isBookmarked = uiState.progressMap[quiz.id]?.isBookmarked == true
    var isFullScreenMap by rememberSaveable(quiz.id) { mutableStateOf(false) }

    BackHandler {
        if (isFullScreenMap) {
            isFullScreenMap = false
        } else {
            viewModel.navigateBack()
        }
    }

    // Full-Screen Map View Mode
    if (isFullScreenMap) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A1214))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            InteractiveImageBoard(
                quiz = quiz,
                mode = session.mode,
                solvedPointIds = session.solvedPointIds,
                wrongTappedPointId = session.wrongTappedPointId,
                hintPointId = session.hintPointId,
                selectedStudyPoint = session.selectedStudyPoint,
                showBilingual = uiState.showBilingualSubtitles,
                isFullScreen = true,
                onToggleFullScreen = { isFullScreenMap = false },
                onPointClicked = { pt -> viewModel.onMarkerClicked(pt) },
                modifier = Modifier.fillMaxSize()
            )

            // Compact floating top-left HUD pill so the user can still see the current prompt or selected item in Full-Screen mode
            Surface(
                color = Color(0xD90A2E36),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 10.dp, end = 185.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        when (session.mode) {
                            InteractionMode.QUIZ -> {
                                val targetPt = session.currentTargetPoint
                                if (!session.isCompleted && targetPt != null) {
                                    Text(
                                        text = "प्रश्न ${minOf(session.currentQuestionIndex + 1, session.shuffledQueue.size)}/${session.shuffledQueue.size} • खोजें: ${targetPt.labelHi}",
                                        color = Color(0xFFD4FF4D),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "शाबाश! सभी ${session.shuffledQueue.size} प्रश्न पूर्ण!",
                                        color = Color(0xFFFFD54F),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            InteractionMode.STUDY, InteractionMode.FLASHCARDS -> {
                                val sel = session.selectedStudyPoint
                                if (sel != null) {
                                    val idx = quiz.points.indexOfFirst { it.id == sel.id } + 1
                                    Text(
                                        text = "#$idx — ${sel.labelHi}" +
                                            if (uiState.showBilingualSubtitles) " (${sel.labelEn})" else "",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = quiz.titleHi,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (uiState.feedbackBannerText != null) {
                            Text(
                                text = uiState.feedbackBannerText,
                                color = if (uiState.isFeedbackPositive) Color(0xFFA5D6A7) else Color(0xFFFF8A80),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.speakCurrentPrompt() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "सुनें",
                            tint = Color(0xFFD4FF4D),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Hint button at Bottom-Start when in Full-Screen Quiz Mode
            if (session.mode == InteractionMode.QUIZ && !session.isCompleted) {
                FilledTonalButton(
                    onClick = { viewModel.showHint() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xD9D95D14),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "संकेत",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("संकेत", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = quiz.titleHi,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.showBilingualSubtitles) {
                            Text(
                                text = "${quiz.categoryNameHi} • ${quiz.titleEn}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("quiz_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "वापस जाएं",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isFullScreenMap = true },
                        modifier = Modifier.testTag("top_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "फुल स्क्रीन मैप",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleBilingualSubtitles() },
                        modifier = Modifier.testTag("toggle_bilingual_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "द्विभाषी उपशीर्षक बदलें",
                            tint = if (uiState.showBilingualSubtitles) {
                                Color(0xFFFFE08A)
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            }
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleVoiceEnabled() },
                        modifier = Modifier.testTag("toggle_voice_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isVoiceEnabled) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.Default.VolumeOff
                            },
                            contentDescription = "आवाज़ चालू/बंद करें",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleBookmark(quiz.id) },
                        modifier = Modifier.testTag("bookmark_quiz_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Default.BookmarkBorder
                            },
                            contentDescription = "बुकमार्क करें",
                            tint = if (isBookmarked) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateAdjacentQuiz(-1) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("prev_topic_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = "पिछला अध्याय"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("पिछला")
                    }

                    if (session.mode == InteractionMode.QUIZ && !session.isCompleted) {
                        FilledTonalButton(
                            onClick = { viewModel.showHint() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("show_hint_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "संकेत",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("संकेत (Hint)")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { viewModel.replayCurrentQuiz() },
                            modifier = Modifier.testTag("replay_quiz_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "फिर से खेलें",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("फिर से खेलें")
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateAdjacentQuiz(1) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("next_topic_button")
                    ) {
                        Text("अगला")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "अगला अध्याय"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mode Switcher Bar: क्विज़ मोड | अध्ययन मोड | फ्लैशकार्ड
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = session.mode == InteractionMode.QUIZ,
                    onClick = { viewModel.switchInteractionMode(InteractionMode.QUIZ) },
                    label = { Text("क्विज़ (खोजें)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_quiz_chip")
                )

                FilterChip(
                    selected = session.mode == InteractionMode.STUDY,
                    onClick = { viewModel.switchInteractionMode(InteractionMode.STUDY) },
                    label = { Text("अध्ययन (सूची)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_study_chip")
                )

                FilterChip(
                    selected = session.mode == InteractionMode.FLASHCARDS,
                    onClick = { viewModel.switchInteractionMode(InteractionMode.FLASHCARDS) },
                    label = { Text("फ्लैशकार्ड") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_flashcards_chip")
                )
            }

            // Active Prompt Banner (Quiz Mode) or Selected Study Banner
            when (session.mode) {
                InteractionMode.QUIZ -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0A2E36)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "प्रश्न: ${minOf(session.currentQuestionIndex + 1, session.shuffledQueue.size)} / ${session.shuffledQueue.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF80DEEA)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "समय",
                                        tint = Color(0xFFFFAB91),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val mins = session.elapsedSeconds / 60
                                    val secs = session.elapsedSeconds % 60
                                    Text(
                                        text = String.format("%02d:%02d", mins, secs),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFFFAB91),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "हल किए: ${session.solvedPointIds.size}/${session.shuffledQueue.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFA5D6A7)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val targetPt = session.currentTargetPoint
                            if (!session.isCompleted && targetPt != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "चित्र में खोजें: ${targetPt.labelHi}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD4FF4D),
                                            modifier = Modifier.testTag("active_question_prompt")
                                        )
                                        if (uiState.showBilingualSubtitles) {
                                            Text(
                                                text = "Find: ${targetPt.labelEn}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.speakCurrentPrompt() },
                                        modifier = Modifier.testTag("speak_prompt_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "प्रश्न सुनें",
                                            tint = Color(0xFFD4FF4D)
                                        )
                                    }
                                }
                            } else if (session.isCompleted) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "शाबाश! क्विज़ पूर्ण हुआ!",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD54F)
                                            )
                                            Text(
                                                text = "समय: ${session.elapsedSeconds} सेकंड • गलतियाँ: ${session.mistakesCount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { viewModel.replayCurrentQuiz() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD95D14)
                                        )
                                    ) {
                                        Text("पुनः खेलें")
                                    }
                                }
                            }
                        }
                    }
                }

                InteractionMode.STUDY -> {
                    val selectedPt = session.selectedStudyPoint
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (selectedPt != null) {
                                    val idx = quiz.points.indexOfFirst { it.id == selectedPt.id } + 1
                                    Text(
                                        text = "#$idx — ${selectedPt.labelHi}" +
                                            if (uiState.showBilingualSubtitles) " (${selectedPt.labelEn})" else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    Text(
                                        text = "अध्ययन मोड: किसी भी क्रमांक या नीचे दी गई सूची पर टैप करें",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.speakCurrentPrompt() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "उच्चारण सुनें",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                InteractionMode.FLASHCARDS -> {
                    val currentCardPt = quiz.points.getOrNull(session.flashcardIndex)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "फ्लैशकार्ड ${session.flashcardIndex + 1} / ${quiz.points.size} — चित्र में बिंदु #${session.flashcardIndex + 1} को पहचानें",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (session.isFlashcardRevealed && currentCardPt != null) {
                                Text(
                                    text = currentCardPt.labelHi,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    textAlign = TextAlign.Center
                                )
                                if (uiState.showBilingualSubtitles) {
                                    Text(
                                        text = currentCardPt.labelEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Text(
                                    text = "बिंदु #${session.flashcardIndex + 1} पर क्या है? सोचें और उत्तर देखें!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { viewModel.prevFlashcard() }) {
                                    Text("पिछला बिंदु")
                                }
                                Button(
                                    onClick = { viewModel.toggleFlashcardReveal() },
                                    modifier = Modifier.testTag("flashcard_reveal_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (session.isFlashcardRevealed) "छुपाएं" else "उत्तर देखें")
                                }
                                OutlinedButton(onClick = { viewModel.nextFlashcard() }) {
                                    Text("अगला बिंदु")
                                }
                            }
                        }
                    }
                }
            }

            // Instant Feedback Toast/Banner (Only in Quiz mode so Study mode doesn't show duplicate green bar)
            AnimatedVisibility(visible = session.mode == InteractionMode.QUIZ && uiState.feedbackBannerText != null) {
                val bannerText = uiState.feedbackBannerText ?: ""
                Surface(
                    color = if (uiState.isFeedbackPositive) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bannerText,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            // Interactive Visual Map / Diagram Board
            InteractiveImageBoard(
                quiz = quiz,
                mode = session.mode,
                solvedPointIds = session.solvedPointIds,
                wrongTappedPointId = session.wrongTappedPointId,
                hintPointId = session.hintPointId,
                selectedStudyPoint = session.selectedStudyPoint,
                showBilingual = uiState.showBilingualSubtitles,
                isFullScreen = false,
                onToggleFullScreen = { isFullScreenMap = true },
                onPointClicked = { pt -> viewModel.onMarkerClicked(pt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (session.mode == InteractionMode.STUDY) 0.62f else 1f)
            )

            // In Study Mode, show a scrollable numbered legend list of all Hindi points below the image
            if (session.mode == InteractionMode.STUDY) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.38f)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(quiz.points, key = { _, p -> p.id }) { idx, pt ->
                            val isSelected = session.selectedStudyPoint?.id == pt.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                    .clickable { viewModel.onMarkerClicked(pt) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFFD95D14)
                                            else MaterialTheme.colorScheme.primary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pt.labelHi,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (uiState.showBilingualSubtitles) {
                                        Text(
                                            text = pt.labelEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "सुनें",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
