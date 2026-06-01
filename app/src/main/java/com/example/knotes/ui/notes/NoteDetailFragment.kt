package com.example.knotes.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.knotes.R
import com.example.knotes.data.entity.Note
import com.example.knotes.databinding.FragmentNoteDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()
    private val args: NoteDetailFragmentArgs by navArgs()
    private var currentNote: Note? = null
    
    private lateinit var markwon: Markwon

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        markwon = Markwon.create(requireContext())

        setupToolbar()
        loadNote()

        binding.fabEdit.setOnClickListener {
            currentNote?.let { note ->
                val action = NoteDetailFragmentDirections.actionNoteDetailFragmentToEditNoteFragment(note.id)
                findNavController().navigate(action)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            val note = viewModel.getNoteById(args.noteId)
            if (note != null) {
                currentNote = note
                displayNote(note)
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun displayNote(note: Note) {
        binding.tvTitle.text = note.title
        markwon.setMarkdown(binding.tvContent, note.description)
        
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        var meta = "Last updated: ${sdf.format(Date(note.timestamp))}"
        
        note.reminderTime?.let {
            meta += "\n⏰ Reminder: ${sdf.format(Date(it))}"
        }
        
        if (note.tags.isNotEmpty()) {
            meta += "\n🏷 Tags: ${note.tags.joinToString(", ")}"
        }
        
        binding.tvTimestamp.text = meta
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                currentNote?.let {
                    viewModel.deleteNote(it)
                    findNavController().navigateUp()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
