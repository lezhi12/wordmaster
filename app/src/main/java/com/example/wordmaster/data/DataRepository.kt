package com.example.wordmaster.data

import kotlinx.coroutines.flow.Flow

interface DataRepository {
    fun getAllWords(): Flow<List<Word>>
    fun getWordsToReview(): Flow<List<Word>>
    suspend fun getWordById(id: Long): Word?
    suspend fun getExistingWordNames(words: List<String>): List<String>
    suspend fun addWord(word: String, definition: String, example: String = ""): Long
    suspend fun addWords(words: List<Word>): List<Long>
    suspend fun updateWord(word: Word)
    suspend fun deleteWord(word: Word)
    suspend fun markWordAsReviewed(word: Word, remembered: Boolean)
    suspend fun countWordsToReview(): Int
    
    // 测试用直接访问方法
    suspend fun addWordDirect(word: Word): Long
    suspend fun getAllWordsDirect(): List<Word>
}

class RoomDataRepository(
    private val wordDao: WordDao
) : DataRepository {

    override fun getAllWords(): Flow<List<Word>> = wordDao.getAllWords()

    override fun getWordsToReview(): Flow<List<Word>> = wordDao.getWordsToReview()

    override suspend fun getWordById(id: Long): Word? = wordDao.getWordById(id)

    override suspend fun getExistingWordNames(words: List<String>): List<String> {
        return wordDao.getWordsByNames(words).map { it.word }
    }

    override suspend fun addWord(word: String, definition: String, example: String): Long {
        val newWord = Word(
            word = word,
            definition = definition,
            example = example
        )
        return wordDao.insert(newWord)
    }

    override suspend fun addWords(words: List<Word>): List<Long> = wordDao.insertAll(words)

    override suspend fun updateWord(word: Word) = wordDao.update(word)

    override suspend fun deleteWord(word: Word) = wordDao.delete(word)

    override suspend fun markWordAsReviewed(word: Word, remembered: Boolean) {
        val (newLevel, nextReviewTime) = ForgettingCurve.calculateNextReviewTime(word.level, remembered)
        val updatedWord = word.copy(
            level = newLevel,
            nextReviewTime = nextReviewTime,
            lastReviewedAt = System.currentTimeMillis()
        )
        updateWord(updatedWord)
    }

    override suspend fun countWordsToReview(): Int = wordDao.countWordsToReview()
    
    // 测试用直接访问实现
    override suspend fun addWordDirect(word: Word): Long = wordDao.insert(word)
    
    override suspend fun getAllWordsDirect(): List<Word> = wordDao.getAllWordsDirect()
}
