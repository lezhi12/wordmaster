package com.example.wordmaster

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordmaster.data.Word
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WordMasterViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = (application as WordMasterApplication).repository

    val allWords: StateFlow<List<Word>> = repository.getAllWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val wordsToReview: StateFlow<List<Word>> = repository.getWordsToReview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addWord(word: String, definition: String, example: String) {
        viewModelScope.launch {
            repository.addWord(word, definition, example)
        }
    }

    fun addWords(words: List<Word>) {
        viewModelScope.launch {
            repository.addWords(words)
        }
    }

    suspend fun getExistingWordNames(words: List<String>): List<String> {
        return repository.getExistingWordNames(words)
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
