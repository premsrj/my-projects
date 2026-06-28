package dev.prem.foodtracker.data.model

enum class MealType(val key: String, val label: String) {
    BREAKFAST("breakfast", "Breakfast"),
    MORNING_SNACK("morning_snack", "Morning Snack"),
    LUNCH("lunch", "Lunch"),
    EVENING_SNACK("evening_snack", "Evening Snack"),
    DINNER("dinner", "Dinner");

    companion object {
        val ordered = entries.toList()

        fun fromKey(key: String): MealType {
            return entries.firstOrNull { it.key == key } ?: BREAKFAST
        }
    }
}
