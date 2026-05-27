package com.example.workouttracker.ui.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.data.ExerciseEntity
import com.example.workouttracker.data.ExerciseType
import com.example.workouttracker.data.LastWorkoutInfo
import com.example.workouttracker.data.WorkoutSetWithExercise
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val statDateFormatter = DateTimeFormatter.ofPattern("dd MMM")

@Composable
fun TrackExerciseRoute(
    exerciseId: Long,
    workoutDate: LocalDate,
    supersetExerciseIds: List<Long>,
    supersetIndex: Int,
    supersetGroupId: String,
    supersetRound: Int,
    onSupersetAdvance: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: TrackExerciseViewModel = viewModel(
        key = "track-$exerciseId-${workoutDate}",
        factory = TrackExerciseViewModel.factory(
            exerciseId = exerciseId,
            workoutDate = workoutDate,
            supersetGroupId = supersetGroupId,
            supersetRound = supersetRound,
            supersetIndex = supersetIndex
        )
    )

    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val todaySets by viewModel.todaySets.collectAsStateWithLifecycle()
    val historySets by viewModel.historySets.collectAsStateWithLifecycle()
    val lastWorkout by viewModel.lastWorkout.collectAsStateWithLifecycle()
    val weightInput by viewModel.weightInput.collectAsStateWithLifecycle()
    val repsInput by viewModel.repsInput.collectAsStateWithLifecycle()
    val durationInput by viewModel.durationInput.collectAsStateWithLifecycle()
    val distanceInput by viewModel.distanceInput.collectAsStateWithLifecycle()
    val commentInput by viewModel.commentInput.collectAsStateWithLifecycle()
    val chartMetric by viewModel.chartMetric.collectAsStateWithLifecycle()
    val personalRecordSetIds by viewModel.personalRecordSetIds.collectAsStateWithLifecycle()
    val setTrackedSignal by viewModel.setTrackedSignal.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val content = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(content)
        viewModel.clearMessage()
    }

    LaunchedEffect(setTrackedSignal, supersetExerciseIds, supersetIndex) {
        if (setTrackedSignal > 0L && supersetExerciseIds.size > 1 && supersetIndex >= 0) {
            onSupersetAdvance()
        }
    }

    TrackExerciseScreen(
        exercise = exercise,
        todaySets = todaySets,
        historySets = historySets,
        lastWorkout = lastWorkout,
        weightInput = weightInput,
        repsInput = repsInput,
        durationInput = durationInput,
        distanceInput = distanceInput,
        commentInput = commentInput,
        chartMetric = chartMetric,
        personalRecordSetIds = personalRecordSetIds,
        workoutDate = workoutDate,
        supersetExerciseIds = supersetExerciseIds,
        supersetIndex = supersetIndex,
        supersetRound = supersetRound,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onWeightChange = viewModel::onWeightChange,
        onRepsChange = viewModel::onRepsChange,
        onDurationChange = viewModel::onDurationChange,
        onDistanceChange = viewModel::onDistanceChange,
        onCommentChange = viewModel::onCommentChange,
        onAdjustWeight = viewModel::adjustWeight,
        onAdjustReps = viewModel::adjustReps,
        onTrackSet = viewModel::trackSet,
        onSetChartMetric = viewModel::setChartMetric,
        onUpdateSetComment = viewModel::updateSetComment,
        onClearSetComment = viewModel::clearSetComment,
        onDeleteSet = viewModel::deleteSet,
        onUpdateWeightIncrement = viewModel::updateWeightIncrement
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrackExerciseScreen(
    exercise: ExerciseEntity?,
    todaySets: List<WorkoutSetWithExercise>,
    historySets: List<WorkoutSetWithExercise>,
    lastWorkout: LastWorkoutInfo?,
    weightInput: String,
    repsInput: String,
    durationInput: String,
    distanceInput: String,
    commentInput: String,
    chartMetric: ChartMetric,
    personalRecordSetIds: Set<Long>,
    workoutDate: LocalDate,
    supersetExerciseIds: List<Long>,
    supersetIndex: Int,
    supersetRound: Int,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onAdjustWeight: (Boolean) -> Unit,
    onAdjustReps: (Boolean) -> Unit,
    onTrackSet: () -> Unit,
    onSetChartMetric: (ChartMetric) -> Unit,
    onUpdateSetComment: (Long, String) -> Unit,
    onClearSetComment: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onUpdateWeightIncrement: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showIncrementDialog by remember { mutableStateOf(false) }
    var incrementInput by remember(exercise?.weightIncrement) {
        mutableStateOf(formatDecimal(exercise?.weightIncrement ?: 5.0))
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = exercise?.name ?: "Track exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Set weight increment") },
                            onClick = {
                                showMenu = false
                                showIncrementDialog = true
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (exercise == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading exercise...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Swipe left to view history, then left again for statistics.",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = "Workout date: ${workoutDate.format(statDateFormatter)}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (supersetExerciseIds.size > 1 && supersetIndex >= 0) {
                Text(
                    text = "Superset: round $supersetRound | exercise ${supersetIndex + 1} of ${supersetExerciseIds.size}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> TrackPage(
                        exercise = exercise,
                        todaySets = todaySets,
                        lastWorkout = lastWorkout,
                        weightInput = weightInput,
                        repsInput = repsInput,
                        durationInput = durationInput,
                        distanceInput = distanceInput,
                        commentInput = commentInput,
                        onWeightChange = onWeightChange,
                        onRepsChange = onRepsChange,
                        onDurationChange = onDurationChange,
                        onDistanceChange = onDistanceChange,
                        onCommentChange = onCommentChange,
                        onAdjustWeight = onAdjustWeight,
                        onAdjustReps = onAdjustReps,
                        personalRecordSetIds = personalRecordSetIds,
                        onTrackSet = onTrackSet,
                        onUpdateSetComment = onUpdateSetComment,
                        onClearSetComment = onClearSetComment,
                        onDeleteSet = onDeleteSet
                    )

                    1 -> HistoryPage(
                        historySets = historySets,
                        personalRecordSetIds = personalRecordSetIds
                    )

                    else -> StatsPage(
                        historySets = historySets,
                        selectedMetric = chartMetric,
                        onSelectMetric = onSetChartMetric,
                        exerciseType = exercise.type
                    )
                }
            }
        }
    }

    if (showIncrementDialog) {
        AlertDialog(
            onDismissRequest = { showIncrementDialog = false },
            title = { Text(text = "Weight increment") },
            text = {
                OutlinedTextField(
                    value = incrementInput,
                    onValueChange = { incrementInput = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(text = "Increment") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateWeightIncrement(incrementInput)
                        showIncrementDialog = false
                    }
                ) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncrementDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun TrackPage(
    exercise: ExerciseEntity,
    todaySets: List<WorkoutSetWithExercise>,
    lastWorkout: LastWorkoutInfo?,
    weightInput: String,
    repsInput: String,
    durationInput: String,
    distanceInput: String,
    commentInput: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onAdjustWeight: (Boolean) -> Unit,
    onAdjustReps: (Boolean) -> Unit,
    personalRecordSetIds: Set<Long>,
    onTrackSet: () -> Unit,
    onUpdateSetComment: (Long, String) -> Unit,
    onClearSetComment: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit
) {
    var selectedSet by remember { mutableStateOf<WorkoutSetWithExercise?>(null) }
    var setPendingDelete by remember { mutableStateOf<WorkoutSetWithExercise?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LastWorkoutCard(lastWorkout = lastWorkout, type = exercise.type)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Track set",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (exercise.type.usesWeight) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { onAdjustWeight(false) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                            }
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = weightInput,
                                onValueChange = onWeightChange,
                                label = { Text(text = "Weight (kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            IconButton(onClick = { onAdjustWeight(true) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                            }
                        }
                    }

                    if (exercise.type.usesReps) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { onAdjustReps(false) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease reps")
                            }
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = repsInput,
                                onValueChange = onRepsChange,
                                label = { Text(text = "Reps") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            IconButton(onClick = { onAdjustReps(true) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase reps")
                            }
                        }
                    }

                    if (exercise.type.usesTime) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = durationInput,
                            onValueChange = onDurationChange,
                            label = { Text(text = "Time (seconds)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    if (exercise.type.usesDistance) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = distanceInput,
                            onValueChange = onDistanceChange,
                            label = { Text(text = "Distance (km)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = commentInput,
                        onValueChange = onCommentChange,
                        label = { Text(text = "Set comment (optional)") },
                        minLines = 2
                    )

                    TextButton(onClick = onTrackSet, modifier = Modifier.align(Alignment.End)) {
                        Text(text = "Track set")
                    }
                }
            }
        }

        item {
            Text(
                text = "Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (todaySets.isEmpty()) {
            item {
                Text(
                    text = "No sets recorded for this exercise today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(todaySets, key = { it.set.id }) { workoutSet ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set ${workoutSet.set.sequenceInExercise}: ${formatSetSummary(exercise.type, workoutSet)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = formatTime(workoutSet.set.performedAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (personalRecordSetIds.contains(workoutSet.set.id)) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Personal record",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        IconButton(onClick = { selectedSet = workoutSet }) {
                            val hasComment = !workoutSet.set.comment.isNullOrBlank()
                            Icon(
                                imageVector = if (hasComment) Icons.Default.Comment else Icons.Default.ChatBubbleOutline,
                                contentDescription = "Comment",
                                tint = if (hasComment) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        IconButton(onClick = { setPendingDelete = workoutSet }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete set",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedSet != null) {
        CommentDialog(
            initialComment = selectedSet?.set?.comment.orEmpty(),
            onDismiss = { selectedSet = null },
            onSave = { comment ->
                selectedSet?.let { onUpdateSetComment(it.set.id, comment) }
                selectedSet = null
            },
            onDelete = {
                selectedSet?.let { onClearSetComment(it.set.id) }
                selectedSet = null
            }
        )
    }

    setPendingDelete?.let { pendingSet ->
        AlertDialog(
            onDismissRequest = { setPendingDelete = null },
            title = { Text(text = "Delete set") },
            text = {
                Text(
                    text = "Delete Set ${pendingSet.set.sequenceInExercise}: " +
                        formatSetSummary(exercise.type, pendingSet) +
                        "?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSet(pendingSet.set.id)
                        setPendingDelete = null
                    }
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { setPendingDelete = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun LastWorkoutCard(lastWorkout: LastWorkoutInfo?, type: ExerciseType) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Last time performed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (lastWorkout == null) {
                Text(
                    text = "No previous workout data.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Date: ${lastWorkout.date}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Exercise order: ${lastWorkout.exerciseOrder ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                lastWorkout.sets.forEachIndexed { index, set ->
                    Text(
                        text = "Set ${index + 1}: ${formatSetSummary(type, set)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentDialog(
    initialComment: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var comment by remember(initialComment) { mutableStateOf(initialComment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Set comment") },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = comment,
                onValueChange = { comment = it },
                minLines = 3,
                label = { Text(text = "Comment") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(comment) }) {
                Text(text = "Edit")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(text = "Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel")
                }
            }
        }
    )
}

@Composable
private fun HistoryPage(
    historySets: List<WorkoutSetWithExercise>,
    personalRecordSetIds: Set<Long>
) {
    val grouped = historySets.groupBy { formatDate(it.set.performedAtMillis) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (historySets.isEmpty()) {
            item {
                Text(
                    text = "No history yet for this exercise.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        grouped.forEach { (date, sets) ->
            item(key = date) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(sets, key = { it.set.id }) { workoutSet ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatSetSummary(workoutSet.exerciseType, workoutSet),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = formatTime(workoutSet.set.performedAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (personalRecordSetIds.contains(workoutSet.set.id)) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Personal record",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            if (!workoutSet.set.comment.isNullOrBlank()) {
                                Icon(
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = "Has comment",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPage(
    historySets: List<WorkoutSetWithExercise>,
    selectedMetric: ChartMetric,
    onSelectMetric: (ChartMetric) -> Unit,
    exerciseType: ExerciseType
) {
    val oneRepSeries = buildMetricSeries(historySets, ChartMetric.ONE_REP_MAX)
    val selectedSeries = buildMetricSeries(historySets, selectedMetric)

    val latestOneRep = oneRepSeries.lastOrNull()
    val allTimeOneRep = oneRepSeries.maxByOrNull { it.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "1RM snapshots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!exerciseType.usesWeight || !exerciseType.usesReps) {
                    Text(
                        text = "1RM is available only for exercises with weight and reps.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Last 1RM: ${latestOneRep?.let { "${formatDecimal(it.value)} kg (${it.date.format(statDateFormatter)})" } ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "All-time 1RM: ${allTimeOneRep?.let { "${formatDecimal(it.value)} kg (${it.date.format(statDateFormatter)})" } ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartMetric.entries.forEach { metric ->
                FilterChip(
                    selected = selectedMetric == metric,
                    onClick = { onSelectMetric(metric) },
                    label = { Text(metric.label) }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Progress chart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                MetricLineChart(points = selectedSeries)
            }
        }
    }
}

@Composable
private fun MetricLineChart(points: List<StatPoint>) {
    if (points.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data points yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxValue = points.maxOf { it.value }
    val minValue = points.minOf { it.value }
    val midValue = (maxValue + minValue) / 2.0
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val accentColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    var selectedPoint by remember(points) { mutableStateOf<StatPoint?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(text = formatDecimal(maxValue), style = MaterialTheme.typography.labelSmall)
            Text(text = formatDecimal(midValue), style = MaterialTheme.typography.labelSmall)
            Text(text = formatDecimal(minValue), style = MaterialTheme.typography.labelSmall)
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .pointerInput(points, maxValue, minValue, range) {
                    detectTapGestures { tapOffset ->
                        val leftPadding = 12f
                        val rightPadding = 12f
                        val topPadding = 12f
                        val bottomPadding = 28f

                        val chartWidth = size.width - leftPadding - rightPadding
                        val chartHeight = size.height - topPadding - bottomPadding

                        if (chartWidth <= 0f || chartHeight <= 0f) {
                            selectedPoint = null
                            return@detectTapGestures
                        }

                        val positions = points.mapIndexed { index, point ->
                            val x = if (points.size == 1) {
                                leftPadding + chartWidth / 2f
                            } else {
                                leftPadding + (chartWidth * index / (points.size - 1))
                            }
                            val y = topPadding + (((maxValue - point.value) / range) * chartHeight).toFloat()
                            point to Offset(x, y)
                        }

                        val nearest = positions.minByOrNull { (_, pointOffset) ->
                            val dx = pointOffset.x - tapOffset.x
                            val dy = pointOffset.y - tapOffset.y
                            (dx * dx) + (dy * dy)
                        }

                        if (nearest != null) {
                            val distanceSquared =
                                (nearest.second.x - tapOffset.x) * (nearest.second.x - tapOffset.x) +
                                    (nearest.second.y - tapOffset.y) * (nearest.second.y - tapOffset.y)
                            selectedPoint = if (distanceSquared <= (28f * 28f)) nearest.first else null
                        } else {
                            selectedPoint = null
                        }
                    }
                }
        ) {
            val leftPadding = 12f
            val rightPadding = 12f
            val topPadding = 12f
            val bottomPadding = 28f

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding

            repeat(4) { index ->
                val y = topPadding + (chartHeight / 3f) * index
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(leftPadding + chartWidth, y),
                    strokeWidth = 1f
                )
            }

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = if (points.size == 1) {
                    leftPadding + chartWidth / 2f
                } else {
                    leftPadding + (chartWidth * index / (points.size - 1))
                }
                val y = topPadding + (((maxValue - point.value) / range) * chartHeight).toFloat()
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                drawCircle(
                    color = if (point == selectedPoint) {
                        selectedColor
                    } else {
                        accentColor
                    },
                    radius = if (point == selectedPoint) 8f else 6f,
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }
    }

    HorizontalDivider()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().date.format(statDateFormatter),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = points.last().date.format(statDateFormatter),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    selectedPoint?.let { point ->
        Text(
            text = "Selected: ${point.date.format(statDateFormatter)} | ${formatDecimal(point.value)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

private data class StatPoint(
    val date: LocalDate,
    val value: Double
)

private fun buildMetricSeries(
    historySets: List<WorkoutSetWithExercise>,
    metric: ChartMetric
): List<StatPoint> {
    val groupedByDate = historySets.groupBy {
        Instant.ofEpochMilli(it.set.performedAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    return groupedByDate.mapNotNull { (date, sets) ->
        val value = when (metric) {
            ChartMetric.ONE_REP_MAX -> {
                sets.mapNotNull { set ->
                    calculateOneRepMax(set.exerciseType, set)
                }.maxOrNull()
            }

            ChartMetric.VOLUME -> {
                sets.sumOf { set ->
                    calculateVolume(set.exerciseType, set)
                }
            }

            ChartMetric.MAX_WEIGHT -> {
                sets.mapNotNull { it.set.weight }.maxOrNull()
            }
        }

        value?.let { StatPoint(date = date, value = it) }
    }.sortedBy { it.date }
}

private fun calculateOneRepMax(
    type: ExerciseType,
    set: WorkoutSetWithExercise
): Double? {
    if (!type.usesWeight || !type.usesReps) {
        return null
    }
    val weight = set.set.weight ?: return null
    val reps = set.set.reps ?: return null
    return weight * (1.0 + reps / 30.0)
}

private fun calculateVolume(type: ExerciseType, set: WorkoutSetWithExercise): Double {
    return when (type) {
        ExerciseType.WEIGHT_REPS -> (set.set.weight ?: 0.0) * (set.set.reps ?: 0)
        ExerciseType.REPS_ONLY -> (set.set.reps ?: 0).toDouble()
        ExerciseType.WEIGHT_TIME -> (set.set.weight ?: 0.0) * (set.set.durationSeconds ?: 0)
        ExerciseType.TIME_ONLY -> (set.set.durationSeconds ?: 0).toDouble()
        ExerciseType.DISTANCE_TIME -> set.set.distance ?: 0.0
        ExerciseType.DISTANCE_ONLY -> set.set.distance ?: 0.0
    }
}
