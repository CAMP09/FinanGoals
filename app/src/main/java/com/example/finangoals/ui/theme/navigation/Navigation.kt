import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.finangoals.db.FirestoreManager
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.screens.questions.FirstQuestion
import com.example.finangoals.ui.theme.screens.goals.GoalScreen
import com.example.finangoals.ui.theme.screens.goals.HomeScreen
import com.example.finangoals.ui.theme.screens.summary.SavingsSummaryScreen
import com.example.finangoals.ui.theme.screens.login.LoginScreen
import com.example.finangoals.ui.theme.screens.questions.SecondQuestion
import com.example.finangoals.ui.theme.screens.expensesnincome.ExpenseIncomeScreen
import com.example.finangoals.ui.theme.screens.login.AuthController
import com.example.finangoals.ui.theme.screens.summary.Summary

@Composable
fun Navigation(context: Context, navController: NavHostController = rememberNavController()) {
    val authController = AuthController(context)
    val firestoreManager = FirestoreManager(context)

    NavHost(navController = navController, startDestination = determineStartDestination(authController)) {
        composable(route = NavRoutes.Login.route) {
            LoginScreen(navController, authController)
        }
        composable(route = NavRoutes.Signup.route) {
            SignupScreen(navController, authController)
        }
        composable(route = NavRoutes.FirstQuestion.route) {
            FirstQuestion(navController)
        }
        composable(route = NavRoutes.SecondQuestion.route) {
            SecondQuestion(navController)
        }

        composable(
            route = NavRoutes.Summary.route,
            arguments = listOf(
                navArgument("ingresos") { type = NavType.FloatType },
                navArgument("gastos") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val ingresos = backStackEntry.arguments?.getFloat("ingresos") ?: 0f
            val gastos = backStackEntry.arguments?.getFloat("gastos") ?: 0f
            Summary(navController, firestoreManager ,ingresos, gastos)
        }


        composable(route = NavRoutes.SavingsSummary.route) {
            SavingsSummaryScreen(navController, firestoreManager, authController)
        }
        composable(route = NavRoutes.Home.route) {
            HomeScreen(navController, authController, firestoreManager)
        }
        composable("goal/{goalId}") { backStackEntry ->
            GoalScreen(navController, backStackEntry.arguments?.getString("goalId") ?: "")
        }
        composable(route = NavRoutes.ExpenseIncome.route) {
            ExpenseIncomeScreen(navController, authController, firestoreManager)
        }
    }
}

// Verificamos si ya se ha iniciado sesión
fun determineStartDestination(authController: AuthController): String {
    return if (authController.isUserAuthenticated()) {
        NavRoutes.Home.route
    } else {
        NavRoutes.Login.route
    }
}