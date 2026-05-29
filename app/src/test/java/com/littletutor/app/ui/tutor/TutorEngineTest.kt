package com.littletutor.app.ui.tutor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TutorEngineTest {

    @Test
    fun checkAnswer_incrementsScoreWhenCorrect() {
        val engine = TutorEngine()

        engine.selectOption(index = 1)
        engine.checkAnswer()

        assertThat(engine.state().score).isEqualTo(1)
    }

    @Test
    fun nextQuestion_marksQuizFinishedAfterLastQuestion() {
        val engine = TutorEngine()

        repeat(3) {
            engine.selectOption(index = 1)
            engine.checkAnswer()
            engine.nextQuestion()
        }

        assertThat(engine.state().isFinished).isTrue()
    }

    @Test
    fun restart_resetsProgress() {
        val engine = TutorEngine()

        engine.selectOption(index = 1)
        engine.checkAnswer()
        engine.nextQuestion()
        engine.restart()

        val state = engine.state()
        assertThat(state.currentQuestionNumber).isEqualTo(1)
        assertThat(state.score).isEqualTo(0)
        assertThat(state.isFinished).isFalse()
    }
}

