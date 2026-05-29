package com.littletutor.app.ui.tutor

import java.io.File
import java.util.Base64
import java.util.Locale
import java.text.SimpleDateFormat

data class UserSpace(
    val id: String,
    val displayName: String
)

data class TestUnit(
    val id: String,
    val title: String,
    val words: List<String>,
    val photoPaths: List<String>
)

data class OcrToken(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

class UserSpaceManager(private val rootDir: File) {
    private val usersFile = File(rootDir, "users.txt")
    private val currentUserFile = File(rootDir, "current_user.txt")

    init {
        ensureInitialized()
    }

    fun listUsers(): List<UserSpace> = loadUsers()

    fun currentUser(): UserSpace {
        val users = loadUsers()
        val currentId = readCurrentUserId()
        return users.firstOrNull { it.id == currentId }
            ?: users.first().also { writeCurrentUserId(it.id) }
    }

    fun addUser(displayName: String): UserSpace {
        val trimmedName = displayName.trim()
        require(trimmedName.isNotEmpty()) { "User name cannot be empty." }

        val users = loadUsers().toMutableList()
        val baseId = toUserId(trimmedName).ifBlank { "user" }
        val newId = buildUniqueId(baseId, users.map { it.id }.toSet())
        val user = UserSpace(id = newId, displayName = trimmedName)

        users += user
        saveUsers(users)
        ensureUserDirectory(user.id)
        return user
    }

    fun switchUser(userId: String) {
        require(loadUsers().any { it.id == userId }) { "User does not exist." }
        writeCurrentUserId(userId)
    }

    fun deleteUser(userId: String): Boolean {
        val users = loadUsers().toMutableList()
        if (users.size <= 1) {
            return false
        }

        val removed = users.removeAll { it.id == userId }
        if (!removed) {
            return false
        }

        saveUsers(users)
        userDirectory(userId).deleteRecursively()

        if (readCurrentUserId() == userId) {
            writeCurrentUserId(users.first().id)
        }

        return true
    }

    fun userDirectory(userId: String): File {
        val directory = File(rootDir, userId)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    fun readProfile(userId: String): String {
        val file = profileFile(userId)
        return if (file.exists()) file.readText() else ""
    }

    fun writeProfile(userId: String, content: String) {
        profileFile(userId).writeText(content)
    }

    fun loadTestUnits(userId: String): List<TestUnit> {
        val file = testUnitsFile(userId)
        if (!file.exists()) {
            return emptyList()
        }

        return file.readLines().mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size != 4) {
                return@mapNotNull null
            }

            val id = decode(parts[0])
            val title = decode(parts[1])
            val words = decodeList(parts[2])
            val photoPaths = decodeList(parts[3])

            if (id.isBlank() || title.isBlank() || words.isEmpty()) {
                return@mapNotNull null
            }

            TestUnit(
                id = id,
                title = title,
                words = words,
                photoPaths = photoPaths
            )
        }
    }

    fun saveTestUnit(
        userId: String,
        title: String,
        words: List<String>,
        photoPaths: List<String>
    ): TestUnit {
        val trimmedTitle = title.trim()
        val cleanedWords = words.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val cleanedPhotos = photoPaths.filter { it.isNotBlank() }.distinct()
        require(trimmedTitle.isNotEmpty()) { "Unit title cannot be empty." }
        require(cleanedWords.isNotEmpty()) { "At least one word is required." }

        val allUnits = loadTestUnits(userId).toMutableList()
        val newUnit = TestUnit(
            id = createUniqueUnitId(allUnits.map { it.id }.toSet()),
            title = trimmedTitle,
            words = cleanedWords,
            photoPaths = cleanedPhotos
        )

        allUnits += newUnit
        writeTestUnits(userId, allUnits)
        return newUnit
    }

    fun appendToTestUnitByTitle(
        userId: String,
        title: String,
        words: List<String>,
        photoPaths: List<String>
    ): TestUnit {
        val trimmedTitle = title.trim()
        val cleanedWords = words.map { it.trim() }.filter { it.isNotEmpty() }
        val cleanedPhotos = photoPaths.filter { it.isNotBlank() }
        require(trimmedTitle.isNotEmpty()) { "Unit title cannot be empty." }
        require(cleanedWords.isNotEmpty()) { "At least one word is required." }

        val allUnits = loadTestUnits(userId).toMutableList()
        val index = allUnits.indexOfFirst { it.title == trimmedTitle }
        val updated = if (index >= 0) {
            val existing = allUnits[index]
            existing.copy(
                words = (existing.words + cleanedWords).distinct(),
                photoPaths = (existing.photoPaths + cleanedPhotos).distinct()
            )
        } else {
            TestUnit(
                id = createUniqueUnitId(allUnits.map { it.id }.toSet()),
                title = trimmedTitle,
                words = cleanedWords.distinct(),
                photoPaths = cleanedPhotos.distinct()
            )
        }

        if (index >= 0) {
            allUnits[index] = updated
        } else {
            allUnits += updated
        }
        writeTestUnits(userId, allUnits)
        return updated
    }

    fun updateTestUnit(
        userId: String,
        unitId: String,
        title: String,
        words: List<String>
    ): Boolean {
        val trimmedTitle = title.trim()
        val cleanedWords = words.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (trimmedTitle.isEmpty() || cleanedWords.isEmpty()) {
            return false
        }

        val allUnits = loadTestUnits(userId).toMutableList()
        val index = allUnits.indexOfFirst { it.id == unitId }
        if (index < 0) {
            return false
        }

        allUnits[index] = allUnits[index].copy(
            title = trimmedTitle,
            words = cleanedWords
        )
        writeTestUnits(userId, allUnits)
        return true
    }

    fun deleteTestUnits(userId: String, unitIds: Set<String>): Int {
        if (unitIds.isEmpty()) return 0

        val allUnits = loadTestUnits(userId)
        val deletedUnits = allUnits.filter { unitIds.contains(it.id) }
        val keptUnits = allUnits.filterNot { unitIds.contains(it.id) }
        val deletedCount = deletedUnits.size
        if (deletedCount == 0) return 0

        writeTestUnits(userId, keptUnits)

        // 刪除孤立照片：已不被任何剩餘課文參照的照片
        val keptPhotos = keptUnits.flatMap { it.photoPaths }.toSet()
        val photoDirCanonical = userPhotosDirectory(userId).canonicalFile
        deletedUnits.flatMap { it.photoPaths }.toSet()
            .filterNot { keptPhotos.contains(it) }
            .forEach { path ->
                val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return@forEach
                if (file.path.startsWith(photoDirCanonical.path) && file.exists() && file.isFile) {
                    file.delete()
                }
            }

        // 刪除測試紀錄：只刪除 unitTitle 完全等於被刪課文名稱的紀錄
        // （跨課文的整合紀錄標題如「第一課, 第二課」，只要其中一課被刪就一起移除）
        val deletedTitles = deletedUnits.map { it.title }.toSet()
        val recordsDir = File(userDirectory(userId), "test_records")
        if (recordsDir.exists()) {
            val recordFile = File(recordsDir, "records.csv")
            if (recordFile.exists()) {
                val linesToKeep = mutableListOf<String>()
                recordFile.readLines().filter { it.isNotBlank() }.forEach { line ->
                    val parts = line.split("\u0001")
                    if (parts.size == 5) {
                        val recordId = parts[0].trim()
                        val unitTitle = decode(parts[2].trim())
                        // 刪除：完全符合 OR 整合紀錄中包含已刪課文
                        val titlesInRecord = unitTitle.split(", ").toSet()
                        val shouldDelete = titlesInRecord.any { it in deletedTitles }
                        if (shouldDelete) {
                            // 一併刪除對應的 results 檔案
                            File(recordsDir, "${recordId}_results.txt").takeIf { it.exists() }?.delete()
                        } else {
                            linesToKeep += line
                        }
                    } else {
                        linesToKeep += line
                    }
                }
                recordFile.writeText(linesToKeep.joinToString("\n"))
            }
        }

        return deletedCount
    }

    fun createPhotoFile(userId: String): File {
        val photoDir = userPhotosDirectory(userId)
        var candidate: File
        do {
            val fileName = "photo_${System.currentTimeMillis()}_${System.nanoTime()}.jpg"
            candidate = File(photoDir, fileName)
        } while (candidate.exists())
        return candidate
    }

    fun listPhotos(userId: String): List<File> {
        val photoDir = userPhotosDirectory(userId)
        return photoDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("jpg", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deletePhotos(userId: String, photoPaths: Set<String>): Int {
        if (photoPaths.isEmpty()) {
            return 0
        }

        val photoDirCanonical = userPhotosDirectory(userId).canonicalFile
        var deletedCount = 0

        photoPaths.forEach { path ->
            val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return@forEach
            if (!file.path.startsWith(photoDirCanonical.path)) {
                return@forEach
            }
            if (file.exists() && file.isFile && file.delete()) {
                deletedCount += 1
            }
        }

        if (deletedCount > 0) {
            val updatedUnits = loadTestUnits(userId).map { unit ->
                unit.copy(photoPaths = unit.photoPaths.filterNot { photoPaths.contains(it) })
            }
            writeTestUnits(userId, updatedUnits)
        }

        return deletedCount
    }

    private fun ensureInitialized() {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        val existingUsers = loadUsers()
        if (existingUsers.isEmpty()) {
            val defaultUser = UserSpace(id = "guest", displayName = "訪客")
            saveUsers(listOf(defaultUser))
            ensureUserDirectory(defaultUser.id)
            writeCurrentUserId(defaultUser.id)
            return
        }

        existingUsers.forEach { ensureUserDirectory(it.id) }
        if (readCurrentUserId().isNullOrEmpty() || existingUsers.none { it.id == readCurrentUserId() }) {
            writeCurrentUserId(existingUsers.first().id)
        }
    }

    private fun ensureUserDirectory(userId: String) {
        userDirectory(userId)
    }

    private fun profileFile(userId: String): File = File(userDirectory(userId), "profile.txt")

    private fun testUnitsFile(userId: String): File = File(userDirectory(userId), "test_units.txt")

    private fun userPhotosDirectory(userId: String): File {
        val directory = File(userDirectory(userId), "photos")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun loadUsers(): List<UserSpace> {
        if (!usersFile.exists()) {
            return emptyList()
        }

        return usersFile.readLines()
            .mapNotNull { line ->
                val separatorIndex = line.indexOf('|')
                if (separatorIndex <= 0 || separatorIndex == line.lastIndex) {
                    return@mapNotNull null
                }
                val id = line.substring(0, separatorIndex).trim()
                val displayName = line.substring(separatorIndex + 1).trim()
                if (id.isBlank() || displayName.isBlank()) {
                    return@mapNotNull null
                }
                UserSpace(id = id, displayName = displayName)
            }
    }

    private fun saveUsers(users: List<UserSpace>) {
        val content = users.joinToString(separator = "\n") {
            "${it.id}|${it.displayName.replace('\n', ' ').trim()}"
        }
        usersFile.writeText(content)
    }

    private fun readCurrentUserId(): String? {
        if (!currentUserFile.exists()) {
            return null
        }
        return currentUserFile.readText().trim().ifBlank { null }
    }

    private fun writeCurrentUserId(userId: String) {
        currentUserFile.writeText(userId)
    }

    private fun toUserId(displayName: String): String {
        val lowered = displayName.lowercase(Locale.US)
        val normalized = lowered.replace("[^a-z0-9]+".toRegex(), "-").trim('-')
        return normalized
    }

    private fun buildUniqueId(baseId: String, existingIds: Set<String>): String {
        if (!existingIds.contains(baseId)) {
            return baseId
        }

        var counter = 2
        var candidate = "$baseId-$counter"
        while (existingIds.contains(candidate)) {
            counter += 1
            candidate = "$baseId-$counter"
        }
        return candidate
    }

    private fun writeTestUnits(userId: String, testUnits: List<TestUnit>) {
        val lines = testUnits.joinToString(separator = "\n") { unit ->
            listOf(
                encode(unit.id),
                encode(unit.title),
                encodeList(unit.words),
                encodeList(unit.photoPaths)
            ).joinToString(separator = "|")
        }
        testUnitsFile(userId).writeText(lines)
    }

    private fun encode(value: String): String {
        return Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decode(value: String): String {
        return runCatching {
            String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun encodeList(values: List<String>): String {
        return values.joinToString(separator = ",") { encode(it) }
    }

    private fun decodeList(value: String): List<String> {
        if (value.isBlank()) {
            return emptyList()
        }
        return value.split(",")
            .map { decode(it).trim() }
            .filter { it.isNotEmpty() }
    }

    private fun createUniqueUnitId(existingIds: Set<String>): String {
        var candidate: String
        do {
            candidate = "unit_${System.currentTimeMillis()}_${System.nanoTime()}"
        } while (existingIds.contains(candidate))
        return candidate
    }

    fun saveTestRecord(userId: String, record: TestRecord) {
        val recordsDir = File(userDirectory(userId), "test_records")
        if (!recordsDir.exists()) {
            recordsDir.mkdirs()
        }

        val recordFile = File(recordsDir, "records.csv")
        val resultFile = File(recordsDir, "${record.id}_results.txt")

        // 保存結果詳情
        val resultsContent = record.results.joinToString("\n") { "${it.word}|${if (it.isCorrect) "1" else "0"}" }
        resultFile.writeText(resultsContent)

        // 記錄到CSV
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val csvLine = buildString {
            append(record.id).append("|")
            append(dateFormat.format(record.timestamp)).append("|")
            append(encode(record.unitTitle)).append("|")
            append(record.totalCount).append("|")
            append(record.correctCount)
        }

        val existingRecords = if (recordFile.exists()) recordFile.readText() + "\n" else ""
        recordFile.writeText(existingRecords + csvLine)
    }

    fun loadTestRecords(userId: String): List<TestRecord> {
        val recordsDir = File(userDirectory(userId), "test_records")
        if (!recordsDir.exists()) {
            return emptyList()
        }

        val recordFile = File(recordsDir, "records.csv")
        if (!recordFile.exists()) {
            return emptyList()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val records = mutableListOf<TestRecord>()

        recordFile.readLines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                val parts = line.split("|")
                if (parts.size == 5) {
                    val id = parts[0].trim()
                    val timestampStr = parts[1].trim()
                    val unitTitle = decode(parts[2].trim())
                    val totalCount = parts[3].trim().toIntOrNull() ?: 0
                    val correctCount = parts[4].trim().toIntOrNull() ?: 0

                    val timestamp = try {
                        dateFormat.parse(timestampStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    // 讀取結果詳情
                    val resultFile = File(recordsDir, "${id}_results.txt")
                    val results = if (resultFile.exists()) {
                        resultFile.readLines()
                            .filter { it.isNotBlank() }
                            .mapNotNull { resultLine ->
                                val resultParts = resultLine.split("|")
                                if (resultParts.size == 2) {
                                    WritingTestResult(
                                        word = resultParts[0].trim(),
                                        isCorrect = resultParts[1].trim() == "1"
                                    )
                                } else {
                                    null
                                }
                            }
                    } else {
                        emptyList()
                    }

                    records.add(
                        TestRecord(
                            id = id,
                            timestamp = timestamp,
                            unitTitle = unitTitle,
                            totalCount = totalCount,
                            correctCount = correctCount,
                            results = results
                        )
                    )
                }
            }

        return records
    }

    companion object {
        fun withAppFilesDir(filesDir: File): UserSpaceManager {
            return UserSpaceManager(rootDir = File(filesDir, "user_spaces"))
        }
    }
}
