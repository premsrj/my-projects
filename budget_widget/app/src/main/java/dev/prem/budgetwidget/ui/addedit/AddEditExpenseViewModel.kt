package dev.prem.budgetwidget.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import dev.prem.budgetwidget.data.repository.ExpenseRepository
import dev.prem.budgetwidget.navigation.Destinations
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AddEditExpenseUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isExistingExpense: Boolean = false,
    val expenseId: Long? = null,
    val dateInput: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val amountInput: String = "",
    val descriptionInput: String = ""
)

sealed interface AddEditExpenseEvent {
    data object Saved : AddEditExpenseEvent
    data object Deleted : AddEditExpenseEvent
    data class Error(val message: String) : AddEditExpenseEvent
}

class AddEditExpenseViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ExpenseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditExpenseUiState())
    val uiState: StateFlow<AddEditExpenseUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditExpenseEvent>(Channel.BUFFERED)
    val events: Flow<AddEditExpenseEvent> = _events.receiveAsFlow()

    private val expenseIdArg: Long? =
        savedStateHandle.get<String>(Destinations.EXPENSE_ID_ARG)?.toLongOrNull()

    init {
        if (expenseIdArg != null) {
            loadExpense(expenseIdArg)
        }
    }

    fun onDateChanged(value: String) {
        _uiState.value = _uiState.value.copy(dateInput = value)
    }

    fun onAmountChanged(value: String) {
        _uiState.value = _uiState.value.copy(amountInput = value)
    }

    fun onDescriptionChanged(value: String) {
        _uiState.value = _uiState.value.copy(descriptionInput = value)
    }

    fun saveExpense() {
        val current = _uiState.value
        if (current.isSaving) return

        val date = current.dateInput.toLocalDateOrNull()
        if (date == null) {
            emitError("Date must be in YYYY-MM-DD format")
            return
        }

        val amount = current.amountInput.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            emitError("Amount must be greater than zero")
            return
        }

        val description = current.descriptionInput.trim()
        if (description.isBlank()) {
            emitError("Description is required")
            return
        }

        _uiState.value = current.copy(isSaving = true)

        viewModelScope.launch {
            val saveResult = runCatching {
                withContext(Dispatchers.IO) {
                    if (current.isExistingExpense && current.expenseId != null) {
                        repository.updateExpense(
                            id = current.expenseId,
                            date = date,
                            amount = amount,
                            description = description
                        )
                    } else {
                        repository.addExpense(
                            date = date,
                            amount = amount,
                            description = description
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(isSaving = false)

            saveResult
                .onSuccess {
                    _events.send(AddEditExpenseEvent.Saved)
                }
                .onFailure {
                    emitError("Unable to save expense. Please try again")
                }
        }
    }

    fun deleteExpense() {
        val id = _uiState.value.expenseId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteExpense(id)
            }
            _events.send(AddEditExpenseEvent.Deleted)
        }
    }

    private fun loadExpense(id: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val expense = withContext(Dispatchers.IO) {
                repository.getExpense(id)
            }

            if (expense == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.send(AddEditExpenseEvent.Error("Expense not found"))
                return@launch
            }

            _uiState.value = AddEditExpenseUiState(
                isLoading = false,
                isExistingExpense = true,
                expenseId = expense.id,
                dateInput = LocalDate.ofEpochDay(expense.dateEpochDay)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE),
                amountInput = expense.amount.toString(),
                descriptionInput = expense.description
            )
        }
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _events.send(AddEditExpenseEvent.Error(message))
        }
    }

    private fun String.toLocalDateOrNull(): LocalDate? {
        return runCatching {
            LocalDate.parse(this.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull()
    }

    companion object {
        fun factory(repository: ExpenseRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    if (modelClass.isAssignableFrom(AddEditExpenseViewModel::class.java)) {
                        return AddEditExpenseViewModel(
                            savedStateHandle = extras.createSavedStateHandle(),
                            repository = repository
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
                }
            }
        }
    }
}
