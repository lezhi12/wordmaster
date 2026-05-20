package com.example.wordmaster

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.wordmaster.ui.AddWordScreen
import com.example.wordmaster.ui.HomeScreen
import com.example.wordmaster.ui.ReviewWordsScreen

@Composable
fun MainNavigation(
    viewModel: WordMasterViewModel = viewModel()
) {
    val backStack = rememberNavBackStack(Home)
    val wordsToReview by viewModel.wordsToReview.collectAsState(emptyList())
    val allWords by viewModel.allWords.collectAsState(emptyList())

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(
                    wordsToReview = wordsToReview,
                    allWords = allWords,
                    onNavigateToAddWord = { backStack.add(AddWord) },
                    onNavigateToReview = { backStack.add(ReviewWords) },
                    onDeleteWord = { viewModel.deleteWord(it) }
                )
            }
            entry<AddWord> {
                AddWordScreen(
                    onAddWord = { word, definition, example ->
                        viewModel.addWord(word, definition, example)
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<ReviewWords> {
                ReviewWordsScreen(
                    words = wordsToReview,
                    onMarkWord = { word, remembered ->
                        viewModel.markWordAsReviewed(word, remembered)
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}

