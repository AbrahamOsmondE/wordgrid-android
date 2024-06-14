package com.boogle.wordgrid.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boogle.wordgrid.ui.theme.WordGridTheme
import com.boogle.wordbank.WordBank

class MainActivity : ComponentActivity() {
    private val wordBank = WordBank()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WordBank.loadWordBank(applicationContext, WordBank.Dictionary.Csw21)
        setContent {
            WordGridTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var words by remember { mutableStateOf(listOf<WordBank.Word>()) }

                    MyApp(
                        onPlayButtonClick = { board ->
                            if (board.length == 16) {
                                words = wordBank.getWordList(board)
                            } else {
                                Toast.makeText(this, "Input must be exactly 16 characters long", Toast.LENGTH_SHORT).show()
                            }
                        },
                        words = words
                    )
                }
            }
        }
    }
}

@Composable
fun MyApp(onPlayButtonClick: (String) -> Unit, words: List<WordBank.Word>) {
    var inputText by remember { mutableStateOf("") }
    val gridSize = 4
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = inputText,
                onValueChange = {
                    if (it.length <= 16) {
                        inputText = it.filter { char -> char.isLetter() }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .border(1.dp, Color.Gray)
                    .padding(8.dp)
                    .focusRequester(focusRequester)
            )

            Button(onClick = { onPlayButtonClick(inputText) }) {
                Text("Solve")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4x4 Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0 until gridSize) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until gridSize) {
                        val index = row * gridSize + col
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(1.dp, Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < inputText.length) {
                                Text(text = inputText[index].toString(), fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Set a fixed height for the scrollable box
                .border(1.dp, Color.Gray)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                words.forEach { wordData ->
                    Row {
                        Text(text = wordData.word)
                    }
                }
            }
        }
    }
}