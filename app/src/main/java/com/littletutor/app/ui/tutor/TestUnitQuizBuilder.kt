package com.littletutor.app.ui.tutor

object TestUnitQuizBuilder {
    fun buildQuestions(testUnits: List<TestUnit>): List<Question> {
        val words = testUnits.flatMap { it.words }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (words.isEmpty()) {
            return emptyList()
        }

        val distractorPool = words
        return words.map { target ->
            val distractors = distractorPool
                .filter { it != target }
                .take(3)
            val options = (distractors + target).distinct().sortedBy { it }
            val correctIndex = options.indexOf(target)
            Question(
                prompt = "請選出正確詞語：$target",
                options = options,
                correctOptionIndex = correctIndex.coerceAtLeast(0)
            )
        }
    }
}

