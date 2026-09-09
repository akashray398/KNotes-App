package com.example.knotes.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.knotes.R
import com.example.knotes.databinding.FragmentSearchBinding
import com.google.android.material.search.SearchView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSearchView()
        observeViewModel()

        binding.searchBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerViews() {
        searchAdapter = SearchAdapter(
            onNoteClick = { noteId ->
                // Navigate to Note Detail
                val action = SearchFragmentDirections.actionSearchFragmentToEditNoteFragment(noteId)
                findNavController().navigate(action)
            },
            onTaskClick = { taskId ->
                // Navigate to Task Detail/Edit
                val action = SearchFragmentDirections.actionSearchFragmentToEditTaskFragment(taskId)
                findNavController().navigate(action)
            }
        )
        binding.searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        historyAdapter = HistoryAdapter(
            onItemClick = { query ->
                binding.searchView.setText(query)
                viewModel.setSearchQuery(query)
            },
            onDeleteClick = { query ->
                viewModel.deleteHistoryItem(query)
            }
        )
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.editText.addTextChangedListener { text ->
            viewModel.setSearchQuery(text.toString())
        }

        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWN) {
                // Focus and show keyboard if needed
            }
        }
        
        binding.searchView.editText.setOnEditorActionListener { v, actionId, event ->
            val query = binding.searchView.text.toString()
            if (query.isNotBlank()) {
                viewModel.saveSearch(query)
            }
            false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResults.collect { results ->
                        searchAdapter.submitList(results)
                        binding.emptySearchText.visibility = 
                            if (results.isEmpty() && viewModel.searchQuery.value.isNotEmpty()) 
                                View.VISIBLE 
                            else 
                                View.GONE
                    }
                }
                launch {
                    viewModel.searchHistory.collect { history ->
                        historyAdapter.submitList(history)
                        binding.recentSearchesHeader.visibility = 
                            if (history.isEmpty()) View.GONE else View.VISIBLE
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
