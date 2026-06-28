package dev.prem.foodtracker.ui.navigation

object AppDestination {
    const val DATE_ARG = "dateEpochDay"
    const val MEAL_ARG = "mealKey"
    const val FOOD_ARG = "foodId"
    const val EDIT_FOOD_ARG = "editFoodId"

    const val HOME = "home"
    const val MEALS = "meals/{$DATE_ARG}"
    const val FOOD_SEARCH = "food_search/{$DATE_ARG}/{$MEAL_ARG}"
    const val TRACK_FOOD = "track_food/{$DATE_ARG}/{$MEAL_ARG}/{$FOOD_ARG}"
    const val FOOD_EDITOR = "food_editor?$EDIT_FOOD_ARG={$EDIT_FOOD_ARG}"
    const val IMPORT_RESULT = "import_result"

    fun home(): String = HOME

    fun meals(dateEpochDay: Long): String = "meals/$dateEpochDay"

    fun foodSearch(dateEpochDay: Long, mealKey: String): String =
        "food_search/$dateEpochDay/$mealKey"

    fun trackFood(dateEpochDay: Long, mealKey: String, foodId: Long): String =
        "track_food/$dateEpochDay/$mealKey/$foodId"

    fun foodEditor(foodId: Long? = null): String {
        val value = foodId ?: -1L
        return "food_editor?$EDIT_FOOD_ARG=$value"
    }

    fun importResult(): String = IMPORT_RESULT
}
