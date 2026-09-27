package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Note
import com.example.knotes.domain.model.Priority
import com.example.knotes.domain.repository.NoteRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class SaveNoteUseCaseTest {

    @Mock
    private lateinit var repository: NoteRepository

    private lateinit var saveNoteUseCase: SaveNoteUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        saveNoteUseCase = SaveNoteUseCase(repository)
    }

    @Test
    fun `invoke should call repository insertNote and return id`() = runBlocking {
        val note = Note(title = "Test Note", content = "Test Content", priority = Priority.LOW)
        val expectedId = 1L
        `when`(repository.insertNote(note)).thenReturn(expectedId)

        val result = saveNoteUseCase(note)

        assertEquals(expectedId, result)
    }
}
