package com.example.knotes.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.knotes.R
import com.example.knotes.ui.components.DashboardCompose
import com.example.knotes.domain.model.Note
import com.example.knotes.databinding.BottomSheetSortFilterBinding
import com.example.knotes.databinding.FragmentNotesBinding
import com.example.knotes.util.HapticHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
        setupDashboard()
        setupToolbarActions()
        setupSwipeActions()
        setupPullToRefresh()
        observeViewModel()
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshNotes.apply {
            setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.purple_6750A4))
            setOnRefreshListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(800)
                    isRefreshing = false
                    Toast.makeText(requireContext(), "Notes updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val note = adapter.currentList[position]
                
                if (direction == ItemTouchHelper.LEFT) {
                    HapticHelper.warning(binding.root)
                    viewModel.moveToTrash(note)
                    showUndoSnackbar("Note moved to trash") {
                        viewModel.restoreFromTrash(note)
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    HapticHelper.lightTick(binding.root)
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
        
        val onMoreClick: (Note, View) -> Unit = { note, view ->
            showNoteMoreMenu(note, view)
        }

        adapter = NotesAdapter(onNoteClick, onPinClick, onFavoriteClick, onMoreClick)
        binding.recyclerViewNotes.adapter = adapter
        updateLayoutManager()

        pinnedAdapter = NotesAdapter(onNoteClick, onPinClick, onFavoriteClick, onMoreClick)
        binding.recyclerViewPinned.adapter = pinnedAdapter
        updateLayoutManager()

        searchAdapter = NotesAdapter(
            onNoteClick = { note ->
                binding.searchView.hide()
                onNoteClick(note)
            },
            onPinClick = onPinClick,
            onFavoriteClick = onFavoriteClick,
            onMoreClick = onMoreClick
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
            HapticHelper.lightTick(it)
            navigateEditNote(-1)
        }
        binding.btnCreateFirstNote.setOnClickListener {
            HapticHelper.lightTick(it)
            navigateEditNote(-1)
        }
    }
    
    private fun navigateEditNote(id: Int) {
        val action = NotesFragmentDirections.actionNotesFragmentToEditNoteFragment(id)
        findNavController().navigate(action)
    }

    private fun setupSearch() {
        binding.searchBar.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }
    }

    private fun setupDashboard() {
        binding.composeDashboard.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.dashboardState.collectAsState()
                DashboardCompose(
                    state = state,
                    onCardClick = { id ->
                        when (id) {
                            "pending", "completed", "due_today", "overdue" -> {
                                findNavController().navigate(R.id.tasksFragment)
                            }
                            "archive" -> {
                                findNavController().navigate(R.id.archiveFragment)
                            }
                            "trash" -> {
                                findNavController().navigate(R.id.trashFragment)
                            }
                        }
                    }
                )
            }
        }
    }

    private fun setupToolbarActions() {
        binding.searchBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_layout -> {
                    viewModel.setGridView(!viewModel.isGridView.value)
                    true
                }
                R.id.action_sort -> {
                    showSortFilterBottomSheet()
                    true
                }
                R.id.action_voice_search -> {
                    Toast.makeText(requireContext(), "Voice search coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_ai_chat -> {
                    findNavController().navigate(R.id.aiChatFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateLayoutManager() {
        val isGrid = viewModel.isGridView.value
        val layoutManager = if (isGrid) {
            androidx.recyclerview.widget.StaggeredGridLayoutManager(2, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireContext())
        }
        
        binding.recyclerViewNotes.layoutManager = layoutManager
        
        binding.recyclerViewPinned.layoutManager = if (isGrid) {
            androidx.recyclerview.widget.StaggeredGridLayoutManager(2, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireContext())
        }
        
        val layoutItem = binding.searchBar.menu.findItem(R.id.action_layout)
        layoutItem?.setIcon(if (isGrid) R.drawable.ic_format_list_bulleted else R.drawable.ic_notes)
    }

    private fun showNoteMoreMenu(note: Note, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Duplicate").setOnMenuItemClickListener {
            viewModel.duplicateNote(note)
            true
        }
        popup.menu.add(if (note.isPinned) "Unpin" else "Pin").setOnMenuItemClickListener {
            viewModel.togglePin(note)
            true
        }
        popup.menu.add(if (note.isFavorite) "Unfavorite" else "Favorite").setOnMenuItemClickListener {
            viewModel.toggleFavorite(note)
            true
        }
        popup.menu.add("Archive").setOnMenuItemClickListener {
            viewModel.archiveNote(note)
            true
        }
        popup.menu.add("Delete").setOnMenuItemClickListener {
            viewModel.moveToTrash(note)
            true
        }
        popup.show()
    }

    private fun showSortFilterBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetSortFilterBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // Folders
        val allFolders = viewModel.folders.value
        val allChip = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "All Folders"
            isCheckable = true
            isChecked = viewModel.selectedFolderId.value == null
        }
        sheetBinding.chipGroupFolders.addView(allChip)
        
        allFolders.forEach { folder ->
            val chip = Chip(requireContext()).apply {
                id = folder.id.toInt()
                text = folder.name
                isCheckable = true
                isChecked = viewModel.selectedFolderId.value == folder.id
            }
            sheetBinding.chipGroupFolders.addView(chip)
        }

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
            
            val selectedFolderId = if (sheetBinding.chipGroupFolders.checkedChipId != -1 && 
                sheetBinding.chipGroupFolders.checkedChipId != allChip.id) {
                sheetBinding.chipGroupFolders.checkedChipId.toLong()
            } else null
            viewModel.selectFolder(selectedFolderId)

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
                        
                        if (notes.isEmpty() && isSearchEmpty) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            startEmptyStateAnimation()
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                        }
                        
                        binding.recyclerViewNotes.visibility = if (others.isNotEmpty() || !isSearchEmpty) View.VISIBLE else View.GONE
                        
                        updateTagFilters(notes)
                    }
                }

                launch {
                    viewModel.streakEvent.collect { streak ->
                        showStreakCelebration(streak)
                    }
                }

                launch {
                    viewModel.isGridView.collect {
                        updateLayoutManager()
                    }
                }
            }
        }
    }

    private fun startEmptyStateAnimation() {
        if (_binding == null) return
        
        val illustration = binding.layoutEmptyState.findViewById<View>(R.id.iv_empty_illustration)
        illustration?.animate()
            ?.scaleX(1.05f)
            ?.scaleY(1.05f)
            ?.setDuration(1500)
            ?.withEndAction {
                if (_binding != null) {
                    illustration.animate()
                        ?.scaleX(1f)
                        ?.scaleY(1f)
                        ?.setDuration(1500)
                        ?.withEndAction { 
                            if (_binding != null) startEmptyStateAnimation() 
                        }
                        ?.start()
                }
            }
            ?.start()
    }

    private fun showStreakCelebration(streak: Int) {
        val message = when (streak) {
            3 -> "🔥 3 Day Streak! You're on fire!"
            7 -> "🚀 7 Days! A full week of productivity!"
            15 -> "⭐ 15 Days! You're becoming a master!"
            30 -> "🏆 30 Days! Monthly Milestone Reached!"
            100 -> "👑 100 DAYS! YOU ARE LEGENDARY!"
            else -> "🔥 Daily Streak increased to $streak!"
        }
        
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.purple_6750A4))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            .show()
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
