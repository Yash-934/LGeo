package com.example.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.CategoryInfo
import com.example.data.QuizPoint
import com.example.data.QuizProgressEntity
import com.example.data.QuizRepository
import com.example.data.QuizTopic
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BottomNavTab {
    SUBJECTS,
    SEARCH,
    BOOKMARKS,
    PROGRESS
}

enum class InteractionMode {
    QUIZ,
    STUDY,
    FLASHCARDS
}

sealed class ScreenRoute {
    data object MainTabs : ScreenRoute()
    data class CategoryTopics(val categoryId: String) : ScreenRoute()
    data class QuizPlayer(val quizId: String) : ScreenRoute()
}

data class ActiveQuizSession(
    val quiz: QuizTopic,
    val mode: InteractionMode = InteractionMode.QUIZ,
    val shuffledQueue: List<QuizPoint> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val solvedPointIds: Set<String> = emptySet(),
    val wrongTappedPointId: String? = null,
    val wrongTappedLabelHi: String? = null,
    val hintPointId: String? = null,
    val selectedStudyPoint: QuizPoint? = null,
    val flashcardIndex: Int = 0,
    val isFlashcardRevealed: Boolean = false,
    val elapsedSeconds: Int = 0,
    val mistakesCount: Int = 0,
    val isCompleted: Boolean = false
) {
    val currentTargetPoint: QuizPoint?
        get() = shuffledQueue.getOrNull(currentQuestionIndex)
}

data class AppUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryInfo> = emptyList(),
    val quizzes: List<QuizTopic> = emptyList(),
    val progressMap: Map<String, QuizProgressEntity> = emptyMap(),
    val selectedTab: BottomNavTab = BottomNavTab.SUBJECTS,
    val currentRoute: ScreenRoute = ScreenRoute.MainTabs,
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val showBilingualSubtitles: Boolean = true,
    val isVoiceEnabled: Boolean = true,
    val activeSession: ActiveQuizSession? = null,
    val feedbackBannerText: String? = null,
    val isFeedbackPositive: Boolean = true
)

class LearnByImageViewModel(
    private val repository: QuizRepository,
    appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var feedbackResetJob: Job? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        } catch (_: Exception) {
        }

        try {
            tts = TextToSpeech(appContext.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("hi", "IN"))
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
        } catch (_: Exception) {
        }

        viewModelScope.launch {
            val (cats, quizzes) = repository.loadOfflineBundleIfNeeded()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    categories = cats,
                    quizzes = quizzes
                )
            }
        }

        viewModelScope.launch {
            repository.allProgressFlow.collect { list ->
                val map = list.associateBy { it.quizId }
                _uiState.update { it.copy(progressMap = map) }
            }
        }
    }

    fun selectTab(tab: BottomNavTab) {
        stopQuizTimer()
        _uiState.update {
            it.copy(
                selectedTab = tab,
                currentRoute = ScreenRoute.MainTabs,
                activeSession = null,
                feedbackBannerText = null
            )
        }
    }

    fun openCategory(categoryId: String) {
        stopQuizTimer()
        _uiState.update {
            it.copy(
                currentRoute = ScreenRoute.CategoryTopics(categoryId),
                activeSession = null,
                feedbackBannerText = null
            )
        }
    }

    fun openQuiz(quizId: String, initialMode: InteractionMode = InteractionMode.QUIZ) {
        val quiz = _uiState.value.quizzes.find { it.id == quizId } ?: return
        val shuffled = quiz.points.shuffled()
        val session = ActiveQuizSession(
            quiz = quiz,
            mode = initialMode,
            shuffledQueue = shuffled,
            currentQuestionIndex = 0,
            solvedPointIds = emptySet(),
            selectedStudyPoint = quiz.points.firstOrNull(),
            elapsedSeconds = 0,
            isCompleted = false
        )
        _uiState.update {
            it.copy(
                currentRoute = ScreenRoute.QuizPlayer(quizId),
                activeSession = session,
                feedbackBannerText = null
            )
        }
        if (initialMode == InteractionMode.QUIZ) {
            startQuizTimer()
            shuffled.firstOrNull()?.let { firstPt ->
                speakHindi("खोजें: ${firstPt.labelHi}")
            }
        }
    }

    fun navigateBack() {
        val current = _uiState.value.currentRoute
        when (current) {
            is ScreenRoute.QuizPlayer -> {
                val catId = _uiState.value.activeSession?.quiz?.categoryId
                stopQuizTimer()
                if (catId != null && _uiState.value.selectedTab == BottomNavTab.SUBJECTS) {
                    _uiState.update {
                        it.copy(
                            currentRoute = ScreenRoute.CategoryTopics(catId),
                            activeSession = null,
                            feedbackBannerText = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            currentRoute = ScreenRoute.MainTabs,
                            activeSession = null,
                            feedbackBannerText = null
                        )
                    }
                }
            }
            is ScreenRoute.CategoryTopics -> {
                _uiState.update {
                    it.copy(
                        currentRoute = ScreenRoute.MainTabs,
                        activeSession = null,
                        feedbackBannerText = null
                    )
                }
            }
            ScreenRoute.MainTabs -> {
                if (_uiState.value.selectedTab != BottomNavTab.SUBJECTS) {
                    _uiState.update { it.copy(selectedTab = BottomNavTab.SUBJECTS) }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryFilter = categoryId) }
    }

    fun toggleBilingualSubtitles() {
        _uiState.update { it.copy(showBilingualSubtitles = !it.showBilingualSubtitles) }
    }

    fun toggleVoiceEnabled() {
        val next = !_uiState.value.isVoiceEnabled
        _uiState.update { it.copy(isVoiceEnabled = next) }
        if (!next) {
            try {
                tts?.stop()
            } catch (_: Exception) {
            }
        }
    }

    fun toggleBookmark(quizId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(quizId)
        }
    }

    fun switchInteractionMode(mode: InteractionMode) {
        val session = _uiState.value.activeSession ?: return
        if (session.mode == mode) return
        if (mode == InteractionMode.QUIZ) {
            startQuizTimer()
            session.currentTargetPoint?.let { speakHindi("खोजें: ${it.labelHi}") }
        } else {
            stopQuizTimer()
        }
        _uiState.update {
            it.copy(
                activeSession = session.copy(
                    mode = mode,
                    hintPointId = null,
                    wrongTappedPointId = null,
                    selectedStudyPoint = session.quiz.points.firstOrNull()
                ),
                feedbackBannerText = null
            )
        }
    }

    fun onMarkerClicked(point: QuizPoint) {
        val session = _uiState.value.activeSession ?: return
        if (session.mode == InteractionMode.STUDY) {
            _uiState.update {
                it.copy(
                    activeSession = session.copy(selectedStudyPoint = point),
                    feedbackBannerText = "${point.labelHi} (${point.labelEn})",
                    isFeedbackPositive = true
                )
            }
            speakHindi(point.labelHi)
            return
        }

        if (session.isCompleted) return
        val target = session.currentTargetPoint ?: return

        if (point.id == target.id || point.labelHi == target.labelHi) {
            // Correct answer!
            playSuccessTone()
            val newSolved = session.solvedPointIds + point.id
            val nextIndex = session.currentQuestionIndex + 1
            val isFinished = nextIndex >= session.shuffledQueue.size

            if (isFinished) {
                stopQuizTimer()
                val totalQ = session.shuffledQueue.size
                val accuracyScore = maxOf(0, totalQ - session.mistakesCount)
                viewModelScope.launch {
                    repository.recordQuizCompletion(
                        quizId = session.quiz.id,
                        timeSeconds = session.elapsedSeconds,
                        score = accuracyScore,
                        totalQuestions = totalQ
                    )
                }
                _uiState.update {
                    it.copy(
                        activeSession = session.copy(
                            solvedPointIds = newSolved,
                            isCompleted = true,
                            wrongTappedPointId = null,
                            hintPointId = null
                        ),
                        feedbackBannerText = "शाबाश! आपने सभी ${totalQ} प्रश्न पूरे कर लिए!",
                        isFeedbackPositive = true
                    )
                }
                speakHindi("शाबाश! आपने यह अध्याय सफलतापूर्वक पूरा कर लिया है।")
            } else {
                val nextPt = session.shuffledQueue[nextIndex]
                _uiState.update {
                    it.copy(
                        activeSession = session.copy(
                            currentQuestionIndex = nextIndex,
                            solvedPointIds = newSolved,
                            wrongTappedPointId = null,
                            hintPointId = null
                        ),
                        feedbackBannerText = "वाह! सही उत्तर: ${point.labelHi}",
                        isFeedbackPositive = true
                    )
                }
                speakHindi("सही उत्तर! अब खोजें: ${nextPt.labelHi}")
            }
        } else {
            // Wrong marker clicked
            playErrorTone()
            _uiState.update {
                it.copy(
                    activeSession = session.copy(
                        wrongTappedPointId = point.id,
                        wrongTappedLabelHi = point.labelHi,
                        mistakesCount = session.mistakesCount + 1
                    ),
                    feedbackBannerText = "ओह! आपने चुना: ${point.labelHi}",
                    isFeedbackPositive = false
                )
            }
            feedbackResetJob?.cancel()
            feedbackResetJob = viewModelScope.launch {
                delay(1800)
                _uiState.update { st ->
                    val cur = st.activeSession ?: return@update st
                    st.copy(activeSession = cur.copy(wrongTappedPointId = null))
                }
            }
        }
    }

    fun showHint() {
        val session = _uiState.value.activeSession ?: return
        val target = session.currentTargetPoint ?: return
        _uiState.update {
            it.copy(
                activeSession = session.copy(hintPointId = target.id),
                feedbackBannerText = "संकेत (Hint): नीले रंग में चमकता बिंदु देखें — ${target.labelHi}",
                isFeedbackPositive = true
            )
        }
        speakHindi(target.labelHi)
    }

    fun replayCurrentQuiz() {
        val quizId = _uiState.value.activeSession?.quiz?.id ?: return
        openQuiz(quizId, InteractionMode.QUIZ)
    }

    fun navigateAdjacentQuiz(offset: Int) {
        val currentQuiz = _uiState.value.activeSession?.quiz ?: return
        val categoryQuizzes = _uiState.value.quizzes.filter { it.categoryId == currentQuiz.categoryId }
        val idx = categoryQuizzes.indexOfFirst { it.id == currentQuiz.id }
        if (idx != -1) {
            val targetIdx = idx + offset
            if (targetIdx in categoryQuizzes.indices) {
                openQuiz(categoryQuizzes[targetIdx].id, _uiState.value.activeSession?.mode ?: InteractionMode.QUIZ)
            }
        }
    }

    fun nextFlashcard() {
        val session = _uiState.value.activeSession ?: return
        val pts = session.quiz.points
        if (pts.isEmpty()) return
        val nextIdx = (session.flashcardIndex + 1) % pts.size
        _uiState.update {
            it.copy(
                activeSession = session.copy(
                    flashcardIndex = nextIdx,
                    isFlashcardRevealed = false,
                    selectedStudyPoint = pts[nextIdx]
                )
            )
        }
    }

    fun prevFlashcard() {
        val session = _uiState.value.activeSession ?: return
        val pts = session.quiz.points
        if (pts.isEmpty()) return
        val prevIdx = if (session.flashcardIndex - 1 < 0) pts.size - 1 else session.flashcardIndex - 1
        _uiState.update {
            it.copy(
                activeSession = session.copy(
                    flashcardIndex = prevIdx,
                    isFlashcardRevealed = false,
                    selectedStudyPoint = pts[prevIdx]
                )
            )
        }
    }

    fun toggleFlashcardReveal() {
        val session = _uiState.value.activeSession ?: return
        val nextReveal = !session.isFlashcardRevealed
        _uiState.update {
            it.copy(activeSession = session.copy(isFlashcardRevealed = nextReveal))
        }
        if (nextReveal) {
            session.quiz.points.getOrNull(session.flashcardIndex)?.let {
                speakHindi(it.labelHi)
            }
        }
    }

    fun speakCurrentPrompt() {
        val session = _uiState.value.activeSession ?: return
        when (session.mode) {
            InteractionMode.QUIZ -> {
                session.currentTargetPoint?.let { speakHindi("खोजें: ${it.labelHi}", force = true) }
            }
            InteractionMode.STUDY -> {
                session.selectedStudyPoint?.let { speakHindi(it.labelHi, force = true) }
            }
            InteractionMode.FLASHCARDS -> {
                session.quiz.points.getOrNull(session.flashcardIndex)?.let {
                    speakHindi(it.labelHi, force = true)
                }
            }
        }
    }

    private fun speakHindi(text: String, force: Boolean = false) {
        if (!force && !_uiState.value.isVoiceEnabled) return
        if (!isTtsReady) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hindi_utt")
        } catch (_: Exception) {
        }
    }

    private fun playSuccessTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 160)
        } catch (_: Exception) {
        }
    }

    private fun playErrorTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 220)
        } catch (_: Exception) {
        }
    }

    private fun startQuizTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { st ->
                    val s = st.activeSession
                    if (s != null && s.mode == InteractionMode.QUIZ && !s.isCompleted) {
                        st.copy(activeSession = s.copy(elapsedSeconds = s.elapsedSeconds + 1))
                    } else {
                        st
                    }
                }
            }
        }
    }

    private fun stopQuizTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopQuizTimer()
        try {
            tts?.stop()
            tts?.shutdown()
            toneGenerator?.release()
        } catch (_: Exception) {
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val db = Room.databaseBuilder(
                        appContext,
                        AppDatabase::class.java,
                        "learn_by_image_hindi.db"
                    ).fallbackToDestructiveMigration(false).build()
                    val repo = QuizRepository(appContext, db.quizProgressDao())
                    return LearnByImageViewModel(repo, appContext) as T
                }
            }
        }
    }
}
