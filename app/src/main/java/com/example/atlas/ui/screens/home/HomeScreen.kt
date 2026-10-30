package com.example.atlas.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.atlas.data.repository.AtlasDatabase

data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tintColor: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onNavigateToShopping: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToWall: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToBirthdays: () -> Unit,
    onNavigateToMeals: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val shoppingDao = remember(context) {
        AtlasDatabase.getInstance(context).shoppingDao()
    }
    val pendingShoppingCount by shoppingDao
        .observePendingCount()
        .collectAsState(initial = 0)
    val dashboardItems = listOf(
        DashboardItem(
            "Listas",
            "$pendingShoppingCount itens pendentes",
            Icons.Default.List,
            Color(0xFF64B5F6),
            onNavigateToShopping
        ),
        DashboardItem("Calendário", "1 evento esta semana", Icons.Default.CalendarMonth, Color(0xFF81C784), onNavigateToCalendar),
        DashboardItem("Orçamento", "Gerencie despesas", Icons.Default.Savings, Color(0xFF80CBC4), onNavigateToFinance),
        DashboardItem("Documentos", "Arquivos da família", Icons.Default.Folder, Color(0xFFB39DDB), onNavigateToDocuments),
        DashboardItem("Aniversários", "Momentos importantes", Icons.Default.Cake, Color(0xFFFFB74D), onNavigateToBirthdays),
        DashboardItem("Refeições", "Planejar cardápio", Icons.Default.Restaurant, Color(0xFF64B5F6), onNavigateToMeals),
        DashboardItem("Horário", "Rotinas da casa", Icons.Default.Schedule, Color(0xFFF06292), onNavigateToSchedule),
        DashboardItem("Receitas", "Livro de receitas", Icons.Default.MenuBook, Color(0xFFFFB74D), onNavigateToRecipes),
        DashboardItem("Mapa", "Localização em tempo real", Icons.Default.LocationOn, Color(0xFFE91E63), onNavigateToLocation),
        DashboardItem("Mensagens", "Chat da família", Icons.Default.Chat, Color(0xFF9575CD), onNavigateToWall),
        DashboardItem("Minha Família", "1 membro", Icons.Default.Group, Color(0xFF4DD0E1), onNavigateToFamily),
        DashboardItem("Galeria", "Fotos e vídeos", Icons.Default.PhotoLibrary, Color(0xFFFFD54F), onNavigateToGallery)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nossa Família ▾",
                style = MaterialTheme.typography.headlineMedium
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Yu",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(dashboardItems, key = { it.title }) { item ->
                DashboardCard(item = item)
            }
        }
    }
}

@Composable
fun DashboardCard(item: DashboardItem) {
    Card(
        onClick = item.onClick,
        modifier = Modifier
            .height(110.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.tintColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
