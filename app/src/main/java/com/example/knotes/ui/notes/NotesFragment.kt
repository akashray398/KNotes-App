package com.example.knotes.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.NestedScrollView
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
import com.example.knotes.data.entity.Note
import com.example.knotes.databinding.BottomSheetSortFilterBinding
import com.example.knotes.databinding.FragmentNotesBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()
    private lateinit var adapter: NotesAdapter
    private lateinit var pinnedAdapter: NotesAdapter
    private lateinit var searchAdapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupFab()
        setupSearch()
        setupToolbarActions()
        setupSwipeActions()
        observeViewModel()
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val note = adapter.currentList[position]
                
                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.moveToTrash(note)
                    showUndoSnackbar("Note moved to trash") {
                        viewModel.restoreFromTrash(note)
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.archiveNote(note)
                    showUndoSnackbar("Note archived") {
                        viewModel.unarchiveNote(note)
                    }
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun showUndoSnackbar(message: String, onUndo: () -> Unit) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                onUndo()
            }
            .show()
    }

    private fun setupRecyclerViews() {
        val onNoteClick: (Note) -> Unit = { note ->
            val action = NotesFragmentDirections.actionNotesFragmentToNoteDetailFragment(note.id)
            findNavController().navigate(action)
        }
        
        val onPinClick: (Note) -> Unit = { note ->
            viewModel.togglePin(note)
            val status = if (note.isPinned) "unpinned" else "pinned"
            showUndoSnackbar("Note $status") {
                viewModel.togglePin(note)
            }
        }
        
        val onFavoriteClick: (Note) -> Unit = { note ->
            viewModel.toggleFavorite(note)
        }

        adapter = NotesAdapter(onNoteClick, onPinClick, onFavoriteClick)
        binding.recyclerViewNotes.adapter = adapter
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())

        pinnedAdapter = NotesAdapter(onNoteClick, onPinClick, onFavoriteClick)
        binding.recyclerViewPinned.adapter = pinnedAdapter
        binding.recyclerViewPinned.layoutManager = LinearLayoutManager(requireContext())

        searchAdapter = NotesAdapter(
            onNoteClick = { note ->
                binding.searchView.hide()
                onNoteClick(note)
            },
            onPinClick = onPinClick,
            onFavoriteClick = onFavoriteClick
        )
        binding.recyclerViewSearch.adapter = searchAdapter
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(requireContext())
        
        // Extended FAB Scroll behavior
        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY + 12 && binding.fabAddNote.isExtended) {
                binding.fabAddNote.shrink()
            } else if (scrollY < oldScrollY - 12 && !binding.fabAddNote.isExtended) {
                binding.fabAddNote.extend()
            }
        })
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            navigateEditNote(-1)
        }
        binding.btnCreateFirstNote.setOnClickListener {
            navigateEditNote(-1)
        }
    }
    
    private fun navigateEditNote(id: Int) {
        val action = NotesFragmentDirections.actionNotesFragmentToEditNoteFragment(id)
        findNavController().navigate(action)
    }

    private fun setupSearch() {
        binding.searchView.editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun setupToolbarActions() {
        binding.searchBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_sort -> {
                    showSortFilterBottomSheet()
                    true
                }
                R.id.action_voice_search -> {
                    Toast.makeText(requireContext(), "Voice search coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun showSortFilterBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetSortFilterBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // Pre-select current values
        when (viewModel.sortOrder.value) {
            NotesViewModel.SortOrder.NEWEST -> sheetBinding.chipNewest.isChecked = true
            NotesViewModel.SortOrder.OLDEST -> sheetBinding.chipOldest.isChecked = true
            NotesViewModel.SortOrder.ALPHABETICAL -> sheetBinding.chipAlphabetical.isChecked = true
            NotesViewModel.SortOrder.PRIORITY -> sheetBinding.chipPrioritySort.isChecked = true
            else -> {}
        }
        
        sheetBinding.chipFavorites.isChecked = viewModel.filterFavorite.value

        sheetBinding.btnApply.setOnClickListener {
            val sortOrder = when (sheetBinding.chipGroupSort.checkedChipId) {
                R.id.chip_oldest -> NotesViewModel.SortOrder.OLDEST
                R.id.chip_alphabetical -> NotesViewModel.SortOrder.ALPHABETICAL
                R.id.chip_priority_sort -> NotesViewModel.SortOrder.PRIORITY
                else -> NotesViewModel.SortOrder.NEWEST
            }
            viewModel.updateSortOrder(sortOrder)
            viewModel.setFilterFavorite(sheetBinding.chipFavorites.isChecked)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notes.collect { notes ->
                        val pinned = notes.filter { it.isPinned }
                        val others = notes.filter { !it.isPinned }

                        pinnedAdapter.submitList(pinned)
                        adapter.submitList(others)
                        searchAdapter.submitList(notes)
                        
                        val isSearchEmpty = viewModel.searchQuery.value.isEmpty()
                        val hasPinned = pinned.isNotEmpty()
                        
                        binding.tvPinnedHeader.visibility = if (hasPinned && isSearchEmpty) View.VISIBLE else View.GONE
                        binding.recyclerViewPinned.visibility = if (hasPinned && isSearchEmpty) View.VISIBLE else View.GONE
                        binding.tvOthersHeader.visibility = if (hasPinned && others.isNotEmpty() && isSearchEmpty) View.VISIBLE else View.GONE
                        
                        binding.layoutEmptyState.visibility = if (notes.isEmpty() && isSearchEmpty) View.VISIBLE else View.GONE
                        binding.recyclerViewNotes.visibility = if (others.isNotEmpty() || !isSearchEmpty) View.VISIBLE else View.GONE
                        
                        updateTagFilters(notes)
                    }
                }

                launch {
                    viewModel.weeklyStats.collect { stats ->
                        binding.layoutDashboard.tvNotesThisWeek.text = stats.first.toString()
                        binding.layoutDashboard.tvTopTag.text = if (stats.second == "None") "None" else "#${stats.second}"
                        binding.layoutDashboard.tvProductivityScore.text = getString(R.string.percent_format, stats.third)
                        binding.layoutDashboard.progressProductivity.progress = stats.third
                    }
                }

                launch {
                    viewModel.totalNotesCount.collectLatest { count ->
                        binding.layoutDashboard.tvTotalNotes.text = count.toString()
                    }
                }
            }
        }
    }

    private fun updateTagFilters(notes: List<Note>) {
        val allTags = notes.flatMap { it.tags }.distinct()
        
        if (binding.chipGroupFilterTags.childCount == allTags.size + 1) return

        binding.chipGroupFilterTags.removeAllViews()
        
        // "All" chip
        val allChip = createFilterChip("All", viewModel.selectedTag.value == null) {
            viewModel.selectTag(null)
        }
        binding.chipGroupFilterTags.addView(allChip)

        allTags.forEach { tag ->
            val chip = createFilterChip(tag, viewModel.selectedTag.value == tag) {
                viewModel.selectTag(tag)
            }
            binding.chipGroupFilterTags.addView(chip)
        }
    }

    private fun createFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit): Chip {
        return Chip(requireContext()).apply {
            text = label
            isCheckable = true
            isChecked = isSelected
            setOnClickListener { onClick() }
            
            // Material 3 Filter Chip Style
            setEnsureMinTouchTargetSize(false)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
            chipStartPadding = 12f
            chipEndPadding = 12f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
