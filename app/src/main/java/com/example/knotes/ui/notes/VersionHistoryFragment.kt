package com.example.knotes.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.knotes.databinding.FragmentVersionHistoryBinding
import com.example.knotes.domain.usecase.GetNoteVersionsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VersionHistoryFragment : Fragment() {

    private var _binding: FragmentVersionHistoryBinding? = null
    private val binding get() = _binding!!

    private val args: VersionHistoryFragmentArgs by navArgs()
    
    @Inject
    lateinit var getNoteVersionsUseCase: GetNoteVersionsUseCase
    
    @Inject
    lateinit var noteRepository: com.example.knotes.domain.repository.NoteRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVersionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val noteId = args.noteId
        
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        
        val adapter = VersionHistoryAdapter { version ->
            restoreVersion(version)
        }
        
        binding.recyclerViewVersions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewVersions.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            getNoteVersionsUseCase(noteId).collectLatest { versions ->
                adapter.submitList(versions)
                binding.tvEmpty.visibility = if (versions.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun restoreVersion(version: com.example.knotes.domain.model.NoteVersion) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentNote = noteRepository.getNoteById(version.noteId)
                currentNote?.let {
                    noteRepository.updateNote(it.copy(
                        title = version.title,
                        content = version.content,
                        updatedTime = System.currentTimeMillis()
                    ))
                    Toast.makeText(requireContext(), "Version restored", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to restore", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
