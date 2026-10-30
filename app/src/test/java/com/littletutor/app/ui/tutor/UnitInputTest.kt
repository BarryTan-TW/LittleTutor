package com.littletutor.app.ui.tutor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitInputTest {
    @Test
    fun parseWords_splitsOnChineseAndAsciiSeparatorsAndDropsDuplicates() {
        assertThat(UnitInput.parseWords(" 春天、花朵，蝴蝶\n春天;ice cream ；\n"))
            .containsExactly("春天", "花朵", "蝴蝶", "ice cream").inOrder()
    }

    @Test
    fun titleError_rejectsBlankAndDuplicateTitles() {
        assertThat(UnitInput.titleError("  ", emptyList())).isNotNull()
        assertThat(UnitInput.titleError(" 第1課 ", listOf("第1課"))).isNotNull()
        assertThat(UnitInput.titleError("第2課", listOf("第1課"))).isNull()
    }
}
