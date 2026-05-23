package com.example.wordmaster.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE nextReviewTime <= strftime('%s', 'now') * 1000 ORDER BY nextReviewTime ASC")
    fun getWordsToReview(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordById(id: Long): Word?

    @Query("SELECT * FROM words WHERE word IN (:words)")
    suspend fun getWordsByNames(words: List<String>): List<Word>

    @Insert
    suspend fun insert(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<Word>): List<Long>

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)

    @Query("SELECT COUNT(*) FROM words WHERE nextReviewTime <= strftime('%s', 'now') * 1000")
    suspend fun countWordsToReview(): Int
    
    // 测试用直接查询
    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    suspend fun getAllWordsDirect(): List<Word>
}
