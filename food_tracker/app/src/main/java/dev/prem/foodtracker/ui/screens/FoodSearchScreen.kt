package dev.prem.foodtracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.prem.foodtracker.data.db.FoodEntity
import dev.prem.foodtracker.data.model.MealType
import dev.prem.foodtracker.data.repo.FoodTrackerRepository
import dev.prem.foodtracker.ui.appContainer
import dev.prem.foodtracker.ui.components.formatDouble
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class FoodSearchViewModel(
    private val repository: FoodTrackerRepository
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<FoodSearchUiState> = combine(
        repository.observeFoods(),
        query
    ) { foods, inputQuery ->
        FoodSearchUiState(
            query = inputQuery,
            foods = rankFoods(foods, inputQuery)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FoodSearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    private fun rankFoods(foods: List<FoodEntity>, rawQuery: String): List<FoodEntity> {
        val normalized = rawQuery.trim().lowercase()
        if (normalized.isBlank()) return foods.sortedBy { it.name.lowercase() }

        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }

        return foods.mapNotNull { food ->
            val haystack = (food.name + " " + food.comments).lowercase()
            if (!tokens.all { token -> haystack.contains(token) }) {
                return@mapNotNull null
            }

            val nameLower = food.name.lowercase()
            var score = 0
            if (nameLower == normalized) score += 2000
            if (nameLower.contains(normalized)) score += 1000

            tokens.forEach { token ->
                score += when {
                    nameLower.startsWith(token) -> 60
                    nameLower.contains(token) -> 30
                    haystack.contains(token) -> 10
                    else -> 0
                }
            }

            food to score
        }.sortedWith(
            compareByDescending<Pair<FoodEntity, Int>> { it.second }
                .thenBy { it.first.name.lowercase() }
        ).map { it.first }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = this.appContainer()
                FoodSearchViewModel(container.repository)
            }
        }
    }
}

data class FoodSearchUiState(
    val query: String = "",
    val foods: List<FoodEntity> = emptyList()
)

@Composable
fun FoodSearchRoute(
    selectedDate: LocalDate,
    mealType: MealType,
    onBack: () -> Unit,
    onFoodSelected: (Long) -> Unit,
    onAddFood: () -> Unit,
    onEditFood: (Long) -> Unit
) {
    val viewModel: FoodSearchViewModel = viewModel(factory = FoodSearchViewModel.factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FoodSearchScreen(
        selectedDate = selectedDate,
        mealType = mealType,
        uiState = uiState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onFoodSelected = onFoodSelected,
        onAddFood = onAddFood,
        onEditFood = onEditFood
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    selectedDate: LocalDate,
    mealType: MealType,
    uiState: FoodSearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFoodSelected: (Long) -> Unit,
    onAddFood: () -> Unit,
    onEditFood: (Long) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("${mealType.label} • ${selectedDate}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddFood) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add food")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFood,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add food")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                label = { Text("Search foods") },
                placeholder = { Text("Try: plain dosa") },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = uiState.foods, key = { it.id }) { food ->
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFoodSelected(food.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${formatDouble(food.calories)} kcal per ${formatDouble(food.servingSize)} ${food.unit}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (food.comments.isNotBlank()) {
                                    Text(
                                        text = food.comments,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { onEditFood(food.id) }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                }
            }
        }
    }
}
