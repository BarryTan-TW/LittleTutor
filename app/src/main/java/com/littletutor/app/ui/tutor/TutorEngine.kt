package com.littletutor.app.ui.tutor

data class Question(
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int
)

data class TutorUiState(
    val currentQuestion: Question? = null,
    val currentQuestionNumber: Int = 0,
    val totalQuestions: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isCurrentAnswerCorrect: Boolean = false,
    val score: Int = 0,
    val isAnswerChecked: Boolean = false,
    val isFinished: Boolean = false
)

class TutorEngine(
    private val questions: List<Question> = defaultQuestions
) {
    private var questionIndex: Int = 0
    private var selectedOptionIndex: Int? = null
    private var score: Int = 0
    private var isAnswerChecked: Boolean = false

    fun state(): TutorUiState {
        val currentQuestion = questions.getOrNull(questionIndex)
        val isFinished = questionIndex >= questions.size
        val isCurrentAnswerCorrect = if (isAnswerChecked) {
            selectedOptionIndex == currentQuestion?.correctOptionIndex
        } else {
            false
        }

        return TutorUiState(
            currentQuestion = currentQuestion,
            currentQuestionNumber = (questionIndex + 1).coerceAtMost(questions.size),
            totalQuestions = questions.size,
            selectedOptionIndex = selectedOptionIndex,
            isCurrentAnswerCorrect = isCurrentAnswerCorrect,
            score = score,
            isAnswerChecked = isAnswerChecked,
            isFinished = isFinished
        )
    }

    fun selectOption(index: Int) {
        if (!isAnswerChecked && questionIndex < questions.size) {
            selectedOptionIndex = index
        }
    }

    fun checkAnswer() {
        if (isAnswerChecked || questionIndex >= questions.size) {
            return
        }

        val currentQuestion = questions[questionIndex]
        val isCorrect = selectedOptionIndex == currentQuestion.correctOptionIndex
        if (isCorrect) {
            score += 1
        }
        isAnswerChecked = true
    }

    fun nextQuestion() {
        if (!isAnswerChecked || questionIndex >= questions.size) {
            return
        }

        questionIndex += 1
        selectedOptionIndex = null
        isAnswerChecked = false
    }

    fun restart() {
        questionIndex = 0
        selectedOptionIndex = null
        score = 0
        isAnswerChecked = false
    }

    companion object {
        private val defaultQuestions = listOf(
            Question(
                prompt = "哪一顆星球被稱為紅色星球？",
                options = listOf("地球", "火星", "金星", "木星"),
                correctOptionIndex = 1
            ),
            Question(
                prompt = "9 x 7 等於多少？",
                options = listOf("56", "63", "72", "81"),
                correctOptionIndex = 1
            ),
            Question(
                prompt = "現代 Android 開發主要使用哪一種語言？",
                options = listOf("Java", "Kotlin", "Swift", "Rust"),
                correctOptionIndex = 1
            )
        )
    }
}

