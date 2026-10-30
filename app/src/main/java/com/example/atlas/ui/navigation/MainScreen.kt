package com.example.atlas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.atlas.ui.screens.calendar.CalendarScreen
import com.example.atlas.ui.screens.documents.DocumentsScreen
import com.example.atlas.ui.screens.finance.FinanceScreen
import com.example.atlas.ui.screens.home.HomeScreen
import com.example.atlas.ui.screens.location.LocationScreen
import com.example.atlas.ui.screens.placeholder.PlaceholderScreen
import com.example.atlas.ui.screens.shopping.ShoppingScreen
import com.example.atlas.ui.screens.tasks.TasksScreen
import com.example.atlas.ui.screens.wall.WallScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atlas - Família") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sair"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToShopping = { navController.navigate(Screen.Shopping.route) },
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                    onNavigateToWall = { navController.navigate(Screen.Wall.route) },
                    onNavigateToFinance = { navController.navigate(Screen.Finance.route) },
                    onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                    onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                    onNavigateToBirthdays = { navController.navigate(Screen.Birthdays.route) },
                    onNavigateToMeals = { navController.navigate(Screen.Meals.route) },
                    onNavigateToRecipes = { navController.navigate(Screen.Recipes.route) },
                    onNavigateToFamily = { navController.navigate(Screen.Family.route) },
                    onNavigateToGallery = { navController.navigate(Screen.Gallery.route) }
                )
            }
            composable(Screen.Shopping.route) { ShoppingScreen() }
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Tasks.route) { TasksScreen() }
            composable(Screen.Wall.route) { WallScreen() }
            composable(Screen.Finance.route) { FinanceScreen() }
            composable(Screen.Location.route) { LocationScreen() }
            composable(Screen.Schedule.route) { PlaceholderScreen("🕒 Horário") }
            composable(Screen.Documents.route) { DocumentsScreen() }
            composable(Screen.Birthdays.route) { PlaceholderScreen("🎂 Aniversários") }
            composable(Screen.Meals.route) { PlaceholderScreen("🍽️ Refeições") }
            composable(Screen.Recipes.route) { PlaceholderScreen("📖 Receitas") }
            composable(Screen.Family.route) { PlaceholderScreen("👥 Minha Família") }
            composable(Screen.Gallery.route) { PlaceholderScreen("🖼️ Galeria") }
        }
    }
}
