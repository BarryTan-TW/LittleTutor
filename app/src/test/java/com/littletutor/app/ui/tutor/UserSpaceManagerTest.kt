package com.littletutor.app.ui.tutor

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserSpaceManagerTest {
    private lateinit var tempDir: File
    private lateinit var manager: UserSpaceManager

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("user-space-test").toFile()
        manager = UserSpaceManager(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun initialize_createsDefaultGuestUser() {
        val users = manager.listUsers()
        val current = manager.currentUser()

        assertThat(users).hasSize(1)
        assertThat(users.first().id).isEqualTo("guest")
        assertThat(current.id).isEqualTo("guest")
        assertThat(manager.userDirectory("guest").exists()).isTrue()
    }

    @Test
    fun addSwitchAndDeleteUser_updatesCurrentUserAndDirectories() {
        val added = manager.addUser("Alice")
        manager.switchUser(added.id)

        assertThat(manager.currentUser().displayName).isEqualTo("Alice")

        val deleted = manager.deleteUser(added.id)

        assertThat(deleted).isTrue()
        assertThat(manager.currentUser().id).isEqualTo("guest")
        assertThat(File(tempDir, added.id).exists()).isFalse()
    }

    @Test
    fun writeAndReadProfile_persistsInUserDirectory() {
        val user = manager.addUser("Bob")
        val profileText = "Bob profile data"

        manager.writeProfile(user.id, profileText)

        assertThat(manager.readProfile(user.id)).isEqualTo(profileText)
    }

    @Test
    fun createPhotoFile_andListPhotos_storesInUserPhotosDirectory() {
        val user = manager.addUser("Camera")
        val first = manager.createPhotoFile(user.id)
        val second = manager.createPhotoFile(user.id)

        first.writeText("a")
        second.writeText("b")

        val photos = manager.listPhotos(user.id)

        assertThat(photos).containsAtLeast(first, second)
        assertThat(first.parentFile?.name).isEqualTo("photos")
    }

    @Test
    fun deletePhotos_removesFilesAndUnitReferences() {
        val user = manager.addUser("PhotoDelete")
        val first = manager.createPhotoFile(user.id)
        val second = manager.createPhotoFile(user.id)
        first.writeText("a")
        second.writeText("b")

        manager.saveTestUnit(
            userId = user.id,
            title = "課文",
            words = listOf("春天"),
            photoPaths = listOf(first.absolutePath, second.absolutePath)
        )

        val deleted = manager.deletePhotos(user.id, setOf(first.absolutePath))

        assertThat(deleted).isEqualTo(1)
        assertThat(first.exists()).isFalse()
        assertThat(second.exists()).isTrue()
        val loaded = manager.loadTestUnits(user.id)
        assertThat(loaded).hasSize(1)
        assertThat(loaded.first().photoPaths).containsExactly(second.absolutePath)
    }

    @Test
    fun saveAndLoadTestUnits_persistsWordsAndPhotoPaths() {
        val user = manager.addUser("Mandarin")
        val unit = manager.saveTestUnit(
            userId = user.id,
            title = "第1課",
            words = listOf("春天", "讀書"),
            photoPaths = listOf("/tmp/a.jpg", "/tmp/b.jpg")
        )

        val loaded = manager.loadTestUnits(user.id)

        assertThat(loaded).hasSize(1)
        assertThat(loaded.first().id).isEqualTo(unit.id)
        assertThat(loaded.first().title).isEqualTo("第1課")
        assertThat(loaded.first().words).containsExactly("春天", "讀書")
        assertThat(loaded.first().photoPaths).containsExactly("/tmp/a.jpg", "/tmp/b.jpg")
    }

    @Test
    fun appendToTestUnitByTitle_mergesWordsAndPhotosInSameUnit() {
        val user = manager.addUser("Append")

        manager.appendToTestUnitByTitle(
            userId = user.id,
            title = "課文單詞句",
            words = listOf("春天", "讀書"),
            photoPaths = listOf("/tmp/a.jpg")
        )
        manager.appendToTestUnitByTitle(
            userId = user.id,
            title = "課文單詞句",
            words = listOf("讀書", "作文"),
            photoPaths = listOf("/tmp/b.jpg")
        )

        val loaded = manager.loadTestUnits(user.id)

        assertThat(loaded).hasSize(1)
        assertThat(loaded.first().words).containsExactly("春天", "讀書", "作文")
        assertThat(loaded.first().photoPaths).containsExactly("/tmp/a.jpg", "/tmp/b.jpg")
    }

    @Test
    fun updateTestUnit_updatesTitleAndWords() {
        val user = manager.addUser("Edit")
        val unit = manager.saveTestUnit(
            userId = user.id,
            title = "原標題",
            words = listOf("甲", "乙"),
            photoPaths = emptyList()
        )

        val updated = manager.updateTestUnit(
            userId = user.id,
            unitId = unit.id,
            title = "新標題",
            words = listOf("丙", "丁")
        )

        assertThat(updated).isTrue()
        val loaded = manager.loadTestUnits(user.id)
        assertThat(loaded).hasSize(1)
        assertThat(loaded.first().title).isEqualTo("新標題")
        assertThat(loaded.first().words).containsExactly("丙", "丁")
    }

    @Test
    fun deleteTestUnits_removesSelectedItems() {
        val user = manager.addUser("Delete")
        val first = manager.saveTestUnit(user.id, "A", listOf("一"), emptyList())
        manager.saveTestUnit(user.id, "B", listOf("二"), emptyList())

        val deleted = manager.deleteTestUnits(user.id, setOf(first.id))

        assertThat(deleted).isEqualTo(1)
        val loaded = manager.loadTestUnits(user.id)
        assertThat(loaded).hasSize(1)
        assertThat(loaded.first().title).isEqualTo("B")
    }

    @Test
    fun deleteUser_returnsFalseWhenTryingToDeleteLastUser() {
        val deleted = manager.deleteUser("guest")

        assertThat(deleted).isFalse()
        assertThat(manager.listUsers()).hasSize(1)
    }
}


