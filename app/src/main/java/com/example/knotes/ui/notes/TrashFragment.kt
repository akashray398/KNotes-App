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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.knotes.R
import com.example.knotes.databinding.FragmentNotesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrashFragment : Fragment() {

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

        binding.layoutDashboard.root.visibility = View.GONE
        binding.fabAddNote.visibility = View.GONE
        binding.tvGreeting.text = getString(R.string.trash)
        binding.tvSubtitle.text = getString(R.string.trash_subtitle)

        setupRecyclerView()
        setupSwipeActions()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = NotesAdapter(
            onNoteClick = { note ->
                showTrashOptions(note)
            },
            onPinClick = { }
        )
        binding.recyclerViewNotes.adapter = adapter
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun showTrashOptions(note: com.example.knotes.data.entity.Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Trash Options")
            .setItems(arrayOf("Restore", "Delete Permanently")) { _, which ->
                when (which) {
                    0 -> viewModel.restoreFromTrash(note)
                    1 -> viewModel.deletePermanently(note)
                }
            }
            .show()
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val note = adapter.currentList[position]
                
                if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.restoreFromTrash(note)
                    Snackbar.make(binding.root, "Note restored", Snackbar.LENGTH_LONG).show()
                } else if (direction == ItemTouchHelper.LEFT) {
                    viewModel.deletePermanently(note)
                    Snackbar.make(binding.root, "Note deleted permanently", Snackbar.LENGTH_LONG).show()
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trashedNotes.collect { notes ->
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
