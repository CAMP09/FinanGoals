package com.example.finangoals.ui.theme.navigation

sealed class NavRoutes(val route: String) {
    object Login: NavRoutes("login_screen")
    object Signup: NavRoutes("signup_screen")
    object FirstQuestion: NavRoutes("first_question")
    object SecondQuestion: NavRoutes("second_question")
    object Home: NavRoutes("home_screen")
    object Summary : NavRoutes("summary/{ingresos}/{gastos}") {
        fun createRoute(ingresos: Float, gastos: Float) = "summary/$ingresos/$gastos"
    }
    object ExpenseIncome: NavRoutes("expenseincome_screen")
    object SavingsSummary : NavRoutes("savings_summary_screen")
}