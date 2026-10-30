package com.littletutor.app.ui.tutor

object UnitInput {
    private val WORD_SEPARATORS = "[、，,；;\n\r]+".toRegex()

    fun parseWords(input: String): List<String> =
        input.split(WORD_SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /** Returns a user-facing error for [title], or null when it can be saved. */
    fun titleError(title: String, otherTitles: Collection<String>): String? {
        val trimmed = title.trim()
        return when {
            trimmed.isEmpty() -> "請輸入課文名稱"
            otherTitles.any { it.trim() == trimmed } -> "已有同名課文，請換一個名稱"
            else -> null
        }
    }
}
