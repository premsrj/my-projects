package com.example.workouttracker.ui.today

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.data.WorkoutSetWithExercise
import com.example.workouttracker.ui.track.formatSetSummary
import com.example.workouttracker.ui.track.formatTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun TodayRoute(
    selectedDate: LocalDate,
    onTrackExerciseClick: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val viewModel: TodayViewModel = viewModel(
        key = "today-${selectedDate}",
        factory = TodayViewModel.factory(selectedDate)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    TodayScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTrackExerciseClick = onTrackExerciseClick,
        onExerciseClick = onExerciseClick,
        onDateSelected = onDateSelected,
        onRecomputePrsClick = viewModel::recomputePersonalRecords
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TodayScreen(
    uiState: TodayUiState,
    snackbarHostState: SnackbarHostState,
    onTrackExerciseClick: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onRecomputePrsClick: () -> Unit
) {
    var showHistoryCalendar by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var visibleComment by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Today")
                        Text(
                            text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryCalendar = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Workout history"
                        )
                    }
                    IconButton(onClick = onTrackExerciseClick) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Track exercise")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (uiState.isRecomputingPrs) {
                                        "Recomputing PRs..."
                                    } else {
                                        "Recompute PRs"
                                    }
                                )
                            },
                            enabled = !uiState.isRecomputingPrs,
                            onClick = {
                                showMenu = false
                                onRecomputePrsClick()
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.timelineItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No exercises tracked for this date.",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Tap + in the top bar to add exercises for this date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = uiState.timelineItems,
                key = { item ->
                    when (item) {
                        is TodayTimelineItem.NonSupersetExerciseBlock -> "exercise-${item.entry.exerciseId}"
                        is TodayTimelineItem.SupersetBlock -> "superset-${item.groupId}"
                    }
                }
            ) { item ->
                when (item) {
                    is TodayTimelineItem.NonSupersetExerciseBlock -> {
                        val entry = item.entry
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExerciseClick(entry.exerciseId) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = entry.exerciseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${entry.categoryName} | ${entry.type.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                entry.sets.forEach { set ->
                                    TodaySetRow(
                                        set = set,
                                        isPr = uiState.personalRecordSetIds.contains(set.set.id),
                                        onCommentClick = { comment ->
                                            visibleComment = comment
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is TodayTimelineItem.SupersetBlock -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Superset",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                item.rounds.forEachIndexed { roundIndex, round ->
                                    if (roundIndex > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Text(
                                        text = "Round ${round.round}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    round.exercises.forEach { exercise ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onExerciseClick(exercise.exerciseId) },
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = exercise.exerciseName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${exercise.categoryName} | ${exercise.type.label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            exercise.sets.forEach { set ->
                                                TodaySetRow(
                                                    set = set,
                                                    isPr = uiState.personalRecordSetIds.contains(set.set.id),
                                                    onCommentClick = { comment ->
                                                        visibleComment = comment
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistoryCalendar) {
        WorkoutHistoryCalendarDialog(
            initialDate = uiState.selectedDate,
            trackedDates = uiState.trackedDates,
            onDateSelected = { chosenDate ->
                showHistoryCalendar = false
                onDateSelected(chosenDate)
            },
            onDismiss = { showHistoryCalendar = false }
        )
    }

    visibleComment?.let { commentText ->
        AlertDialog(
            onDismissRequest = { visibleComment = null },
            title = { Text(text = "Comment") },
            text = { Text(text = commentText) },
            confirmButton = {
                TextButton(onClick = { visibleComment = null }) {
                    Text(text = "Close")
                }
            }
        )
    }
}

@Composable
private fun TodaySetRow(
    set: WorkoutSetWithExercise,
    isPr: Boolean,
    onCommentClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Set ${set.set.sequenceInExercise}: ${formatSetSummary(set.exerciseType, set)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isPr) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Personal record",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            val comment = set.set.comment
            if (!comment.isNullOrBlank()) {
                IconButton(
                    onClick = { onCommentClick(comment) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "View set comment",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = formatTime(set.set.performedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutHistoryCalendarDialog(
    initialDate: LocalDate,
    trackedDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayedMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Workout history") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }
                    Text(
                        text = displayedMonth.format(monthLabelFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }

                CalendarPreview(
                    displayedMonth = displayedMonth,
                    trackedDates = trackedDates,
                    onDateSelected = onDateSelected
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}

@Composable
private fun CalendarPreview(
    displayedMonth: YearMonth,
    trackedDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val weekdayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val firstDayOffset = displayedMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = displayedMonth.lengthOfMonth()
    val totalSlots = ((firstDayOffset + daysInMonth + 6) / 7) * 7

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayHeaders.forEach { header ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        for (week in 0 until (totalSlots / 7)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayInWeek in 0 until 7) {
                    val slotIndex = (week * 7) + dayInWeek
                    val dayNumber = slotIndex - firstDayOffset + 1

                    if (dayNumber !in 1..daysInMonth) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )
                    } else {
                        val currentDate = displayedMonth.atDay(dayNumber)
                        val hasWorkout = trackedDates.contains(currentDate)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { onDateSelected(currentDate) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (hasWorkout) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Workout tracked",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
