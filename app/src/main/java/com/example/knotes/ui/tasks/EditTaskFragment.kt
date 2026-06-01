package com.example.knotes.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.knotes.data.entity.Priority
import com.example.knotes.data.entity.Task
import com.example.knotes.databinding.FragmentEditTaskBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EditTaskFragment : Fragment() {

    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TasksViewModel by viewModels()
    private val args: EditTaskFragmentArgs by navArgs()
    private var currentTask: Task? = null
    private var selectedDeadline: Long = System.currentTimeMillis()
    private var selectedReminder: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskId = args.taskId
        if (taskId != -1) {
            loadTask(taskId)
        } else {
            updateDeadlineText()
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.buttonPickDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonSave.setOnClickListener {
            saveTask()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.editTextTitle.addTextChangedListener {
            binding.layoutTitle.error = null
        }
    }

    private fun loadTask(taskId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                currentTask = viewModel.getTaskById(taskId)
                currentTask?.let {
                    binding.editTextTitle.setText(it.title)
                    binding.editTextTags.setText(it.tags.joinToString(", "))
                    selectedDeadline = it.deadline ?: System.currentTimeMillis()
                    selectedReminder = it.reminderTime
                    updateDeadlineText()
                    when (it.priority) {
                        Priority.LOW -> binding.chipLow.isChecked = true
                        Priority.MEDIUM -> binding.chipMedium.isChecked = true
                        Priority.HIGH -> binding.chipHigh.isChecked = true
                    }
                    binding.buttonSave.text = "Update Task"
                    binding.toolbar.title = "Edit Task"
                } ?: run {
                    Toast.makeText(requireContext(), "Task not found", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading task", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Deadline")
            .setSelection(selectedDeadline)
            .build()

        datePicker.addOnPositiveButtonClickListener {
            selectedDeadline = it
            updateDeadlineText()
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun updateDeadlineText() {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.textViewDeadline.text = sdf.format(Date(selectedDeadline))
    }

    private fun saveTask() {
        val title = binding.editTextTitle.text.toString().trim()
        val tagsString = binding.editTextTags.text.toString().trim()
        val tags = if (tagsString.isEmpty()) emptyList() else tagsString.split(",").map { it.trim() }

        if (title.isEmpty()) {
            binding.layoutTitle.error = "Title is required"
            binding.editTextTitle.requestFocus()
            return
        }

        val priority = when (binding.chipGroupPriority.checkedChipId) {
            binding.chipLow.id -> Priority.LOW
            binding.chipHigh.id -> Priority.HIGH
            else -> Priority.MEDIUM
        }

        val task = currentTask?.copy(
            title = title,
            deadline = selectedDeadline,
            priority = priority,
            tags = tags,
            reminderTime = selectedReminder
        ) ?: Task(
            title = title,
            deadline = selectedDeadline,
            priority = priority,
            tags = tags,
            reminderTime = selectedReminder
        )

        try {
            if (currentTask == null) {
                viewModel.insertTask(task)
            } else {
                viewModel.updateTask(task)
            }
            findNavController().navigateUp()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Failed to save task", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
