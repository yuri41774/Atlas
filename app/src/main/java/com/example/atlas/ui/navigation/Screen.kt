package com.example.atlas.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object Shopping : Screen("shopping", "Compras", Icons.Default.List)
    object Calendar : Screen("calendar", "Agenda", Icons.Default.CalendarMonth)
    object Tasks : Screen("tasks", "Tarefas", Icons.Default.Checklist)
    object Wall : Screen("wall", "Mural", Icons.Default.PushPin)
    object Finance : Screen("finance", "Orçamento", Icons.Default.AttachMoney)
    object Location : Screen("location", "Mapa", Icons.Default.LocationOn)
    object Documents : Screen("documents", "Documentos", Icons.Default.Folder)
    object Birthdays : Screen("birthdays", "Aniversários", Icons.Default.Cake)
    object Meals : Screen("meals", "Refeições", Icons.Default.Restaurant)
    object Schedule : Screen("schedule", "Horário", Icons.Default.Schedule)
    object Recipes : Screen("recipes", "Receitas", Icons.Default.MenuBook)
    object Family : Screen("family", "Minha Família", Icons.Default.Group)
    object Gallery : Screen("gallery", "Galeria", Icons.Default.PhotoLibrary)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Shopping,
    Screen.Calendar,
    Screen.Tasks,
    Screen.Wall
)
