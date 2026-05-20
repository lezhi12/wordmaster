package com.example.wordmaster.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordmaster.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    onAddWord: (String, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var word by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "添加单词",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                label = {
                    Text(
                        "单词",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OliveGray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderWarm,
                    focusedBorderColor = TerracottaBrand,
                    unfocusedContainerColor = Ivory,
                    focusedContainerColor = Ivory,
                    cursorColor = TerracottaBrand
                ),
                textStyle = MaterialTheme.typography.titleMedium.copy(color = AnthropicNearBlack)
            )

            OutlinedTextField(
                value = definition,
                onValueChange = { definition = it },
                label = {
                    Text(
                        "释义",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OliveGray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderWarm,
                    focusedBorderColor = TerracottaBrand,
                    unfocusedContainerColor = Ivory,
                    focusedContainerColor = Ivory,
                    cursorColor = TerracottaBrand
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AnthropicNearBlack)
            )

            OutlinedTextField(
                value = example,
                onValueChange = { example = it },
                label = {
                    Text(
                        "例句（可选）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OliveGray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderWarm,
                    focusedBorderColor = TerracottaBrand,
                    unfocusedContainerColor = Ivory,
                    focusedContainerColor = Ivory,
                    cursorColor = TerracottaBrand
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AnthropicNearBlack)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (word.isNotBlank() && definition.isNotBlank()) {
                        onAddWord(word, definition, example)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = word.isNotBlank() && definition.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaBrand,
                    contentColor = Ivory,
                    disabledContainerColor = WarmSand,
                    disabledContentColor = StoneGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "添加",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
