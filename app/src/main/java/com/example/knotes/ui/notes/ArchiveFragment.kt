package com.example.knotes.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.knotes.R
import com.example.knotes.databinding.FragmentNotesBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArchiveFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()
    private lateinit var adapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide dashboard and FAB as they are not needed in Archive
        binding.layoutDashboard.root.visibility = View.GONE
        binding.fabAddNote.visibility = View.GONE
        binding.tvGreeting.text = getString(R.string.archive)
        binding.tvSubtitle.text = getString(R.string.archived_notes_subtitle)

        setupRecyclerView()
        setupSwipeActions()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = NotesAdapter(
            onNoteClick = { note ->
                val action = ArchiveFragmentDirections.actionArchiveFragmentToNoteDetailFragment(note.id)
                findNavController().navigate(action)
            },
            onPinClick = { note -> viewModel.togglePin(note) }
        )
        binding.recyclerViewNotes.adapter = adapter
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val note = adapter.currentList[position]
                
                if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.unarchiveNote(note)
                    Snackbar.make(binding.root, "Note unarchived", Snackbar.LENGTH_LONG)
                        .setAction("Undo") { viewModel.archiveNote(note) }
                        .show()
                } else if (direction == ItemTouchHelper.LEFT) {
                    viewModel.moveToTrash(note)
                    Snackbar.make(binding.root, "Note moved to trash", Snackbar.LENGTH_LONG)
                        .setAction("Undo") { viewModel.restoreFromTrash(note) }
                        .show()
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.archivedNotes.collect { notes ->
                    adapter.submitList(notes)
                    binding.layoutEmptyState.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerViewNotes.visibility = if (notes.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
