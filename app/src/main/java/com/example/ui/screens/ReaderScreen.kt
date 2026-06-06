package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.TTSViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: TTSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val inputText by viewModel.inputText.collectAsState()
    val inputTitle by viewModel.inputTitle.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isTtsReady by viewModel.isTtsInitialized.collectAsState()
    val currentSentenceIndex by viewModel.currentSentenceIndex.collectAsState()
    val sentences by viewModel.sentences.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val supportedLanguages by viewModel.supportedLanguages.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }
    var showLanguageSheet by remember { mutableStateOf(false) }

    // Toggle for Editor vs Interactive Highlight Follow Mode
    var isFollowModeActive by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importTextFromUri(
                context = context,
                uri = it,
                onSuccess = {
                    Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                },
                onError = {
                    Toast.makeText(context, context.getString(R.string.import_fail), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Auto toggle to follow mode when starting TTS if text is long and has sentences
    LaunchedEffect(isPlaying) {
        if (isPlaying && sentences.isNotEmpty()) {
            isFollowModeActive = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Upper Quick Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // New / Clear Button
            TextButton(
                onClick = { viewModel.createNewDocument() },
                modifier = Modifier.testTag("new_doc_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "جديد", fontWeight = FontWeight.Bold)
            }

            // Tabs for switching View Mode: Editor vs Flow highlight
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.widthIn(max = 240.dp)
            ) {
                SegmentedButton(
                    selected = !isFollowModeActive,
                    onClick = { isFollowModeActive = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("محرر النصوص", fontSize = 12.sp)
                }
                SegmentedButton(
                    selected = isFollowModeActive,
                    onClick = {
                        if (inputText.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.empty_text_error), Toast.LENGTH_SHORT).show()
                        } else {
                            isFollowModeActive = true
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("المتابعة التفاعلية", fontSize = 12.sp)
                }
            }

            // File Import FAB/Icon
            IconButton(
                onClick = { filePickerLauncher.launch("text/plain") },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .size(40.dp)
                    .testTag("import_button")
            ) {
                Icon(
                    Icons.Outlined.UploadFile,
                    contentDescription = stringResource(R.string.import_file),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Active Load Badge if working on a saved entity
        if (inputTitle.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "العنوان النشط: $inputTitle",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Main Editor or Sentence Flow Box (Flex weights)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!isFollowModeActive) {
                // Editor View
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.input_hint),
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("text_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Paste button when empty
                if (inputText.isBlank()) {
                    Button(
                        onClick = {
                            clipboardManager.getText()?.text?.let {
                                viewModel.setInputText(it)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("paste_button")
                            .shadow(2.dp, RoundedCornerShape(50))
                            .clip(CircleShape),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("لصق من الحافظة", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Floating clear text button
                    IconButton(
                        onClick = { viewModel.setInputText("") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .testTag("clear_button")
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "مسح النص",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

            } else {
                // Interactive Sentences Follow Flow
                if (sentences.isEmpty() && inputText.isNotBlank()) {
                    // Split text if sentences list is raw empty
                    LaunchedEffect(inputText) {
                        viewModel.setInputText(inputText)
                    }
                }

                if (sentences.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("اكتب نصاً في المحرر لبدء المتابعة تفاعلياً.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val listState = rememberLazyListState()
                    // Auto scroll to active sentence
                    LaunchedEffect(currentSentenceIndex) {
                        if (currentSentenceIndex >= 0 && currentSentenceIndex < sentences.size) {
                            listState.animateScrollToItem(currentSentenceIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(sentences) { idx, sentence ->
                            val isActive = idx == currentSentenceIndex && isPlaying
                            val cardColor = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                            val textColor = if (isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            val border = if (isActive) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            }

                            Card(
                                onClick = { viewModel.selectSentence(idx) },
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                border = border,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sentence_item_$idx")
                                    .animateContentSize()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Bullet or Active Play head indicator
                                    if (isActive) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = "يقرأ حالياً",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                    }

                                    // Sentence string
                                    Text(
                                        text = sentence,
                                        color = textColor,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Customizer Config Sliders Block
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pitch and Speed in a Grid/Row configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Speed Control
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.speech_rate, speed),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = speed,
                            onValueChange = { viewModel.setSpeed(it) },
                            valueRange = 0.5f..2.5f,
                            steps = 3, // Snap intervals: 0.5, 1.0, 1.5, 2.0, 2.5
                            modifier = Modifier.testTag("speed_slider")
                        )
                    }

                    // Pitch Control
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.voice_pitch, pitch),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = pitch,
                            onValueChange = { viewModel.setPitch(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 2, // Snap intervals: 0.5, 1.0, 1.5, 2.0
                            modifier = Modifier.testTag("pitch_slider")
                        )
                    }
                }

                // Language Select Field
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageSheet = true }
                        .testTag("language_selector")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "لغة النطق: ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            // Display locale display name nicely
                            val displayName = selectedLanguage.getDisplayName(Locale("ar"))
                            Text(
                                text = displayName.ifEmpty { selectedLanguage.language },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Audio Playback Bar
        Surface(
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Button
                IconButton(
                    onClick = {
                        if (inputText.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.empty_text_error), Toast.LENGTH_SHORT).show()
                        } else {
                            saveTitleInput = inputTitle.ifEmpty { "" }
                            showSaveDialog = true
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .size(44.dp)
                        .testTag("save_button")
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = stringResource(R.string.btn_save),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Play / Pause main button
                val buttonIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                val buttonColor = if (isPlaying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                val contentDesc = if (isPlaying) stringResource(R.string.btn_pause) else stringResource(R.string.btn_play)

                IconButton(
                    onClick = {
                        if (inputText.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.empty_text_error), Toast.LENGTH_SHORT).show()
                        } else {
                            if (isPlaying) {
                                viewModel.pauseTTS()
                            } else {
                                viewModel.playTTS()
                                Toast.makeText(context, context.getString(R.string.tts_speech_started), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .background(buttonColor, CircleShape)
                        .size(56.dp)
                        .testTag("play_button")
                ) {
                    Icon(
                        buttonIcon,
                        contentDescription = contentDesc,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Stop Button
                IconButton(
                    onClick = {
                        viewModel.stopTTS()
                        Toast.makeText(context, context.getString(R.string.tts_speech_stopped), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .size(44.dp)
                        .testTag("stop_button")
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = stringResource(R.string.btn_stop),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Language Chooser Dialog Sheet
        if (showLanguageSheet) {
            Dialog(onDismissRequest = { showLanguageSheet = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "اختر لغة القارئ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showLanguageSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "اغلاق")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val sortedLangs = supportedLanguages.sortedBy { it.getDisplayName(Locale("ar")) }
                            itemsIndexed(sortedLangs) { _, locale ->
                                val active = locale.language == selectedLanguage.language
                                val name = locale.getDisplayName(Locale("ar")).replaceFirstChar { it.uppercase() }

                                val containerColor = if (active) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                                val textColor = if (active) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }

                                Surface(
                                    onClick = {
                                        viewModel.setLanguage(locale)
                                        showLanguageSheet = false
                                    },
                                    color = containerColor,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("lang_${locale.language}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name.ifEmpty { locale.language },
                                            color = textColor,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (active) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "محدد",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Save Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.title_dialog),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        placeholder = { Text(text = stringResource(R.string.text_title_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_title_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveCurrentText(saveTitleInput)
                            showSaveDialog = false
                            Toast.makeText(context, "تم حفظ النص بنجاح في السجل", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_confirm_button")
                    ) {
                        Text(text = stringResource(R.string.btn_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSaveDialog = false }
                    ) {
                        Text(text = stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}
