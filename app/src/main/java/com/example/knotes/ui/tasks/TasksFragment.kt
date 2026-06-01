package com.example.knotes.ui.tasks

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
import com.example.knotes.data.entity.Priority
import com.example.knotes.databinding.FragmentTasksBinding
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

        setupRecyclerViews()
        setupFab()
        setupSearch()
        setupFilters()
        setupToolbarActions()
        setupSwipeActions()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        adapter = TasksAdapter(
            onTaskClick = { task -> navigateToEdit(task.id) },
            onTaskCheckedChange = { task -> viewModel.toggleTaskCompletion(task) }
        )
        binding.recyclerViewTasks.adapter = adapter
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())

        completedAdapter = TasksAdapter(
            onTaskClick = { task -> navigateToEdit(task.id) },
            onTaskCheckedChange = { task -> viewModel.toggleTaskCompletion(task) }
        )
        binding.recyclerViewCompletedTasks.adapter = completedAdapter
        binding.recyclerViewCompletedTasks.layoutManager = LinearLayoutManager(requireContext())

        searchAdapter = TasksAdapter(
            onTaskClick = { task ->
                binding.searchView.hide()
                navigateToEdit(task.id)
            },
            onTaskCheckedChange = { task -> viewModel.toggleTaskCompletion(task) }
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

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener { navigateToEdit(-1) }
        binding.btnCreateFirstTask.setOnClickListener { navigateToEdit(-1) }
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

    private fun setupFilters() {
        val filterOptions = listOf("All", "Low", "Medium", "High", "Completed", "Overdue")
        filterOptions.forEach { option ->
            val chip = Chip(requireContext()).apply {
                text = option
                isCheckable = true
                isChecked = option == "All"
                setEnsureMinTouchTargetSize(false)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
                setOnClickListener {
                    val priority = when (option) {
                        "Low" -> Priority.LOW
                        "Medium" -> Priority.MEDIUM
                        "High" -> Priority.HIGH
                        else -> null
                    }
                    viewModel.updatePriorityFilter(priority)
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
                        binding.progressIndicator.progress = percent
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
