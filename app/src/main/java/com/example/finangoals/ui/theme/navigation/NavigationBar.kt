package com.example.finangoals.ui.theme.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.finangoals.R
import com.example.finangoals.ui.theme.screens.login.AuthController
import com.example.finangoals.ui.theme.theme.LightGray
import com.example.finangoals.ui.theme.theme.Orange
import com.example.finangoals.ui.theme.theme.White

//Diseño y configuración de la barra de navegación de la app
@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String) {
    NavigationBar(
        containerColor = Orange,
        contentColor = White
    ) {
        NavigationBarItem(
            selected = currentRoute == NavRoutes.SavingsSummary.route,
            onClick = { navController.navigate(NavRoutes.SavingsSummary.route) },
            colors = NavigationBarItemDefaults.colors(indicatorColor= LightGray),
            icon = {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ahorros),
                    contentDescription = "Resumen Ahorros",
                    tint = if (currentRoute == NavRoutes.SavingsSummary.route) Orange else White,
                    modifier = Modifier.size(25.dp)
                )
            },
            label = { Text("") }
        )
        NavigationBarItem(
            selected = currentRoute == NavRoutes.Home.route,
            onClick = { navController.navigate(NavRoutes.Home.route) },
            colors = NavigationBarItemDefaults.colors(indicatorColor= LightGray),
            icon = { Icon(Icons.Filled.Home,
                contentDescription = "Home",
                tint = if (currentRoute == NavRoutes.Home.route) Orange else White) },
                modifier = Modifier.size(25.dp),
            label = { Text("") }
        )
        NavigationBarItem(
            selected = currentRoute == NavRoutes.ExpenseIncome.route,
            onClick = { navController.navigate(NavRoutes.ExpenseIncome.route) },
            colors = NavigationBarItemDefaults.colors(indicatorColor= LightGray),
            icon = { Icon(
                ImageVector.vectorResource(id = R.drawable.coins),
                contentDescription = "Gastos y ahorros",
                tint = if (currentRoute == NavRoutes.ExpenseIncome.route) Orange else White) },
                modifier = Modifier.size(25.dp),
            label = { Text("") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavHostController, authController: AuthController) {
    // Obtener la ruta actual
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Determinar el título basado en la ruta actual
    val title = when (currentRoute) {
        NavRoutes.Home.route -> "Objetivos"
        NavRoutes.ExpenseIncome.route -> "Gastos e Ingresos"
        NavRoutes.Summary.route -> "Estado financiero"
        NavRoutes.SavingsSummary.route -> "Resumen de Ahorros"  // Agregado en caso de necesitar
        else -> if (currentRoute?.startsWith("goal/") == true) "Objetivo" else ""
    }

    // Construir la TopBar con los valores determinados
    CenterAlignedTopAppBar(
        title = { Text(text = title, fontSize = 24.sp) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Orange,
            titleContentColor = White
        ),
        actions = {
            IconButton(onClick = {
                authController.signOut()
                navController.navigate(NavRoutes.Login.route) }) {
                Icon(imageVector = Icons.Filled.ExitToApp,
                    contentDescription = "Cierre de sesión",
                    tint = White,
                    modifier = Modifier.size(25.dp))
            }
        }
    )
}

