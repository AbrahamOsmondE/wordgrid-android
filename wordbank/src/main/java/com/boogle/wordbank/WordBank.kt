package com.boogle.wordbank

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WordBank {
    data class Word(var word: String, val indices: ArrayList<Index>)
    data class Index(val x: Int, val y: Int)
    enum class Dictionary(val filename: String){
        Csw19("csw19.txt"),
        Csw21("csw21.txt")
    }

    companion object {
        private lateinit var activeWordBank: Trie
        internal val dictionaryTrieMap = mutableMapOf<Dictionary, Trie>()
        fun loadWordBank(context: Context, dictionary: Dictionary): Trie {
            val inputStream = context.assets.open(dictionary.filename)
            val reader = inputStream.bufferedReader()

            val trie = Trie()

            reader.useLines { lines ->
                lines.forEach { word ->
                    trie.insert(word.trim())
                }
            }

            dictionaryTrieMap[dictionary] = trie
            activeWordBank = trie
            return trie
        }
    }

    fun selectWordBank(dictionary: Dictionary) {
        val trie = dictionaryTrieMap[dictionary] ?:
            throw Error("Invalid dictionary name")

        activeWordBank = trie
    }

    fun getWordList(letters: String): ArrayList<Word> {
        val board = toBoggleBoard(letters)
        return findWords(board)
    }

    private fun findWords(board: Array<Array<Char>>): ArrayList<Word> {
        val wordList: ArrayList<Word> = ArrayList()

        val rowNum = board.size
        val colNum = board[0].size

        val matchedWords = mutableListOf<String>()

        fun backtracking(row: Int, col: Int, parent: Trie.Node, indices:ArrayList<Index>) {
            val letter = board[row][col]
            val currNode = parent.childNodes[letter] ?: return

            indices.add(Index(row, col))

            // Check if we find a match of word
            val wordMatch = currNode.word
            if (wordMatch != null && wordMatch.length != 2) {
                matchedWords.add(wordMatch)
                wordList.add(Word(wordMatch, indices))
                currNode.word = null  // Avoid duplicates
            }

            // Before the EXPLORATION, mark the cell as visited
            board[row][col] = '#'

            // Explore the neighbors in 8 directions
            val directions = listOf(
                Pair(-1, 0), Pair(0, 1), Pair(1, 0), Pair(0, -1),
                Pair(1, 1), Pair(-1, -1), Pair(1, -1), Pair(-1, 1)
            )

            // Special handling for "Q" and "U"
            if (letter == 'Q' && currNode.childNodes.containsKey('U')) {
                val tempNode = currNode.childNodes['U']!!
                for ((rowOffset, colOffset) in directions) {
                    val newRow = row + rowOffset
                    val newCol = col + colOffset
                    if (newRow < 0 || newRow >= rowNum || newCol < 0 || newCol >= colNum) continue
                    if (board[newRow][newCol] !in tempNode.childNodes) continue
                    backtracking(newRow, newCol, tempNode, ArrayList(indices))
                }
            }
            for ((rowOffset, colOffset) in directions) {
                val newRow = row + rowOffset
                val newCol = col + colOffset
                if (newRow < 0 || newRow >= rowNum || newCol < 0 || newCol >= colNum) continue
                if (board[newRow][newCol] !in currNode.childNodes) continue
                backtracking(newRow, newCol, currNode, ArrayList(indices))
            }

            // End of EXPLORATION, restore the cell
            board[row][col] = letter

            // Optimization: incrementally remove the matched leaf node in Trie
            if (currNode.childNodes.isEmpty()) {
                parent.childNodes.remove(letter)
            }
        }

        for (row in 0 until rowNum) {
            for (col in 0 until colNum) {
                if (board[row][col] in activeWordBank.getRoot().childNodes) {
                    backtracking(row, col, activeWordBank.getRoot(), ArrayList())
                }
            }
        }

        return wordList
    }

    private fun toBoggleBoard(letters: String): Array<Array<Char>> {
        require(letters.length == 16) { "Input string must be of length 16" }
        val result = Array(4) { Array(4) { ' ' } }
        for (i in letters.indices) {
            result[i / 4][i % 4] = letters[i].uppercaseChar()
        }

        return result
    }
}