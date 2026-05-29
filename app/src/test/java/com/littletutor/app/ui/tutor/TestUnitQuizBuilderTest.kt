package com.littletutor.app.ui.tutor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TestUnitQuizBuilderTest {

    @Test
    fun buildQuestions_createsOneQuestionPerDistinctWord() {
        val units = listOf(
            TestUnit(
                id = "u1",
                title = "課文一",
                words = listOf("春天", "花朵", "春天"),
                photoPaths = emptyList()
            ),
            TestUnit(
                id = "u2",
                title = "課文二",
                words = listOf("閱讀"),
                photoPaths = emptyList()
            )
        )

        val questions = TestUnitQuizBuilder.buildQuestions(units)

        assertThat(questions).hasSize(3)
        assertThat(questions.map { it.prompt }).contains("請選出正確詞語：春天")
        assertThat(questions.map { it.prompt }).contains("請選出正確詞語：花朵")
        assertThat(questions.map { it.prompt }).contains("請選出正確詞語：閱讀")
    }

    @Test
    fun buildQuestions_keepsCorrectOptionInEachQuestion() {
        val units = listOf(
            TestUnit(
                id = "u1",
                title = "課文",
                words = listOf("國語", "課文", "測驗", "字詞"),
                photoPaths = emptyList()
            )
        )

        val questions = TestUnitQuizBuilder.buildQuestions(units)

        questions.forEach { question ->
            val target = question.prompt.substringAfter("：")
            assertThat(question.correctOptionIndex).isAtLeast(0)
            assertThat(question.options[question.correctOptionIndex]).isEqualTo(target)
        }
    }
}

