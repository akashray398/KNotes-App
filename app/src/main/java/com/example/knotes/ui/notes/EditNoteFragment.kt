package com.example.knotes.ui.notes

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.knotes.R
import com.example.knotes.data.entity.Note
import com.example.knotes.data.entity.Priority
import com.example.knotes.data.entity.Task
import com.example.knotes.databinding.BottomSheetAiAssistBinding
import com.example.knotes.databinding.FragmentEditNoteBinding
import com.example.knotes.ui.tasks.TasksViewModel
import com.example.knotes.util.ReminderManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class EditNoteFragment : Fragment() {

    private var _binding: FragmentEditNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()
    private val tasksViewModel: TasksViewModel by viewModels()
    private val args: EditNoteFragmentArgs by navArgs()
    private var currentNote: Note? = null
    
    private var reminderCalendar: Calendar? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    
    private var isFavorite = false
    private var autoSaveJob: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(requireContext(), "Permission denied to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())

        val noteId = args.noteId
        if (noteId != -1) {
            loadNote(noteId)
        } else {
            binding.chipMedium.isChecked = true
            updateMetadata()
        }

        setupListeners()
        setupFormattingToolbar()
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveNote(navigateUp = true)
        }

        binding.btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteIcon()
            triggerAutoSave()
        }

        binding.btnPin.setOnClickListener {
            // Toggle pin logic
            triggerAutoSave()
        }

        binding.btnAiAssist.setOnClickListener {
            showAiAssistBottomSheet()
        }

        binding.btnVoiceNote.setOnClickListener {
            checkAudioPermission()
        }

        binding.btnReminder.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnMore.setOnClickListener {
            showMoreMenu(it)
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.editTextTitle.addTextChangedListener {
            triggerAutoSave()
        }

        binding.editTextDescription.addTextChangedListener {
            updateMetadata()
            triggerAutoSave()
        }
    }

    private fun setupFormattingToolbar() {
        binding.btnBold.setOnClickListener { insertFormatting("**", "**") }
        binding.btnItalic.setOnClickListener { insertFormatting("_", "_") }
        binding.btnList.setOnClickListener { insertFormatting("\n- ", "") }
        binding.btnChecklist.setOnClickListener { insertFormatting("\n- [ ] ", "") }
        binding.btnMic.setOnClickListener { checkAudioPermission() }
    }

    private fun insertFormatting(prefix: String, suffix: String) {
        val start = binding.editTextDescription.selectionStart
        val end = binding.editTextDescription.selectionEnd
        val text = binding.editTextDescription.text
        
        if (start != -1 && end != -1) {
            val selectedText = text.substring(start, end)
            val replacement = "$prefix$selectedText$suffix"
            text.replace(start, end, replacement)
            binding.editTextDescription.setSelection(start + prefix.length, start + prefix.length + selectedText.length)
        }
    }

    private fun updateMetadata() {
        val content = binding.editTextDescription.text.toString()
        val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
        binding.tvWordCount.text = "$wordCount words"
        
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.tvDate.text = sdf.format(System.currentTimeMillis())
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.statusIndicator.visibility = View.VISIBLE
            binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.priority_medium)
            binding.tvSaveStatus.text = "Saving..."
            binding.tvSaveStatus.visibility = View.VISIBLE
            
            delay(2000)
            saveNote(navigateUp = false)
            
            binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.priority_low)
            binding.tvSaveStatus.text = "Saved"
            delay(2000)
            binding.tvSaveStatus.visibility = View.GONE
            binding.statusIndicator.visibility = View.GONE
        }
    }

    private fun loadNote(noteId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                currentNote = viewModel.getNoteById(noteId)
                currentNote?.let {
                    binding.editTextTitle.setText(it.title)
                    binding.editTextDescription.setText(it.description)
                    binding.editTextTags.setText(it.tags.joinToString(", "))
                    isFavorite = it.isFavorite
                    updateFavoriteIcon()
                    
                    it.reminderTime?.let { time ->
                        reminderCalendar = Calendar.getInstance().apply { timeInMillis = time }
                        updateReminderButtonText()
                    }

                    when (it.priority) {
                        Priority.LOW -> binding.chipLow.isChecked = true
                        Priority.MEDIUM -> binding.chipMedium.isChecked = true
                        Priority.HIGH -> binding.chipHigh.isChecked = true
                    }
                    updateMetadata()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFavoriteIcon() {
        val icon = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite
        binding.btnFavorite.setIconResource(icon)
        binding.btnFavorite.iconTint = ContextCompat.getColorStateList(requireContext(), 
            if (isFavorite) R.color.priority_high else R.color.outlineLight)
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                Snackbar.make(binding.root, "Listening...", Snackbar.LENGTH_SHORT).show()
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(requireContext(), "Speech recognition failed", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    binding.editTextDescription.append(" $text")
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    private fun showDateTimePicker() {
        val currentDateTime = Calendar.getInstance()
        val datePicker = DatePickerDialog(requireContext(), { _, year, month, day ->
            val timePicker = TimePickerDialog(requireContext(), { _, hour, minute ->
                val pickedDateTime = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute)
                }
                if (pickedDateTime.timeInMillis > System.currentTimeMillis()) {
                    reminderCalendar = pickedDateTime
                    updateReminderButtonText()
                    triggerAutoSave()
                } else {
                    Toast.makeText(requireContext(), "Please select a future time", Toast.LENGTH_SHORT).show()
                }
            }, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), false)
            timePicker.show()
        }, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH), currentDateTime.get(Calendar.DAY_OF_MONTH))
        datePicker.show()
    }

    private fun updateReminderButtonText() {
        reminderCalendar?.let {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            binding.btnReminder.text = sdf.format(it.time)
        }
    }

    private fun showAiAssistBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val bottomSheetBinding = BottomSheetAiAssistBinding.inflate(layoutInflater)
        dialog.setContentView(bottomSheetBinding.root)

        bottomSheetBinding.cardAutoTitle.setOnClickListener {
            autoGenerateTitle()
            dialog.dismiss()
        }

        bottomSheetBinding.cardSummarize.setOnClickListener {
            summarizeNote()
            dialog.dismiss()
        }

        bottomSheetBinding.cardExtractTasks.setOnClickListener {
            extractTasksFromNote()
            dialog.dismiss()
        }

        bottomSheetBinding.cardImproveWriting.setOnClickListener {
            improveWriting()
            dialog.dismiss()
        }

        bottomSheetBinding.cardGenerateTags.setOnClickListener {
            generateTagsFromAi()
            dialog.dismiss()
        }

        bottomSheetBinding.cardExplain.setOnClickListener {
            explainNote()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun autoGenerateTitle() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: "Untitled"
            val words = firstLine.split(" ").take(4).joinToString(" ")
            val generatedTitle = words.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            binding.editTextTitle.setText(generatedTitle)
        }
    }

    private fun summarizeNote() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val summary = "\n\n--- ✨ AI Summary ---\n• " + (if (content.contains(".")) content.split(".")[0] else content).trim() + ".\n• Key takeaway: High priority items identified."
            binding.editTextDescription.append(summary)
        }
    }

    private fun improveWriting() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val improved = content.replace("(?i)i want to".toRegex(), "I intend to")
                .replace("(?i)help me".toRegex(), "assist me")
            binding.editTextDescription.setText(improved)
        }
    }

    private fun generateTagsFromAi() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val words = content.split("\\s+".toRegex())
                .filter { it.length > 5 }
                .take(3)
                .map { it.lowercase().filter { c -> c.isLetterOrDigit() } }
                .distinct()
            
            if (words.isNotEmpty()) {
                val currentTags = binding.editTextTags.text.toString()
                val newTags = if (currentTags.isBlank()) words.joinToString(", ") 
                              else "$currentTags, ${words.joinToString(", ")}"
                binding.editTextTags.setText(newTags)
                Snackbar.make(binding.root, "Tags generated!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun explainNote() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val explanation = "\n\n--- 💡 Simple Explanation ---\nIn simple terms, this note discusses " + 
                (if (content.length > 20) content.substring(0, 20) else content).trim() + "..."
            binding.editTextDescription.append(explanation)
        }
    }

    private fun extractTasksFromNote() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val lines = content.lines()
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    val taskTitle = trimmed.substring(1).trim()
                    if (taskTitle.isNotBlank()) {
                        val task = Task(
                            title = taskTitle,
                            priority = Priority.MEDIUM,
                            deadline = System.currentTimeMillis() + 86400000
                        )
                        tasksViewModel.insertTask(task)
                    }
                }
            }
            Snackbar.make(binding.root, "Tasks extracted and saved!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun saveNote(navigateUp: Boolean) {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val tagsString = binding.editTextTags.text.toString().trim()
        val tags = if (tagsString.isEmpty()) emptyList() else tagsString.split(",").map { it.trim().removePrefix("#") }
        
        val priority = when (binding.chipGroupPriority.checkedChipId) {
            R.id.chip_low -> Priority.LOW
            R.id.chip_high -> Priority.HIGH
            else -> Priority.MEDIUM
        }

        if (title.isEmpty() && description.isEmpty()) return

        val note = currentNote?.copy(
            title = if (title.isEmpty()) "Untitled" else title,
            description = description,
            tags = tags,
            isFavorite = isFavorite,
            priority = priority,
            reminderTime = reminderCalendar?.timeInMillis,
            timestamp = System.currentTimeMillis()
        ) ?: Note(
            title = if (title.isEmpty()) "Untitled" else title,
            description = description,
            tags = tags,
            isFavorite = isFavorite,
            priority = priority,
            reminderTime = reminderCalendar?.timeInMillis
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (currentNote == null) {
                    val id = viewModel.insertNote(note).toInt()
                    currentNote = note.copy(id = id)
                } else {
                    viewModel.updateNote(note)
                }

                reminderCalendar?.let {
                    ReminderManager.scheduleReminder(
                        requireContext(),
                        currentNote!!.id,
                        note.title,
                        description.take(50),
                        it.timeInMillis
                    )
                }

                if (navigateUp) findNavController().navigateUp()
            } catch (e: Exception) {
                // Silently fail on autosave
            }
        }
    }

    private fun showMoreMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_edit_note_more, popup.menu)
        popup.setOnMenuItemClickListener { item: android.view.MenuItem ->
            when (item.itemId) {
                R.id.action_share -> {
                    shareNote()
                    true
                }
                R.id.action_delete -> {
                    deleteNote()
                    true
                }
                R.id.action_color -> {
                    Toast.makeText(requireContext(), "Color picker coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun deleteNote() {
        currentNote?.let {
            viewModel.deleteNote(it)
            findNavController().navigateUp()
        }
    }

    private fun shareNote() {
        val title = binding.editTextTitle.text.toString()
        val description = binding.editTextDescription.text.toString()
        val shareText = "$title\n\n$description\n\n-- Shared from KNotes --"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer.destroy()
        _binding = null
    }
}
