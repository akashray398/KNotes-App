package com.example.knotes.ui.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.knotes.R
import com.example.knotes.databinding.FragmentFoldersBinding
import com.example.knotes.domain.model.Folder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FoldersViewModel by viewModels()
    private lateinit var adapter: FoldersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupToolbar()
        observeViewModel()

        binding.fabAddFolder.setOnClickListener {
            showFolderDialog()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = FoldersAdapter(
            onEditClick = { folder ->
                showFolderDialog(folder)
            },
            onDeleteClick = { folder ->
                showDeleteConfirmation(folder)
            }
        )
        binding.foldersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@FoldersFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.folders.collect { folders ->
                    adapter.submitList(folders)
                }
            }
        }
    }

    private fun showFolderDialog(folder: Folder? = null) {
        val builder = AlertDialog.Builder(requireContext())
        val title = if (folder == null) R.string.add_folder else R.string.edit_folder
        builder.setTitle(title)

        val input = EditText(requireContext())
        input.hint = getString(R.string.folder_name)
        if (folder != null) {
            input.setText(folder.name)
        }
        builder.setView(input)

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val name = input.text.toString()
            if (name.isNotBlank()) {
                if (folder == null) {
                    viewModel.addFolder(name)
                } else {
                    viewModel.updateFolder(folder.copy(name = name))
                }
            }
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    private fun showDeleteConfirmation(folder: Folder) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_folder)
            .setMessage("Are you sure you want to delete this folder? Notes in this folder will not be deleted.")
            .setPositiveButton(R.string.delete_folder) { _, _ ->
                viewModel.deleteFolder(folder)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
