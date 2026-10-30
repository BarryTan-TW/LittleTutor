package com.littletutor.app.ui.tutor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UserNameRulesTest {
    private val users = listOf(UserSpace(id = "guest", displayName = "Guest"), UserSpace(id = "amy", displayName = "小美"))

    @Test
    fun duplicateNameIgnoresCaseAndSurroundingSpaces() {
        assertThat(TutorViewModel.isDuplicateUserName("  guest ", users)).isTrue()
        assertThat(TutorViewModel.isDuplicateUserName("小美", users)).isTrue()
    }

    @Test
    fun newNameIsNotDuplicate() {
        assertThat(TutorViewModel.isDuplicateUserName("小明", users)).isFalse()
    }
}
