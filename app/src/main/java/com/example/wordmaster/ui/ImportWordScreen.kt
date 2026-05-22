package com.example.wordmaster.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordmaster.WordMasterViewModel
import com.example.wordmaster.data.Word
import com.example.wordmaster.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WordJson(
    val word: String,
    val definition: String,
    val example: String = ""
)

private const val IMPORT_PROMPT = """请将我给出的英文单词整理为 JSON 数组格式返回，每个单词包含以下字段：
- word: 英文单词
- definition: 中文释义
- example: 英文例句

请严格按以下格式返回，不要添加任何其他内容：
[
  {
    "word": "eloquent",
    "definition": "雄辩的，有说服力的",
    "example": "She gave an eloquent speech at the ceremony."
  }
]

单词列表："""

data class ParsedWordItem(
    val word: Word,
    val isExisting: Boolean,
    val isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWordScreen(
    viewModel: WordMasterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var jsonInput by remember { mutableStateOf("") }
    var parsedItems by remember { mutableStateOf<List<ParsedWordItem>>(emptyList()) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isPromptExpanded by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val selectedCount = parsedItems.count { it.isSelected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "批量导入",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PromptCard(
                isExpanded = isPromptExpanded,
                onToggle = { isPromptExpanded = !isPromptExpanded },
                onCopyPrompt = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("WordMaster Prompt", IMPORT_PROMPT))
                }
            )

            OutlinedTextField(
                value = jsonInput,
                onValueChange = { newInput ->
                    jsonInput = newInput
                    parseError = null
                    if (newInput.isBlank()) {
                        parsedItems = emptyList()
                    }
                },
                label = {
                    Text(
                        "粘贴大模型返回的 JSON",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OliveGray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderWarm,
                    focusedBorderColor = TerracottaBrand,
                    unfocusedContainerColor = Ivory,
                    focusedContainerColor = Ivory,
                    cursorColor = TerracottaBrand
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = AnthropicNearBlack)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = parseJson(jsonInput)
                            when (result) {
                                is ParseResult.Success -> {
                                    parseError = null
                                    val wordNames = result.words.map { it.word }
                                    val existingNames = viewModel.getExistingWordNames(wordNames).toSet()
                                    parsedItems = result.words.map { wj ->
                                        val isExisting = wj.word in existingNames
                                        ParsedWordItem(
                                            word = Word(
                                                word = wj.word,
                                                definition = wj.definition,
                                                example = wj.example
                                            ),
                                            isExisting = isExisting,
                                            isSelected = !isExisting
                                        )
                                    }
                                }
                                is ParseResult.Error -> {
                                    parseError = result.message
                                    parsedItems = emptyList()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmSand,
                        contentColor = CharcoalWarm
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("解析", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = {
                        jsonInput = ""
                        parsedItems = emptyList()
                        parseError = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmSand,
                        contentColor = CharcoalWarm
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("清空", style = MaterialTheme.typography.titleMedium)
                }
            }

            parseError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorCrimson.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorCrimson,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (parsedItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "预览 (${parsedItems.size} 个单词)",
                        style = MaterialTheme.typography.titleMedium,
                        color = AnthropicNearBlack
                    )
                    TextButton(onClick = {
                        val allSelected = parsedItems.all { it.isSelected }
                        parsedItems = parsedItems.map { it.copy(isSelected = !allSelected) }
                    }) {
                        Text(
                            if (parsedItems.all { it.isSelected }) "取消全选" else "全选",
                            color = TerracottaBrand
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(parsedItems) { index, item ->
                        ParsedWordItemCard(
                            item = item,
                            onToggleSelect = {
                                parsedItems = parsedItems.toMutableList().apply {
                                    this[index] = item.copy(isSelected = !item.isSelected)
                                }
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        isImporting = true
                        scope.launch {
                            val wordsToImport = parsedItems.filter { it.isSelected }.map { it.word }
                            viewModel.addWords(wordsToImport)
                            isImporting = false
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedCount > 0 && !isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaBrand,
                        contentColor = Ivory,
                        disabledContainerColor = WarmSand,
                        disabledContentColor = StoneGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Ivory,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "导入 $selectedCount 个单词",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromptCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCopyPrompt: () -> Unit
) {
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = Ivory),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Prompt 模板",
                    style = MaterialTheme.typography.titleMedium,
                    color = AnthropicNearBlack
                )
                Text(
                    text = if (isExpanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelMedium,
                    color = TerracottaBrand
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = IMPORT_PROMPT,
                    style = MaterialTheme.typography.bodySmall,
                    color = OliveGray,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCopyPrompt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaBrand,
                        contentColor = Ivory
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("复制 Prompt")
                }
            }
        }
    }
}

@Composable
fun ParsedWordItemCard(
    item: ParsedWordItem,
    onToggleSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) Ivory else WarmSand.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TerracottaBrand,
                    uncheckedColor = StoneGray
                )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.word.word,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.isSelected) AnthropicNearBlack else StoneGray
                    )
                    if (item.isExisting) {
                        Text(
                            text = "已存在",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorCrimson
                        )
                    }
                }
                Text(
                    text = item.word.definition,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isSelected) OliveGray else StoneGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.word.example.isNotBlank()) {
                    Text(
                        text = item.word.example,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isSelected) StoneGray else StoneGray.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

private sealed class ParseResult {
    data class Success(val words: List<WordJson>) : ParseResult()
    data class Error(val message: String) : ParseResult()
}

private fun parseJson(input: String): ParseResult {
    val trimmed = input.trim()
    if (trimmed.isBlank()) {
        return ParseResult.Error("请先粘贴 JSON 内容")
    }

    var jsonStr = trimmed
    val codeBlockRegex = Regex("```(?:json)?\\s*\\n([\\s\\S]*?)\\n```")
    val match = codeBlockRegex.find(jsonStr)
    if (match != null) {
        jsonStr = match.groupValues[1].trim()
    }

    return try {
        val words = json.decodeFromString<List<WordJson>>(jsonStr)
        if (words.isEmpty()) {
            ParseResult.Error("解析结果为空，请检查 JSON 内容")
        } else {
            ParseResult.Success(words)
        }
    } catch (e: Exception) {
        try {
            val singleWord = json.decodeFromString<WordJson>(jsonStr)
            ParseResult.Success(listOf(singleWord))
        } catch (e2: Exception) {
            ParseResult.Error("JSON 解析失败: ${e2.message?.take(100)}")
        }
    }
}
