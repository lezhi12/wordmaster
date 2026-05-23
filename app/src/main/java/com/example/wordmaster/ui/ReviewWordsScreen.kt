package com.example.wordmaster.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordmaster.data.Word
import com.example.wordmaster.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ReviewWordsScreen(
    words: List<Word>,
    onMarkWord: (Word, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    if (currentIndex >= words.size) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "复习完成！",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AnthropicNearBlack
                        )
                    },
                    navigationIcon = {
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmSand,
                                contentColor = CharcoalWarm
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("←")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Parchment
                    )
                )
            },
            containerColor = Parchment,
            modifier = modifier
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = "太棒了！",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AnthropicNearBlack
                    )
                    Text(
                        text = "您已经完成了所有单词的复习",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OliveGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerracottaBrand,
                            contentColor = Ivory
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("返回主页")
                    }
                }
            }
        }
        return
    }

    val currentWord = words[currentIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${currentIndex + 1} / ${words.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = OliveGray,
                        modifier = Modifier.testTag("review-progress")
                    )
                },
                navigationIcon = {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmSand,
                            contentColor = CharcoalWarm
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("←")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Parchment
                )
            )
        },
        containerColor = Parchment,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FlashCard(
                word = currentWord,
                isFlipped = isFlipped,
                onFlip = { isFlipped = !isFlipped },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onMarkWord(currentWord, false)
                        currentIndex++
                        isFlipped = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorCrimson,
                        contentColor = Ivory
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "忘记了",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Button(
                    onClick = {
                        onMarkWord(currentWord, true)
                        currentIndex++
                        isFlipped = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaBrand,
                        contentColor = Ivory
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "记住了",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FlashCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f),
        onClick = onFlip,
        colors = CardDefaults.cardColors(
            containerColor = Ivory
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isFlipped,
                label = "card flip",
                transitionSpec = {
                    fadeIn() + slideInVertically { it / 2 } togetherWith fadeOut() + slideOutVertically { -it / 2 }
                }
            ) { flipped ->
                if (flipped) {
                    CardBack(word = word)
                } else {
                    CardFront(word = word)
                }
            }
        }
    }
}

@Composable
fun CardFront(word: Word) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = word.word,
            style = MaterialTheme.typography.displaySmall,
            color = AnthropicNearBlack,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "点击卡片查看释义",
            style = MaterialTheme.typography.bodyMedium,
            color = StoneGray
        )
    }
}

@Composable
fun CardBack(word: Word) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = word.word,
            style = MaterialTheme.typography.titleLarge,
            color = TerracottaBrand
        )
        Text(
            text = word.definition,
            style = MaterialTheme.typography.bodyLarge,
            color = AnthropicNearBlack,
            textAlign = TextAlign.Center
        )
        if (word.example.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = word.example,
                style = MaterialTheme.typography.bodyMedium,
                color = OliveGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
