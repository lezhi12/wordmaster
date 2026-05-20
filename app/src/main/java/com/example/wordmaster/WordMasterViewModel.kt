package com.example.wordmaster

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordmaster.data.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WordMasterViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = (application as WordMasterApplication).repository

    val allWords: Flow<List<Word>> = repository.getAllWords()
    val wordsToReview: Flow<List<Word>> = repository.getWordsToReview()

    fun addWord(word: String, definition: String, example: String) {
        viewModelScope.launch {
            repository.addWord(word, definition, example)
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            repository.deleteWord(word)
        }
    }

    fun markWordAsReviewed(word: Word, remembered: Boolean) {
        viewModelScope.launch {
            repository.markWordAsReviewed(word, remembered)
        }
    }
}
