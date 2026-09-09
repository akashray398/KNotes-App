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
import com.example.knotes.R
import com.example.knotes.domain.model.Recurrence
import com.example.knotes.domain.model.Priority
import com.example.knotes.domain.model.Task
import com.example.knotes.databinding.FragmentEditTaskBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.example.knotes.util.TaskReminderManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
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
    private var selectedNoteId: Int? = null

    @javax.inject.Inject
    lateinit var taskReminderManager: com.example.knotes.util.TaskReminderManager

    @javax.inject.Inject
    lateinit var getNotesUseCase: com.example.knotes.domain.usecase.GetNotesUseCase

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
            selectedDeadline = getStartOfTomorrow()
            updateDeadlineText()
        }

        setupListeners()
    }

    private fun getStartOfTomorrow(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    private fun setupListeners() {
        binding.toolbar.inflateMenu(R.menu.menu_edit_note_more) // Reusing note menu for simplicity or create a task specific one
        // Better create a task specific one or just add delete to toolbar
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_delete -> {
                    deleteTask()
                    true
                }
                else -> false
            }
        }

        binding.buttonPickDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonPickTime.setOnClickListener {
            showTimePicker()
        }

        binding.switchReminders.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked && selectedReminder == null) {
                selectedReminder = selectedDeadline
                updateReminderText()
            } else if (!isChecked) {
                selectedReminder = null
            }
        }

        binding.buttonLinkNote.setOnClickListener {
            showNoteSelector()
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
                    selectedNoteId = it.relatedNoteId
                    
                    updateDeadlineText()
                    updateReminderText()
                    
                    binding.switchReminders.isChecked = selectedReminder != null
                    binding.layoutReminderTime.visibility = if (selectedReminder != null) View.VISIBLE else View.GONE

                    if (selectedNoteId != null && selectedNoteId != -1) {
                        updateNoteLinkText()
                    }

                    when (it.priority) {
                        Priority.LOW -> binding.chipLow.isChecked = true
                        Priority.MEDIUM -> binding.chipMedium.isChecked = true
                        Priority.HIGH -> binding.chipHigh.isChecked = true
                    }
                    when (it.recurrence) {
                        Recurrence.NONE -> binding.chipRepeatNone.isChecked = true
                        Recurrence.DAILY -> binding.chipRepeatDaily.isChecked = true
                        Recurrence.WEEKDAYS -> binding.chipRepeatWeekdays.isChecked = true
                        Recurrence.WEEKLY -> binding.chipRepeatWeekly.isChecked = true
                        else -> binding.chipRepeatNone.isChecked = true
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

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        selectedReminder?.let { calendar.timeInMillis = it }
        
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Select Reminder Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedDeadline
            cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            cal.set(Calendar.MINUTE, timePicker.minute)
            selectedReminder = cal.timeInMillis
            updateReminderText()
        }

        timePicker.show(parentFragmentManager, "TIME_PICKER")
    }

    private fun showNoteSelector() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notes = getNotesUseCase().first()
                val noteTitles = notes.map { it.title }.toTypedArray()
                
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Note to Link")
                    .setItems(noteTitles) { _, which ->
                        val selectedNote = notes[which]
                        selectedNoteId = selectedNote.id
                        updateNoteLinkText(selectedNote.title)
                    }
                    .setNeutralButton("Clear Link") { _, _ ->
                        selectedNoteId = null
                        binding.buttonLinkNote.text = "Select Note"
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load notes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNoteLinkText(title: String? = null) {
        if (title != null) {
            binding.buttonLinkNote.text = "Linked: $title"
        } else {
            selectedNoteId?.let { id ->
                 viewLifecycleOwner.lifecycleScope.launch {
                     try {
                         val notes = getNotesUseCase().first()
                         val note = notes.find { it.id == id }
                         binding.buttonLinkNote.text = note?.let { "Linked: ${it.title}" } ?: "Select Note"
                     } catch (e: Exception) {
                         binding.buttonLinkNote.text = "Linked to Note ID: $id"
                     }
                 }
            }
        }
    }

    private fun updateReminderText() {
        selectedReminder?.let {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            binding.textViewReminderTime.text = sdf.format(Date(it))
        } ?: run {
            binding.textViewReminderTime.text = "Not set"
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

        val recurrence = when (binding.chipGroupRecurrence.checkedChipId) {
            binding.chipRepeatDaily.id -> Recurrence.DAILY
            binding.chipRepeatWeekdays.id -> Recurrence.WEEKDAYS
            binding.chipRepeatWeekly.id -> Recurrence.WEEKLY
            else -> Recurrence.NONE
        }

        val taskToSave = currentTask?.copy(
            title = title,
            deadline = selectedDeadline,
            priority = priority,
            recurrence = recurrence,
            tags = tags,
            reminderTime = selectedReminder,
            relatedNoteId = selectedNoteId,
            updatedTime = System.currentTimeMillis()
        ) ?: Task(
            title = title,
            deadline = selectedDeadline,
            priority = priority,
            recurrence = recurrence,
            tags = tags,
            reminderTime = selectedReminder,
            relatedNoteId = selectedNoteId,
            createdTime = System.currentTimeMillis(),
            updatedTime = System.currentTimeMillis()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (currentTask == null) {
                    val id = viewModel.insertTask(taskToSave)
                    taskReminderManager.scheduleTaskReminders(taskToSave.copy(id = id.toInt()))
                } else {
                    viewModel.updateTask(taskToSave)
                    taskReminderManager.cancelTaskReminders(taskToSave)
                    taskReminderManager.scheduleTaskReminders(taskToSave)
                }
                findNavController().navigateUp()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Failed to save task", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteTask() {
        currentTask?.let {
            viewModel.deleteTask(it)
            findNavController().navigateUp()
        } ?: run {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
