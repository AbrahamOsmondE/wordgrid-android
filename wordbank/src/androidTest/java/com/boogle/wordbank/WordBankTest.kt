package com.boogle.wordbank

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class WordBankTest {
    @Test
    fun loadWordBank() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val wordBank = WordBank.loadWordBank(appContext, WordBank.Dictionary.Csw21)
        Assert.assertTrue(wordBank.search("TEST"))
        Assert.assertTrue(wordBank.search("EQUATE"))

        val wordBankInstance = WordBank()
        val trie = wordBankInstance.selectWordBank(WordBank.Dictionary.Csw21)
        Assert.assertNotNull(trie)

        Assert.assertThrows(Error::class.java) {
            wordBankInstance.selectWordBank(WordBank.Dictionary.Csw19)
        }
    }

    @Test
    fun getWordList() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        WordBank.loadWordBank(appContext, WordBank.Dictionary.Csw21)

        val wordBankInstance = WordBank()
        val wordList = wordBankInstance.getWordList("aaaaaaaaaaaaaaal")

        Assert.assertEquals(
            WordBank.Word("AAL", arrayListOf(WordBank.Index(1, 1), WordBank.Index(2, 2), WordBank.Index(3, 3))),
            wordList.find { it.word == "AAL" }
        )

        Assert.assertEquals(
            WordBank.Word("ALA", arrayListOf(WordBank.Index(2, 2), WordBank.Index(3, 3), WordBank.Index(2, 3))),
            wordList.find { it.word == "ALA" }
        )
    }

    @Test
    fun getWordListWithQ() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        WordBank.loadWordBank(appContext, WordBank.Dictionary.Csw21)

        val wordBankInstance = WordBank()
        val wordList = wordBankInstance.getWordList("qaaaaaaaaaaaaaaa")

        Assert.assertNotNull(wordList.find {it.word == "AQUA"})
    }
}