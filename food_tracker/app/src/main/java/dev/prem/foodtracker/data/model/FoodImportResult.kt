package dev.prem.foodtracker.data.model

data class FoodImportResult(
    val importedCount: Int,
    val failedNames: List<String>,
    val duplicateNames: List<String>
)
