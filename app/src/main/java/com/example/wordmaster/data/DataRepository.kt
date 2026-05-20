package com.example.wordmaster.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DataRepository {
    fun getAllWords(): StateFlow<List<Word>>
    fun getWordsToReview(): StateFlow<List<Word>>
    suspend fun getWordById(id: Long): Word?
    suspend fun addWord(word: String, definition: String, example: String = ""): Long
    suspend fun updateWord(word: Word)
    suspend fun deleteWord(word: Word)
    suspend fun markWordAsReviewed(word: Word, remembered: Boolean)
    suspend fun countWordsToReview(): Int
}

class DefaultDataRepository : DataRepository {
    private val _words = MutableStateFlow<List<Word>>(emptyList())
    private val _wordsToReview = MutableStateFlow<List<Word>>(emptyList())
    
    override fun getAllWords(): StateFlow<List<Word>> = _words.asStateFlow()
    override fun getWordsToReview(): StateFlow<List<Word>> = _wordsToReview.asStateFlow()

    private fun updateWordsToReview() {
        _wordsToReview.value = _words.value.filter { it.nextReviewTime <= System.currentTimeMillis() }
    }

    override suspend fun getWordById(id: Long): Word? = _words.value.find { it.id == id }

    override suspend fun addWord(word: String, definition: String, example: String): Long {
        val newId = (_words.value.maxOfOrNull { it.id } ?: 0) + 1
        val newWord = Word(
            id = newId,
            word = word,
            definition = definition,
            example = example
        )
        _words.value = _words.value + newWord
        updateWordsToReview()
        return newId
    }

    override suspend fun updateWord(word: Word) {
        _words.value = _words.value.map { if (it.id == word.id) word else it }
        updateWordsToReview()
    }

    override suspend fun deleteWord(word: Word) {
        _words.value = _words.value.filterNot { it.id == word.id }
        updateWordsToReview()
    }

    override suspend fun markWordAsReviewed(word: Word, remembered: Boolean) {
        val (newLevel, nextReviewTime) = ForgettingCurve.calculateNextReviewTime(word.level, remembered)
        val updatedWord = word.copy(
            level = newLevel,
            nextReviewTime = nextReviewTime,
            lastReviewedAt = System.currentTimeMillis()
        )
        updateWord(updatedWord)
    }

    override suspend fun countWordsToReview(): Int = _wordsToReview.value.size
}

