package com.boogle.wordgrid.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.boogle.wordgrid.ui.theme.WordGridTheme
import com.boogle.wordbank.WordBank
import com.boogle.wordgrid.ui.main.MainScreen

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

                    MainScreen(
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