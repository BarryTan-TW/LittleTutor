package com.littletutor.app.ui.tutor

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTokenSplitterTest {
    private val line = OcrToken(text = "春天 花朵", left = 0, top = 0, right = 500, bottom = 100)

    @Test
    fun singleWordIsKeptAsIs() {
        val token = line.copy(text = "春天")
        assertEquals(listOf(token), OcrTokenSplitter.split(token, emptyList()))
    }

    @Test
    fun spaceSeparatedWordsUseSymbolBoxes() {
        val symbols = listOf(0, 100, 300, 400).map { x -> OcrToken("字", x, 0, x + 100, 100) }

        val result = OcrTokenSplitter.split(line, symbols)

        assertEquals(listOf("春天", "花朵"), result.map { it.text })
        assertEquals(listOf(0 to 200, 300 to 500), result.map { it.left to it.right })
    }

    @Test
    fun withoutMatchingSymbolsWordsAreSplitProportionally() {
        val result = OcrTokenSplitter.split(line, emptyList())

        assertEquals(listOf("春天", "花朵"), result.map { it.text })
        assertEquals(listOf(0 to 200, 300 to 500), result.map { it.left to it.right })
    }

    @Test
    fun withoutSpacesWordsAreSplitOnVisibleGapsBetweenCharacters() {
        val element = OcrToken(text = "春天花朵", left = 0, top = 0, right = 420, bottom = 100)
        val symbols = listOf(0, 105, 310, 415).map { x -> OcrToken("字", x, 0, x + 100, 100) }

        val result = OcrTokenSplitter.split(element, symbols)

        assertEquals(listOf("春天", "花朵"), result.map { it.text })
        assertEquals(listOf(0 to 205, 310 to 515), result.map { it.left to it.right })
    }

    @Test
    fun verticalTextIsSplitOnVerticalGaps() {
        val element = OcrToken(text = "春天花", left = 0, top = 0, right = 100, bottom = 400)
        val symbols = listOf(0, 102, 300).map { y -> OcrToken("字", 0, y, 100, y + 100) }

        assertEquals(listOf("春天", "花"), OcrTokenSplitter.split(element, symbols).map { it.text })
    }

    @Test
    fun evenlySpacedCharactersStayOneWord() {
        val element = OcrToken(text = "太陽小草", left = 0, top = 0, right = 400, bottom = 100)
        val symbols = listOf(0, 100, 200, 300).map { x -> OcrToken("字", x, 0, x + 100, 100) }

        assertEquals(listOf("太陽小草"), OcrTokenSplitter.split(element, symbols).map { it.text })
    }
}
