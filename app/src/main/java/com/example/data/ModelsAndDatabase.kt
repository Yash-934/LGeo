package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

data class CategoryInfo(
    val id: String,
    val nameHi: String,
    val nameEn: String,
    val subtitleHi: String,
    val colorHex: String,
    val iconName: String
)

data class QuizPoint(
    val id: String,
    val x: Float,
    val y: Float,
    val labelHi: String,
    val labelEn: String
)

data class QuizTopic(
    val id: String,
    val orderIndex: Int,
    val categoryId: String,
    val categoryNameHi: String,
    val categoryNameEn: String,
    val titleHi: String,
    val titleEn: String,
    val imageAsset: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val points: List<QuizPoint>
)

@Entity(tableName = "quiz_progress")
data class QuizProgressEntity(
    @PrimaryKey val quizId: String,
    val isCompleted: Boolean = false,
    val isBookmarked: Boolean = false,
    val bestTimeSeconds: Int = 0,
    val bestScore: Int = 0,
    val totalQuestions: Int = 0,
    val attemptsCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L
)

@Dao
interface QuizProgressDao {
    @Query("SELECT * FROM quiz_progress")
    fun getAllProgressFlow(): Flow<List<QuizProgressEntity>>

    @Query("SELECT * FROM quiz_progress WHERE quizId = :quizId LIMIT 1")
    suspend fun getProgressById(quizId: String): QuizProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: QuizProgressEntity)
}

@Database(entities = [QuizProgressEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quizProgressDao(): QuizProgressDao
}
