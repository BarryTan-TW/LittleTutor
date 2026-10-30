package com.littletutor.app.ui.tutor

/**
 * The Chinese recognizer often returns a whole line such as "春天 花朵 蝴蝶" as one element.
 * Textbook word lists are space separated, so split such elements into one selectable token per word,
 * using explicit whitespace when present, otherwise visible gaps between the character boxes.
 */
object OcrTokenSplitter {
    private val WHITESPACE = "\\s+".toRegex()

    /** [symbols] are per-character boxes for the non-whitespace characters of [element], if available. */
    fun split(element: OcrToken, symbols: List<OcrToken>): List<OcrToken> {
        val words = element.text.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.size <= 1) {
            val text = element.text.trim()
            return if (symbols.size == text.length && symbols.size >= 2) splitOnGaps(text, symbols) else listOf(element)
        }

        val nonSpaceCount = words.sumOf { it.length }
        return if (symbols.size == nonSpaceCount) {
            var cursor = 0
            words.map { word ->
                val wordSymbols = symbols.subList(cursor, cursor + word.length)
                cursor += word.length
                OcrToken(
                    text = word,
                    left = wordSymbols.minOf { it.left },
                    top = wordSymbols.minOf { it.top },
                    right = wordSymbols.maxOf { it.right },
                    bottom = wordSymbols.maxOf { it.bottom }
                )
            }
        } else {
            splitProportionally(element, words)
        }
    }

    /**
     * Recognizer boxes leave small, fairly uniform gaps between characters of one word; a word break is a gap
     * clearly wider than the typical gap and a noticeable fraction of a character.
     */
    private const val MIN_GAP_TO_CHAR_RATIO = 0.2f
    private const val GAP_TO_TYPICAL_GAP_RATIO = 2.5f

    private fun splitOnGaps(text: String, symbols: List<OcrToken>): List<OcrToken> {
        val first = symbols.first()
        val last = symbols.last()
        val horizontal = kotlin.math.abs(last.left - first.left) >= kotlin.math.abs(last.top - first.top)
        val sizes = symbols.map { if (horizontal) it.right - it.left else it.bottom - it.top }.sorted()
        val gaps = (1 until symbols.size).map { i ->
            if (horizontal) symbols[i].left - symbols[i - 1].right else symbols[i].top - symbols[i - 1].bottom
        }
        val typicalGap = gaps.sorted()[(gaps.size - 1) / 2].coerceAtLeast(0)
        val threshold = maxOf(sizes[sizes.size / 2] * MIN_GAP_TO_CHAR_RATIO, typicalGap * GAP_TO_TYPICAL_GAP_RATIO)

        val groups = mutableListOf(mutableListOf(0))
        for (i in 1 until symbols.size) {
            if (gaps[i - 1] > threshold) groups += mutableListOf(i) else groups.last() += i
        }
        if (groups.size == 1) {
            return listOf(OcrToken(text, symbols.minOf { it.left }, symbols.minOf { it.top }, symbols.maxOf { it.right }, symbols.maxOf { it.bottom }))
        }
        return groups.map { indexes ->
            val group = indexes.map { symbols[it] }
            OcrToken(
                text = indexes.joinToString(separator = "") { text[it].toString() },
                left = group.minOf { it.left },
                top = group.minOf { it.top },
                right = group.maxOf { it.right },
                bottom = group.maxOf { it.bottom }
            )
        }
    }

    private fun splitProportionally(element: OcrToken, words: List<String>): List<OcrToken> {
        val totalUnits = words.sumOf { it.length } + (words.size - 1)
        val horizontal = (element.right - element.left) >= (element.bottom - element.top)
        val start = if (horizontal) element.left else element.top
        val extent = if (horizontal) element.right - element.left else element.bottom - element.top
        var offset = 0
        return words.map { word ->
            val from = start + extent * offset / totalUnits
            val to = start + extent * (offset + word.length) / totalUnits
            offset += word.length + 1
            if (horizontal) {
                element.copy(text = word, left = from, right = to)
            } else {
                element.copy(text = word, top = from, bottom = to)
            }
        }
    }
}
