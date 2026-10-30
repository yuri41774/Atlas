#!/bin/bash

# Define a pasta base do pacote principal do app
PACKAGE_DIR="app/src/main/java/com/example/atlas"

echo "📂 Criando estrutura de pastas para o Atlas em $PACKAGE_DIR..."

# 1. Criação dos diretórios por funcionalidade/camada
mkdir -p "$PACKAGE_DIR/data/model"
mkdir -p "$PACKAGE_DIR/data/repository"
mkdir -p "$PACKAGE_DIR/ui/navigation"
mkdir -p "$PACKAGE_DIR/ui/screens/shopping"
mkdir -p "$PACKAGE_DIR/ui/screens/calendar"
mkdir -p "$PACKAGE_DIR/ui/screens/tasks"
mkdir -p "$PACKAGE_DIR/ui/screens/wall"
mkdir -p "$PACKAGE_DIR/ui/screens/finance"
mkdir -p "$PACKAGE_DIR/ui/screens/location"
mkdir -p "$PACKAGE_DIR/ui/screens/home"
mkdir -p "$PACKAGE_DIR/ui/components"
mkdir -p "$PACKAGE_DIR/ui/theme"

echo "📄 Criando arquivos Kotlin com códigos iniciais..."

# 2. Navegação: Screen.kt
cat << 'EOF' > "$PACKAGE_DIR/ui/navigation/Screen.kt"
package com.example.atlas.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object Shopping : Screen("shopping", "Compras", Icons.Default.ShoppingCart)
    object Calendar : Screen("calendar", "Agenda", Icons.Default.CalendarMonth)
    object Tasks : Screen("tasks", "Tarefas", Icons.Default.Checklist)
    object Wall : Screen("wall", "Mural", Icons.Default.PushPin)
    object Finance : Screen("finance", "Finanças", Icons.Default.AttachMoney)
    object Location : Screen("location", "Locais", Icons.Default.LocationOn)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Shopping,
    Screen.Calendar,
    Screen.Tasks,
    Screen.Wall
)
EOF

# 3. Navegação: MainScreen.kt
cat << 'EOF' > "$PACKAGE_DIR/ui/navigation/MainScreen.kt"
package com.example.atlas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.example.atlas.ui.screens.finance.FinanceScreen
import com.example.atlas.ui.screens.home.HomeScreen
import com.example.atlas.ui.screens.location.LocationScreen
import com.example.atlas.ui.screens.shopping.ShoppingScreen
import com.example.atlas.ui.screens.tasks.TasksScreen
import com.example.atlas.ui.screens.wall.WallScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atlas - Família") },
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
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToWall = { navController.navigate(Screen.Wall.route) },
                    onNavigateToFinance = { navController.navigate(Screen.Finance.route) },
                    onNavigateToLocation = { navController.navigate(Screen.Location.route) }
                )
            }
            composable(Screen.Shopping.route) { ShoppingScreen() }
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Tasks.route) { TasksScreen() }
            composable(Screen.Wall.route) { WallScreen() }
            composable(Screen.Finance.route) { FinanceScreen() }
            composable(Screen.Location.route) { LocationScreen() }
        }
    }
}
EOF

# 4. Dashboard: HomeScreen.kt
cat << 'EOF' > "$PACKAGE_DIR/ui/screens/home/HomeScreen.kt"
package com.example.atlas.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.atlas.data.model.HomeSummary

@Composable
fun HomeScreen(
    onNavigateToShopping: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToWall: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToLocation: () -> Unit
) {
    val summary = HomeSummary()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Bem-vindo de volta! 👋",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = summary.familyName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text(
            text = "Resumo da Casa",
            style = MaterialTheme.typography.titleLarge
        )

        HomeCard(
            title = "Lista de Compras",
            description = "${summary.pendingShoppingCount} itens pendentes",
            icon = Icons.Default.ShoppingCart,
            onClick = onNavigateToShopping
        )

        HomeCard(
            title = "Próximo Evento",
            description = summary.upcomingEvent,
            icon = Icons.Default.CalendarMonth,
            onClick = onNavigateToCalendar
        )

        HomeCard(
            title = "Sua Tarefa do Dia",
            description = summary.pendingTask,
            icon = Icons.Default.Checklist,
            onClick = onNavigateToTasks
        )

        HomeCard(
            title = "Último Aviso",
            description = summary.latestAnnouncement,
            icon = Icons.Default.PushPin,
            onClick = onNavigateToWall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallShortcutCard(
                title = "Finanças",
                icon = Icons.Default.AttachMoney,
                onClick = onNavigateToFinance,
                modifier = Modifier.weight(1f)
            )
            SmallShortcutCard(
                title = "Locais",
                icon = Icons.Default.LocationOn,
                onClick = onNavigateToLocation,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HomeCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SmallShortcutCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge)
        }
    }
}
EOF

# 5. Criando Telas nas suas respetivas pastas

cat << 'EOF' > "$PACKAGE_DIR/ui/screens/shopping/ShoppingScreen.kt"
package com.example.atlas.ui.screens.shopping

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ShoppingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("🛒 Lista de Compras", style = MaterialTheme.typography.headlineMedium)
    }
}
EOF

cat << 'EOF' > "$PACKAGE_DIR/ui/screens/calendar/CalendarScreen.kt"
package com.example.atlas.ui.screens.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CalendarScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("📅 Agenda Familiar", style = MaterialTheme.typography.headlineMedium)
    }
}
EOF

cat << 'EOF' > "$PACKAGE_DIR/ui/screens/tasks/TasksScreen.kt"
package com.example.atlas.ui.screens.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TasksScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("📋 Tarefas Domésticas", style = MaterialTheme.typography.headlineMedium)
    }
}
EOF

cat << 'EOF' > "$PACKAGE_DIR/ui/screens/wall/WallScreen.kt"
package com.example.atlas.ui.screens.wall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun WallScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("📌 Mural da Família", style = MaterialTheme.typography.headlineMedium)
    }
}
EOF

cat << 'EOF' > "$PACKAGE_DIR/ui/screens/finance/FinanceScreen.kt"
package com.example.atlas.ui.screens.finance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FinanceScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("💰 Finanças da Casa", style = MaterialTheme.typography.headlineMedium)
    }
}
EOF

cat << 'EOF' > "$PACKAGE_DIR/ui/screens/location/LocationScreen.kt"
package com.example.atlas.ui.screens.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LocationScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("📍 Check-in e Localização", style = MaterialTheme.typography.headlineMedium)
    }
}
EOF

# 6. Modelos Iniciais da Lista de Compras
cat << 'EOF' > "$PACKAGE_DIR/data/model/ShoppingItem.kt"
package com.example.atlas.data.model

data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val category: String = "Geral",
    val quantity: Int = 1,
    val isBought: Boolean = false,
    val addedBy: String = ""
)
EOF

echo "✅ Estrutura criada com sucesso!"
