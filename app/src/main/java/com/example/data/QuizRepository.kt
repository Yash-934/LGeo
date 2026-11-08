package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class QuizRepository(
    private val context: Context,
    private val progressDao: QuizProgressDao
) {
    private var cachedCategories: List<CategoryInfo> = emptyList()
    private var cachedQuizzes: List<QuizTopic> = emptyList()

    val allProgressFlow: Flow<List<QuizProgressEntity>> = progressDao.getAllProgressFlow()

    suspend fun loadOfflineBundleIfNeeded(): Pair<List<CategoryInfo>, List<QuizTopic>> =
        withContext(Dispatchers.IO) {
            if (cachedCategories.isNotEmpty() && cachedQuizzes.isNotEmpty()) {
                return@withContext cachedCategories to cachedQuizzes
            }

            val jsonText = context.assets.open("quizzes_hi.json").bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
            val root = JSONObject(jsonText)

            val catArray = root.getJSONArray("categories")
            val cats = ArrayList<CategoryInfo>(catArray.length())
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                cats.add(
                    CategoryInfo(
                        id = obj.getString("id"),
                        nameHi = obj.getString("nameHi"),
                        nameEn = obj.getString("nameEn"),
                        subtitleHi = obj.getString("subtitleHi"),
                        colorHex = obj.getString("colorHex"),
                        iconName = obj.getString("iconName")
                    )
                )
            }

            val quizArray = root.getJSONArray("quizzes")
            val quizzes = ArrayList<QuizTopic>(quizArray.length())
            for (i in 0 until quizArray.length()) {
                val obj = quizArray.getJSONObject(i)
                val ptsArray = obj.getJSONArray("points")
                val points = ArrayList<QuizPoint>(ptsArray.length())
                for (j in 0 until ptsArray.length()) {
                    val pObj = ptsArray.getJSONObject(j)
                    points.add(
                        QuizPoint(
                            id = pObj.getString("id"),
                            x = pObj.getDouble("x").toFloat(),
                            y = pObj.getDouble("y").toFloat(),
                            labelHi = pObj.getString("labelHi"),
                            labelEn = pObj.getString("labelEn")
                        )
                    )
                }
                quizzes.add(
                    QuizTopic(
                        id = obj.getString("id"),
                        orderIndex = obj.getInt("orderIndex"),
                        categoryId = obj.getString("categoryId"),
                        categoryNameHi = obj.getString("categoryNameHi"),
                        categoryNameEn = obj.getString("categoryNameEn"),
                        titleHi = obj.getString("titleHi"),
                        titleEn = obj.getString("titleEn"),
                        imageAsset = obj.getString("imageAsset"),
                        originalWidth = obj.getInt("originalWidth"),
                        originalHeight = obj.getInt("originalHeight"),
                        points = points
                    )
                )
            }

            cachedCategories = cats
            cachedQuizzes = quizzes
            cats to quizzes
        }

    suspend fun toggleBookmark(quizId: String) {
        val current = progressDao.getProgressById(quizId)
        val updated = if (current != null) {
            current.copy(isBookmarked = !current.isBookmarked)
        } else {
            QuizProgressEntity(quizId = quizId, isBookmarked = true)
        }
        progressDao.upsertProgress(updated)
    }

    suspend fun recordQuizCompletion(
        quizId: String,
        timeSeconds: Int,
        score: Int,
        totalQuestions: Int
    ) {
        val current = progressDao.getProgressById(quizId)
        val newBestTime = if (current == null || current.bestTimeSeconds == 0) {
            timeSeconds
        } else {
            minOf(current.bestTimeSeconds, timeSeconds)
        }
        val newBestScore = if (current == null) score else maxOf(current.bestScore, score)
        val newAttempts = (current?.attemptsCount ?: 0) + 1
        val updated = QuizProgressEntity(
            quizId = quizId,
            isCompleted = true,
            isBookmarked = current?.isBookmarked ?: false,
            bestTimeSeconds = newBestTime,
            bestScore = newBestScore,
            totalQuestions = totalQuestions,
            attemptsCount = newAttempts,
            lastPlayedTimestamp = System.currentTimeMillis()
        )
        progressDao.upsertProgress(updated)
    }
}
