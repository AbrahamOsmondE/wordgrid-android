package com.boogle.wordbank

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TrieUnitTest {
    @Test
    fun shouldInsertAndSearchTrie() {
        val trie = Trie()
        trie.insert("tests")

        assertTrue(trie.search("tests"))
        assertFalse(trie.search("test"))
        assertFalse(trie.search(""))
    }

    @Test
    fun shouldInsertAndSearchPrefixesInTrie() {
        val trie = Trie()
        trie.insert("prefix")
        trie.insert("pre")

        assertTrue(trie.search("prefix"))
        assertTrue(trie.search("pre"))
    }
}