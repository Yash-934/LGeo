package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CategoryInfo
import com.example.data.QuizProgressEntity
import com.example.data.QuizTopic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun categoryColor(categoryId: String): Color = when (categoryId) {
    "basic_gk" -> Color(0xFF0D7A6E)
    "geography" -> Color(0xFF1E6B9B)
    "history" -> Color(0xFFB8541A)
    "science_tech" -> Color(0xFF5D3FD3)
    "political_science" -> Color(0xFF9E2A4A)
    else -> Color(0xFF0A5C6B)
}

fun categoryIcon(iconName: String): ImageVector = when (iconName) {
    "lightbulb" -> Icons.Default.Lightbulb
    "public" -> Icons.Default.Public
    "account_balance" -> Icons.Default.AccountBalance
    "biotech" -> Icons.Default.Biotech
    "gavel" -> Icons.Default.Gavel
    else -> Icons.Default.MenuBook
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(
    uiState: AppUiState,
    viewModel: LearnByImageViewModel
) {
    if (uiState.selectedTab != BottomNavTab.SUBJECTS) {
        BackHandler {
            viewModel.selectTab(BottomNavTab.SUBJECTS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = "चित्र से सीखें (Learn By Image)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "संपूर्ण ऑफलाइन हिंदी सामान्य ज्ञान • 242 अध्याय",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleBilingualSubtitles() },
                        modifier = Modifier.testTag("main_bilingual_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "द्विभाषी (हिंदी + English)",
                            tint = if (uiState.showBilingualSubtitles) {
                                Color(0xFFFFE08A)
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            }
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
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.selectedTab == BottomNavTab.SUBJECTS,
                    onClick = { viewModel.selectTab(BottomNavTab.SUBJECTS) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "विषय") },
                    label = { Text("विषय") },
                    modifier = Modifier.testTag("nav_subjects")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == BottomNavTab.SEARCH,
                    onClick = { viewModel.selectTab(BottomNavTab.SEARCH) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "खोजें") },
                    label = { Text("खोजें") },
                    modifier = Modifier.testTag("nav_search")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == BottomNavTab.BOOKMARKS,
                    onClick = { viewModel.selectTab(BottomNavTab.BOOKMARKS) },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "बुकमार्क") },
                    label = { Text("बुकमार्क") },
                    modifier = Modifier.testTag("nav_bookmarks")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == BottomNavTab.PROGRESS,
                    onClick = { viewModel.selectTab(BottomNavTab.PROGRESS) },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "प्रगति") },
                    label = { Text("प्रगति") },
                    modifier = Modifier.testTag("nav_progress")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                BottomNavTab.SUBJECTS -> SubjectsTabContent(uiState, viewModel)
                BottomNavTab.SEARCH -> SearchTabContent(uiState, viewModel)
                BottomNavTab.BOOKMARKS -> BookmarksTabContent(uiState, viewModel)
                BottomNavTab.PROGRESS -> ProgressTabContent(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun SubjectsTabContent(
    uiState: AppUiState,
    viewModel: LearnByImageViewModel
) {
    val totalQuizzes = uiState.quizzes.size
    val completedCount = uiState.progressMap.values.count { it.isCompleted }
    val totalPoints = remember(uiState.quizzes) { uiState.quizzes.sumOf { it.points.size } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("subjects_list")
    ) {
        // Hero Banner Card with generated artwork & offline stats
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1794123192809),
                        contentDescription = "चित्र से सीखें बैनर",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x55000000),
                                        Color(0xE607252C)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        Surface(
                            color = Color(0xFFD95D14),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflinePin,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "100% ऑफलाइन हिंदी डेटाबेस",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "चित्र और मानचित्र से सामान्य ज्ञान सीखें",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "5 मुख्य विषय • $totalQuizzes इंटरैक्टिव चित्र अध्याय • $totalPoints+ प्रश्न • पूर्णतः देवनागरी हिंदी में",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE0F7FA)
                        )
                    }
                }
            }
        }

        // Quick Daily / Random Quiz & Progress Strip
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "आपकी अध्ययन प्रगति: $completedCount / $totalQuizzes पूर्ण",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (totalQuizzes > 0) completedCount.toFloat() / totalQuizzes else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    }
                    Button(
                        onClick = {
                            val randomQuiz = uiState.quizzes.randomOrNull()
                            if (randomQuiz != null) {
                                viewModel.openQuiz(randomQuiz.id, InteractionMode.QUIZ)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("random_quiz_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("रैंडम क्विज़")
                    }
                }
            }
        }

        item {
            Text(
                text = "विषय चुनें (Choose Subject)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(uiState.categories, key = { it.id }) { cat ->
            val catQuizzes = uiState.quizzes.filter { it.categoryId == cat.id }
            val catCompleted = catQuizzes.count { uiState.progressMap[it.id]?.isCompleted == true }
            val catPointsCount = catQuizzes.sumOf { it.points.size }
            val accent = categoryColor(cat.id)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openCategory(cat.id) }
                    .testTag("category_card_${cat.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(cat.iconName),
                            contentDescription = cat.nameHi,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.nameHi,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat.subtitleHi,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = accent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${catQuizzes.size} अध्याय",
                                    color = accent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$catPointsCount बिंदु",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (catCompleted > 0) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$catCompleted पूर्ण",
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "खोलें",
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTopicsScreen(
    categoryId: String,
    uiState: AppUiState,
    viewModel: LearnByImageViewModel
) {
    val category = uiState.categories.find { it.id == categoryId }
    val topics = remember(categoryId, uiState.quizzes, uiState.searchQuery) {
        val catQuizzes = uiState.quizzes.filter { it.categoryId == categoryId }
        val q = uiState.searchQuery.trim()
        if (q.isEmpty()) {
            catQuizzes
        } else {
            catQuizzes.filter { topic ->
                topic.titleHi.contains(q, ignoreCase = true) ||
                    topic.titleEn.contains(q, ignoreCase = true) ||
                    topic.points.any {
                        it.labelHi.contains(q, ignoreCase = true) ||
                            it.labelEn.contains(q, ignoreCase = true)
                    }
            }
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    val accentColor = categoryColor(categoryId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = category?.nameHi ?: "विषय अध्याय",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "कुल ${topics.size} चित्र अध्याय उपलब्ध",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("category_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "वापस जाएं",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleBilingualSubtitles() }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "द्विभाषी",
                            tint = if (uiState.showBilingualSubtitles) Color(0xFFFFE08A) else Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = accentColor,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("इस विषय में अध्याय या प्रश्न खोजें...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "साफ़ करें")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("category_search_input")
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("category_topics_list")
            ) {
                items(topics, key = { it.id }) { topic ->
                    QuizTopicCardItem(
                        topic = topic,
                        progress = uiState.progressMap[topic.id],
                        showBilingual = uiState.showBilingualSubtitles,
                        accentColor = accentColor,
                        onOpenQuiz = { viewModel.openQuiz(topic.id, InteractionMode.QUIZ) },
                        onOpenStudy = { viewModel.openQuiz(topic.id, InteractionMode.STUDY) },
                        onToggleBookmark = { viewModel.toggleBookmark(topic.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuizTopicCardItem(
    topic: QuizTopic,
    progress: QuizProgressEntity?,
    showBilingual: Boolean,
    accentColor: Color,
    onOpenQuiz: () -> Unit,
    onOpenStudy: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val context = LocalContext.current
    var thumbBitmap by remember(topic.imageAsset) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(topic.imageAsset) {
        thumbBitmap = withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                context.assets.open(topic.imageAsset).use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenQuiz() }
            .testTag("topic_card_${topic.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail preview from offline asset
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFECE6DD)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumbBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = topic.titleHi,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "#${topic.orderIndex}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                if (progress?.isCompleted == true) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "पूर्ण",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.titleHi,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (showBilingual) {
                    Text(
                        text = topic.titleEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${topic.points.size} प्रश्न",
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (progress?.bestTimeSeconds != null && progress.bestTimeSeconds > 0) {
                        Text(
                            text = "सर्वश्रेष्ठ: ${progress.bestTimeSeconds}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (progress?.isBookmarked == true) {
                            Icons.Default.Bookmark
                        } else {
                            Icons.Default.BookmarkBorder
                        },
                        contentDescription = "बुकमार्क",
                        tint = if (progress?.isBookmarked == true) Color(0xFFD95D14) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onOpenStudy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("पढ़ें", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onOpenQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("खेलें", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTabContent(
    uiState: AppUiState,
    viewModel: LearnByImageViewModel
) {
    val filteredQuizzes = remember(uiState.quizzes, uiState.searchQuery, uiState.selectedCategoryFilter) {
        val q = uiState.searchQuery.trim()
        uiState.quizzes.filter { topic ->
            val matchesCat = uiState.selectedCategoryFilter == null || topic.categoryId == uiState.selectedCategoryFilter
            val matchesQuery = q.isEmpty() ||
                topic.titleHi.contains(q, ignoreCase = true) ||
                topic.titleEn.contains(q, ignoreCase = true) ||
                topic.points.any {
                    it.labelHi.contains(q, ignoreCase = true) ||
                        it.labelEn.contains(q, ignoreCase = true)
                }
            matchesCat && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("सभी 242 अध्यायों और 3,000+ प्रश्नों में खोजें...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "साफ़ करें")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("global_search_input")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedCategoryFilter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("सभी विषय (${uiState.quizzes.size})") }
                )
            }
            items(uiState.categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = uiState.selectedCategoryFilter == cat.id,
                    onClick = { viewModel.setCategoryFilter(cat.id) },
                    label = { Text(cat.nameHi.substringBefore(" (")) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredQuizzes, key = { it.id }) { topic ->
                QuizTopicCardItem(
                    topic = topic,
                    progress = uiState.progressMap[topic.id],
                    showBilingual = uiState.showBilingualSubtitles,
                    accentColor = categoryColor(topic.categoryId),
                    onOpenQuiz = { viewModel.openQuiz(topic.id, InteractionMode.QUIZ) },
                    onOpenStudy = { viewModel.openQuiz(topic.id, InteractionMode.STUDY) },
                    onToggleBookmark = { viewModel.toggleBookmark(topic.id) }
                )
            }
        }
    }
}

@Composable
private fun BookmarksTabContent(
    uiState: AppUiState,
    viewModel: LearnByImageViewModel
) {
    val bookmarkedQuizzes = remember(uiState.quizzes, uiState.progressMap) {
        uiState.quizzes.filter { uiState.progressMap[it.id]?.isBookmarked == true }
    }

    if (bookmarkedQuizzes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "अभी तक कोई अध्याय बुकमार्क नहीं किया गया है",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "महत्वपूर्ण मानचित्रों और चित्रों को बाद में दोहराने के लिए बुकमार्क आइकन पर टैप करें।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(bookmarkedQuizzes, key = { it.id }) { topic ->
                QuizTopicCardItem(
                    topic = topic,
                    progress = uiState.progressMap[topic.id],
                    showBilingual = uiState.showBilingualSubtitles,
                    accentColor = categoryColor(topic.categoryId),
                    onOpenQuiz = { viewModel.openQuiz(topic.id, InteractionMode.QUIZ) },
                    onOpenStudy = { viewModel.openQuiz(topic.id, InteractionMode.STUDY) },
                    onToggleBookmark = { viewModel.toggleBookmark(topic.id) }
                )
            }
        }
    }
}

@Composable
private fun ProgressTabContent(
    uiState: AppUiState,
    viewModel: LearnByImageViewModel
) {
    val completedEntries = uiState.progressMap.values.filter { it.isCompleted }
    val totalAttempts = uiState.progressMap.values.sumOf { it.attemptsCount }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "आपकी समग्र रिपोर्ट (Overall Progress)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBadge("पूर्ण अध्याय", "${completedEntries.size} / ${uiState.quizzes.size}")
                        StatBadge("कुल प्रयास", "$totalAttempts")
                        StatBadge("बुकमार्क", "${uiState.progressMap.values.count { it.isBookmarked }}")
                    }
                }
            }
        }

        item {
            Text(
                text = "विषयवार प्रगति (Subject-wise Mastery)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.categories, key = { it.id }) { cat ->
            val catQuizzes = uiState.quizzes.filter { it.categoryId == cat.id }
            val done = catQuizzes.count { uiState.progressMap[it.id]?.isCompleted == true }
            val ratio = if (catQuizzes.isNotEmpty()) done.toFloat() / catQuizzes.size else 0f
            val accent = categoryColor(cat.id)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openCategory(cat.id) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cat.nameHi,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$done / ${catQuizzes.size} (${(ratio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        color = accent,
                        trackColor = accent.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}
