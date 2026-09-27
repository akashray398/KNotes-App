package com.example.knotes.ui.notes

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Html
import android.text.Layout
import android.text.Spannable
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.knotes.R
import com.example.knotes.domain.model.Note
import com.example.knotes.domain.model.Priority
import com.example.knotes.domain.model.Task
import com.example.knotes.databinding.BottomSheetAiAssistBinding
import com.example.knotes.databinding.FragmentEditNoteBinding
import com.example.knotes.ui.tasks.TasksViewModel
import com.example.knotes.util.ReminderManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
    private val aiViewModel: AiViewModel by viewModels()
    private val args: EditNoteFragmentArgs by navArgs()
    
    @javax.inject.Inject
    lateinit var settingsManager: com.example.knotes.util.SettingsManager

    @javax.inject.Inject
    lateinit var saveNoteVersionUseCase: com.example.knotes.domain.usecase.SaveNoteVersionUseCase

    private var currentNote: Note? = null
    
    private var reminderCalendar: Calendar? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    
    private var isFavorite = false
    private var selectedFolderId: Long? = null
    private var noteColor: Int = 0
    private var autoSaveJob: Job? = null
    
    private val undoStack = mutableListOf<String>()

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
        observeFolders()
        setupBackNavigation()
        observeAiState()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (hasUnsavedChanges()) {
                showDiscardDialog()
            } else {
                findNavController().navigateUp()
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            if (hasUnsavedChanges()) {
                showDiscardDialog()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        val currentTitle = binding.editTextTitle.text.toString().trim()
        val currentContent = Html.toHtml(binding.editTextDescription.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        
        return if (currentNote == null) {
            currentTitle.isNotEmpty() || binding.editTextDescription.text.isNotEmpty()
        } else {
            currentTitle != currentNote?.title || 
            currentContent != currentNote?.content ||
            isFavorite != currentNote?.isFavorite ||
            selectedFolderId != currentNote?.folderId ||
            noteColor != currentNote?.color
        }
    }

    private fun showDiscardDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Discard changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun observeFolders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.folders.collect { folders ->
                setupFolderChips(folders)
            }
        }
    }

    private fun setupFolderChips(folders: List<com.example.knotes.domain.model.Folder>) {
        binding.chipGroupFolder.removeAllViews()
        
        // "None" chip
        val noneChip = com.google.android.material.chip.Chip(requireContext()).apply {
            text = "None"
            isCheckable = true
            isChecked = selectedFolderId == null
            setOnClickListener { selectedFolderId = null; triggerAutoSave() }
        }
        binding.chipGroupFolder.addView(noneChip)

        folders.forEach { folder ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = folder.name
                isCheckable = true
                isChecked = selectedFolderId == folder.id
                setOnClickListener { selectedFolderId = folder.id; triggerAutoSave() }
            }
            binding.chipGroupFolder.addView(chip)
        }
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
            // Pin logic handled in NotesFragment mostly, but can toggle here
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
        binding.btnBold.setOnClickListener { pushToUndo(); toggleStyleSpan(Typeface.BOLD) }
        binding.btnItalic.setOnClickListener { pushToUndo(); toggleStyleSpan(Typeface.ITALIC) }
        binding.btnColorText.setOnClickListener { showColorPicker { color -> pushToUndo(); applySpan(ForegroundColorSpan(color)) } }
        binding.btnColorFill.setOnClickListener { showColorPicker { color -> pushToUndo(); applySpan(BackgroundColorSpan(color)) } }
        binding.btnFont.setOnClickListener { showFontPicker() }
        
        binding.btnAlignLeft.setOnClickListener { pushToUndo(); applyAlignment(Layout.Alignment.ALIGN_NORMAL) }
        binding.btnAlignCenter.setOnClickListener { pushToUndo(); applyAlignment(Layout.Alignment.ALIGN_CENTER) }
        binding.btnAlignRight.setOnClickListener { pushToUndo(); applyAlignment(Layout.Alignment.ALIGN_OPPOSITE) }
        
        binding.btnList.setOnClickListener { pushToUndo(); insertMarkdown("- ") }
        binding.btnChecklist.setOnClickListener { pushToUndo(); insertMarkdown("- [ ] ") }
        binding.btnUndo.setOnClickListener { performUndo() }
        binding.btnMic.setOnClickListener { checkAudioPermission() }
    }

    private fun toggleStyleSpan(style: Int) {
        val start = binding.editTextDescription.selectionStart
        val end = binding.editTextDescription.selectionEnd
        if (start == -1 || end == -1 || start == end) return

        val spannable = binding.editTextDescription.text
        val spans = spannable.getSpans(start, end, StyleSpan::class.java)
        var exists = false
        for (span in spans) {
            if (span.style == style) {
                spannable.removeSpan(span)
                exists = true
            }
        }
        if (!exists) {
            spannable.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applySpan(span: Any) {
        val start = binding.editTextDescription.selectionStart
        val end = binding.editTextDescription.selectionEnd
        if (start == -1 || end == -1 || start == end) return
        binding.editTextDescription.text.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun applyAlignment(alignment: Layout.Alignment) {
        val start = binding.editTextDescription.selectionStart
        val end = binding.editTextDescription.selectionEnd
        if (start == -1 || end == -1) return
        
        val spannable = binding.editTextDescription.text
        spannable.setSpan(
            AlignmentSpan.Standard(alignment),
            start, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        Snackbar.make(binding.root, "Alignment applied", Snackbar.LENGTH_SHORT).show()
    }

    private fun insertMarkdown(prefix: String) {
        val start = binding.editTextDescription.selectionStart
        val text = binding.editTextDescription.text
        if (start != -1) {
            text.insert(start, prefix)
        }
    }

    private fun pushToUndo() {
        val currentHtml = Html.toHtml(binding.editTextDescription.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        if (undoStack.isEmpty() || undoStack.last() != currentHtml) {
            undoStack.add(currentHtml)
            if (undoStack.size > 20) undoStack.removeAt(0)
        }
    }

    private fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val lastState = undoStack.removeAt(undoStack.size - 1)
            binding.editTextDescription.setText(Html.fromHtml(lastState, Html.FROM_HTML_MODE_COMPACT))
            Snackbar.make(binding.root, "Undo successful", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, "Nothing to undo", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showColorPicker(onColorSelected: (Int) -> Unit) {
        val colors = intArrayOf(
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CYAN,
            Color.BLACK, Color.WHITE, Color.GRAY, Color.DKGRAY,
            MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.MAGENTA)
        )
        val colorNames = arrayOf("Red", "Blue", "Green", "Yellow", "Magenta", "Cyan", "Black", "White", "Gray", "Dark Gray", "Theme Primary")
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Color")
            .setItems(colorNames) { _, which ->
                onColorSelected(colors[which])
            }
            .show()
    }

    private fun showFontPicker() {
        val fonts = arrayOf("monospace", "serif", "sans-serif", "cursive")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Font")
            .setItems(fonts) { _, which ->
                applySpan(TypefaceSpan(fonts[which]))
            }
            .show()
    }

    private fun updateMetadata() {
        val content = binding.editTextDescription.text.toString()
        val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
        binding.tvWordCount.text = getString(R.string.word_count, wordCount)
        
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
                    binding.editTextDescription.setText(
                        Html.fromHtml(it.content, Html.FROM_HTML_MODE_COMPACT)
                    )
                    binding.editTextTags.setText(it.tags.joinToString(", "))
                    isFavorite = it.isFavorite
                    selectedFolderId = it.folderId
                    noteColor = it.color
                    if (noteColor != 0) {
                        binding.root.setBackgroundColor(noteColor)
                    }
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
        binding.btnFavorite.iconTint =  ContextCompat.getColorStateList(requireContext(),
            if (isFavorite) R.color.priority_high else R.color.outline)
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
                    when {
                        text.lowercase().contains("set title") -> {
                            val title = text.lowercase().substringAfter("set title").trim()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            binding.editTextTitle.setText(title)
                            Snackbar.make(binding.root, "Title set from voice", Snackbar.LENGTH_SHORT).show()
                        }
                        text.lowercase().contains("add tag") -> {
                            val tag = text.lowercase().substringAfter("add tag").trim()
                            val currentTags = binding.editTextTags.text.toString()
                            val newTags = if (currentTags.isBlank()) tag else "$currentTags, $tag"
                            binding.editTextTags.setText(newTags)
                            Snackbar.make(binding.root, "Tag added from voice", Snackbar.LENGTH_SHORT).show()
                        }
                        text.lowercase().contains("set reminder") -> {
                            reminderCalendar = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                            }
                            updateReminderButtonText()
                            Snackbar.make(binding.root, "Reminder set for tomorrow 9 AM via voice", Snackbar.LENGTH_LONG).show()
                        }
                        else -> {
                            binding.editTextDescription.append(" $text")
                        }
                    }
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

    private val attachmentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            analyzeAttachment(it)
        }
    }

    private fun analyzeAttachment(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    binding.tvSaveStatus.text = "AI Analyzing Image..."
                    binding.tvSaveStatus.visibility = View.VISIBLE
                    aiViewModel.analyzeImage(bitmap, "Analyze this image and extract any text or summarize its contents.")
                } else {
                    Toast.makeText(requireContext(), "Unsupported attachment", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load attachment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAiAssistBottomSheet() {
        viewLifecycleOwner.lifecycleScope.launch {
            val consent = settingsManager.aiConsentGiven.first()
            if (!consent) {
                showAiConsentDialog()
            } else {
                val dialog = BottomSheetDialog(requireContext())
                val bottomSheetBinding = BottomSheetAiAssistBinding.inflate(layoutInflater)
                dialog.setContentView(bottomSheetBinding.root)

                bottomSheetBinding.cardFullSuggestion.setOnClickListener {
                    aiFullSuggestion()
                    dialog.dismiss()
                }

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

                bottomSheetBinding.cardAnalyzeAttachment.setOnClickListener {
                    attachmentLauncher.launch("image/*")
                    dialog.dismiss()
                }

                dialog.show()
            }
        }
    }

    private fun showAiConsentDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("AI Privacy Notice")
            .setMessage("KNotes AI uses Google Gemini to process your notes. By enabling this, your note content will be sent to external servers. No data is stored permanently by the AI service. Do you consent?")
            .setPositiveButton("I Consent") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    settingsManager.setAiConsentGiven(true)
                    settingsManager.setAiEnabled(true)
                    showAiAssistBottomSheet()
                }
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun aiFullSuggestion() {
        val currentTitle = binding.editTextTitle.text.toString()
        val currentContent = binding.editTextDescription.text.toString()
        aiViewModel.askQuestion(currentContent, "Suggest content for this note titled: $currentTitle")
    }

    private fun autoGenerateTitle() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return
        aiViewModel.generateTitle(content)
    }

    private fun summarizeNote() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return
        saveCurrentVersion("Before AI Summary")
        aiViewModel.summarize(content)
    }

    private fun improveWriting() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return
        saveCurrentVersion("Before AI Polish")
        aiViewModel.improveGrammar(content)
    }

    private fun saveCurrentVersion(name: String) {
        val noteId = currentNote?.id ?: return
        val title = binding.editTextTitle.text.toString()
        val content = Html.toHtml(binding.editTextDescription.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        
        viewLifecycleOwner.lifecycleScope.launch {
            saveNoteVersionUseCase(
                com.example.knotes.domain.model.NoteVersion(
                    noteId = noteId,
                    title = title,
                    content = content,
                    versionName = name
                )
            )
        }
    }

    private fun generateTagsFromAi() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return
        aiViewModel.generateTags(content)
    }

    private fun explainNote() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return
        aiViewModel.askQuestion(content, "Explain this note in simple terms.")
    }

    private fun extractTasksFromNote() {
        val content = binding.editTextDescription.text.toString()
        if (content.isBlank()) return
        aiViewModel.extractTasks(content)
    }

    private fun saveNote(navigateUp: Boolean) {
        val title = binding.editTextTitle.text.toString().trim()
        val descriptionSpannable = binding.editTextDescription.text
        val description = Html.toHtml(descriptionSpannable, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        val tagsString = binding.editTextTags.text.toString().trim()
        val tags = if (tagsString.isEmpty()) emptyList() else tagsString.split(",").map { it.trim().removePrefix("#") }
        
        val priority = when (binding.chipGroupPriority.checkedChipId) {
            R.id.chip_low -> Priority.LOW
            R.id.chip_high -> Priority.HIGH
            else -> Priority.MEDIUM
        }

        if (title.isEmpty() && descriptionSpannable.isEmpty()) return

        val note = currentNote?.copy(
            title = if (title.isEmpty()) "Untitled" else title,
            content = description,
            tags = tags,
            isFavorite = isFavorite,
            priority = priority,
            folderId = selectedFolderId,
            color = noteColor,
            reminderTime = reminderCalendar?.timeInMillis,
            updatedTime = System.currentTimeMillis()
        ) ?: Note(
            title = if (title.isEmpty()) "Untitled" else title,
            content = description,
            tags = tags,
            isFavorite = isFavorite,
            priority = priority,
            folderId = selectedFolderId,
            color = noteColor,
            reminderTime = reminderCalendar?.timeInMillis,
            createdTime = System.currentTimeMillis(),
            updatedTime = System.currentTimeMillis()
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

    private val exportTextLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val outputStream = requireContext().contentResolver.openOutputStream(it)
                    outputStream?.use { stream ->
                        val content = "${binding.editTextTitle.text}\n\n${binding.editTextDescription.text}"
                        stream.write(content.toByteArray())
                    }
                    Toast.makeText(requireContext(), "Note exported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
                }
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
                R.id.action_copy -> {
                    copyToClipboard()
                    true
                }
                R.id.action_export_text -> {
                    exportTextLauncher.launch("${binding.editTextTitle.text.toString().take(20)}.txt")
                    true
                }
                R.id.action_delete -> {
                    deleteNote()
                    true
                }
                R.id.action_color -> {
                    showNoteColorPicker()
                    true
                }
                R.id.action_history -> {
                    currentNote?.let {
                        val action = EditNoteFragmentDirections.actionEditNoteFragmentToVersionHistoryFragment(it.id)
                        findNavController().navigate(action)
                    } ?: run {
                        Toast.makeText(requireContext(), "Save the note first", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun copyToClipboard() {
        val clipboard = ContextCompat.getSystemService(requireContext(), android.content.ClipboardManager::class.java)
        val clip = android.content.ClipData.newPlainText("KNote", 
            "${binding.editTextTitle.text}\n\n${binding.editTextDescription.text}")
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showNoteColorPicker() {
        val colors = intArrayOf(
            0, // Default
            Color.parseColor("#FFF4F4"), // Light Red
            Color.parseColor("#F4FFF4"), // Light Green
            Color.parseColor("#F4F4FF"), // Light Blue
            Color.parseColor("#FFFFF4"), // Light Yellow
            Color.parseColor("#FFF4FF"), // Light Pink
            Color.parseColor("#F4FFFF")  // Light Cyan
        )
        val colorNames = arrayOf("Default", "Light Red", "Light Green", "Light Blue", "Light Yellow", "Light Pink", "Light Cyan")

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Note Color")
            .setItems(colorNames) { _, which ->
                noteColor = colors[which]
                if (noteColor != 0) {
                    binding.root.setBackgroundColor(noteColor)
                } else {
                    binding.root.setBackgroundColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, Color.WHITE))
                }
                triggerAutoSave()
            }
            .show()
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

    private fun observeAiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            aiViewModel.uiState.collect { state ->
                when (state) {
                    is AiViewModel.AiUiState.Loading -> {
                        binding.tvSaveStatus.text = "AI thinking..."
                        binding.tvSaveStatus.visibility = View.VISIBLE
                    }
                    is AiViewModel.AiUiState.Success -> {
                        binding.tvSaveStatus.visibility = View.GONE
                        binding.editTextDescription.append("\n\n--- AI Output ---\n${state.output}")
                        aiViewModel.resetState()
                    }
                    is AiViewModel.AiUiState.TagsGenerated -> {
                        binding.tvSaveStatus.visibility = View.GONE
                        val currentTags = binding.editTextTags.text.toString()
                        val newTags = if (currentTags.isBlank()) state.tags.joinToString(", ") 
                                      else "$currentTags, ${state.tags.joinToString(", ")}"
                        binding.editTextTags.setText(newTags)
                        aiViewModel.resetState()
                    }
                    is AiViewModel.AiUiState.TasksExtracted -> {
                        binding.tvSaveStatus.visibility = View.GONE
                        state.tasks.forEach { taskTitle ->
                            tasksViewModel.insertTask(Task(title = taskTitle))
                        }
                        Snackbar.make(binding.root, "${state.tasks.size} tasks extracted!", Snackbar.LENGTH_SHORT).show()
                        aiViewModel.resetState()
                    }
                    is AiViewModel.AiUiState.Error -> {
                        binding.tvSaveStatus.visibility = View.GONE
                        Toast.makeText(requireContext(), "AI Error: ${state.message}", Toast.LENGTH_SHORT).show()
                        aiViewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
}
