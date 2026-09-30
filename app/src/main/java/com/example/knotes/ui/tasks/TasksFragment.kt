package com.example.knotes.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
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
import com.example.knotes.databinding.FragmentTasksBinding
import com.example.knotes.ui.components.NextUpTaskCompose
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TasksViewModel by viewModels()
    private lateinit var adapter: TasksAdapter
    private lateinit var completedAdapter: TasksAdapter
    private lateinit var searchAdapter: TasksAdapter

    private var isCompletedExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkPermissions()
        setupRecyclerViews()
        setupFab()
        setupSearch()
        setupNextUpCard()
        setupFilters()
        setupToolbarActions()
        setupSwipeActions()
        setupPullToRefresh()
        observeViewModel()
    }

    private fun checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Reminders might not work without notification permission", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupNextUpCard() {
        binding.composeNextUp.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
                NextUpTaskCompose(
                    tasks = tasks,
                    onTaskClick = { taskId ->
                        navigateToEdit(taskId)
                    }
                )
            }
        }
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshTasks.apply {
            val primaryColor = com.google.android.material.color.MaterialColors.getColor(
                requireContext(),
                androidx.appcompat.R.attr.colorPrimary,
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
            setColorSchemeColors(primaryColor)
            setOnRefreshListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(800)
                    isRefreshing = false
                    Toast.makeText(requireContext(), "Tasks updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        adapter = TasksAdapter(
            onTaskClick = { task -> navigateToEdit(task.id) },
            onTaskCheckedChange = { task -> viewModel.toggleTaskCompletion(task) },
            onNoteClick = { noteId -> navigateToNote(noteId) }
        )
        binding.recyclerViewTasks.adapter = adapter
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())

        completedAdapter = TasksAdapter(
            onTaskClick = { task -> navigateToEdit(task.id) },
            onTaskCheckedChange = { task -> viewModel.toggleTaskCompletion(task) },
            onNoteClick = { noteId -> navigateToNote(noteId) }
        )
        binding.recyclerViewCompletedTasks.adapter = completedAdapter
        binding.recyclerViewCompletedTasks.layoutManager = LinearLayoutManager(requireContext())

        searchAdapter = TasksAdapter(
            onTaskClick = { task ->
                binding.searchView.hide()
                navigateToEdit(task.id)
            },
            onTaskCheckedChange = { task -> viewModel.toggleTaskCompletion(task) },
            onNoteClick = { noteId ->
                binding.searchView.hide()
                navigateToNote(noteId)
            }
        )
        binding.recyclerViewSearch.adapter = searchAdapter
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(requireContext())

        binding.layoutCompletedHeader.setOnClickListener {
            isCompletedExpanded = !isCompletedExpanded
            binding.recyclerViewCompletedTasks.visibility = if (isCompletedExpanded) View.VISIBLE else View.GONE
            binding.ivCompletedArrow.rotation = if (isCompletedExpanded) 90f else 270f
        }

        // Extended FAB Scroll behavior
        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY + 12 && binding.fabAddTask.isExtended) {
                binding.fabAddTask.shrink()
            } else if (scrollY < oldScrollY - 12 && !binding.fabAddTask.isExtended) {
                binding.fabAddTask.extend()
            }
        })
    }

    private fun navigateToEdit(taskId: Int) {
        val action = TasksFragmentDirections.actionTasksFragmentToEditTaskFragment(taskId)
        findNavController().navigate(action)
    }

    private fun navigateToNote(noteId: Int) {
        val action = TasksFragmentDirections.actionTasksFragmentToEditNoteFragment(noteId)
        findNavController().navigate(action)
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener { navigateToEdit(-1) }
        binding.btnCreateFirstTask.setOnClickListener { navigateToEdit(-1) }
    }

    private fun setupSearch() {
        binding.searchView.editText.addTextChangedListener { text ->
            viewModel.updateSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupFilters() {
        val filterOptions = listOf("All", "Today", "Upcoming", "Overdue", "Completed")
        binding.chipGroupFilters.removeAllViews()
        filterOptions.forEach { option ->
            val chip = Chip(requireContext()).apply {
                text = option
                isCheckable = true
                isChecked = (option == "All" && viewModel.taskFilter.value == TasksViewModel.TaskFilter.ALL) ||
                            (option == "Today" && viewModel.taskFilter.value == TasksViewModel.TaskFilter.TODAY) ||
                            (option == "Upcoming" && viewModel.taskFilter.value == TasksViewModel.TaskFilter.UPCOMING) ||
                            (option == "Overdue" && viewModel.taskFilter.value == TasksViewModel.TaskFilter.OVERDUE) ||
                            (option == "Completed" && viewModel.taskFilter.value == TasksViewModel.TaskFilter.COMPLETED)
                setEnsureMinTouchTargetSize(false)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
                setOnClickListener {
                    val filter = when (option) {
                        "Today" -> TasksViewModel.TaskFilter.TODAY
                        "Upcoming" -> TasksViewModel.TaskFilter.UPCOMING
                        "Overdue" -> TasksViewModel.TaskFilter.OVERDUE
                        "Completed" -> TasksViewModel.TaskFilter.COMPLETED
                        else -> TasksViewModel.TaskFilter.ALL
                    }
                    viewModel.updateTaskFilter(filter)
                }
            }
            binding.chipGroupFilters.addView(chip)
        }
    }

    private fun setupToolbarActions() {
        binding.btnFilterMenu.setOnClickListener { showSortMenu() }
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.btnFilterMenu)
        popup.menuInflater.inflate(R.menu.menu_task_sort, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            val order = when (item.itemId) {
                R.id.sort_due_date -> TasksViewModel.SortOrder.DUE_DATE
                R.id.sort_priority -> TasksViewModel.SortOrder.PRIORITY
                R.id.sort_newest -> TasksViewModel.SortOrder.NEWEST
                R.id.sort_oldest -> TasksViewModel.SortOrder.OLDEST
                R.id.sort_alphabetical -> TasksViewModel.SortOrder.ALPHABETICAL
                else -> TasksViewModel.SortOrder.DUE_DATE
            }
            viewModel.updateSortOrder(order)
            true
        }
        popup.show()
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                val task = if (viewHolder.itemView.parent == binding.recyclerViewTasks) {
                    adapter.currentList[pos]
                } else {
                    completedAdapter.currentList[pos]
                }
                
                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.deleteTask(task)
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.toggleTaskCompletion(task)
                }
            }
        }
        ItemTouchHelper(swipeHandler).apply {
            attachToRecyclerView(binding.recyclerViewTasks)
            attachToRecyclerView(binding.recyclerViewCompletedTasks)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tasks.collect { tasks ->
                        val activeTasks = tasks.filter { !it.isCompleted }
                        val completedTasks = tasks.filter { it.isCompleted }
                        
                        adapter.submitList(activeTasks)
                        completedAdapter.submitList(completedTasks)
                        searchAdapter.submitList(tasks)
                        
                        binding.layoutEmptyState.visibility = if (tasks.isEmpty() && viewModel.searchQuery.value.isEmpty()) View.VISIBLE else View.GONE
                        binding.layoutCompletedHeader.visibility = if (completedTasks.isNotEmpty()) View.VISIBLE else View.GONE
                        
                        binding.tvCompletedHeader.text = getString(R.string.completed_tasks_count, completedTasks.size)
                    }
                }

                launch {
                    viewModel.productivityStats.collect { stats ->
                        val completed = stats.first
                        val pending = stats.second
                        val percent = stats.third
                        
                        binding.tvSummary.text = getString(R.string.tasks_summary, pending, completed)
                        binding.progressIndicator.setProgress(percent, true)
                        binding.tvProgressPercent.text = getString(R.string.percent_format, percent)
                        binding.tvMotivation.text = when {
                            percent >= 100 -> "Incredible! Everything is done. 🎉"
                            percent >= 70 -> "Keep Going! You are almost there."
                            percent >= 40 -> "Great progress, keep at it!"
                            else -> "Start small. You can do this! 🚀"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
