package com.example.wordmaster.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.wordmaster.data.Word
import com.example.wordmaster.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    wordsToReview: List<Word>,
    allWords: List<Word>,
    onNavigateToAddWord: () -> Unit,
    onNavigateToImportWord: () -> Unit,
    onNavigateToReview: () -> Unit,
    onDeleteWord: (Word) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var wordToDelete by remember { mutableStateOf<Word?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "WordMaster",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AnthropicNearBlack
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Parchment
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = TerracottaBrand,
                contentColor = Ivory,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        },
        containerColor = Parchment,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ReviewCard(
                wordCount = wordsToReview.size,
                onReviewClick = onNavigateToReview,
                enabled = wordsToReview.isNotEmpty()
            )

            Text(
                text = "我的单词本",
                style = MaterialTheme.typography.titleLarge,
                color = AnthropicNearBlack
            )

            if (allWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📚",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "还没有添加单词",
                            style = MaterialTheme.typography.titleMedium,
                            color = OliveGray
                        )
                        Text(
                            text = "点击右下角的 + 开始添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StoneGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allWords) { word ->
                        WordItem(
                            word = word,
                            onDelete = { wordToDelete = word }
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        AddWordBottomSheet(
            onManualAdd = {
                showBottomSheet = false
                onNavigateToAddWord()
            },
            onBatchImport = {
                showBottomSheet = false
                onNavigateToImportWord()
            },
            onDismiss = { showBottomSheet = false }
        )
    }

    if (wordToDelete != null) {
        AlertDialog(
            onDismissRequest = { wordToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除单词 \"${wordToDelete!!.word}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        wordToDelete?.let(onDeleteWord)
                        wordToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("删除", color = ErrorCrimson)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { wordToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordBottomSheet(
    onManualAdd: () -> Unit,
    onBatchImport: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Ivory,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "添加单词",
                style = MaterialTheme.typography.headlineSmall,
                color = AnthropicNearBlack
            )
            Text(
                text = "选择添加方式",
                style = MaterialTheme.typography.bodyMedium,
                color = OliveGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onManualAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = WarmSand,
                    contentColor = CharcoalWarm
                )
            ) {
                Text(
                    "手动添加",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Button(
                onClick = onBatchImport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaBrand,
                    contentColor = Ivory
                )
            ) {
                Text(
                    "批量导入",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun ReviewCard(
    wordCount: Int,
    onReviewClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onReviewClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) TerracottaBrand else WarmSand
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "复习单词",
                style = MaterialTheme.typography.headlineSmall,
                color = if (enabled) Ivory else OliveGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (wordCount > 0) "您有 $wordCount 个单词需要复习" else "暂无需要复习的单词",
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) WarmSilver else StoneGray
            )
        }
    }
}

@Composable
fun WordItem(
    word: Word,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Ivory
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleMedium,
                    color = AnthropicNearBlack
                )
                Text(
                    text = word.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OliveGray
                )
                Text(
                    text = "等级: ${word.level}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StoneGray
                )
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_${word.word}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmSand,
                    contentColor = CharcoalWarm
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("删除")
            }
        }
    }
}
