package com.example.atlas.ui.screens.shopping

import android.database.sqlite.SQLiteException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.atlas.data.model.ShoppingItem
import com.example.atlas.data.repository.AtlasDatabase
import com.example.atlas.data.repository.ShoppingRepository
import com.example.atlas.data.repository.ShoppingSyncResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen() {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext
    val shoppingRepository = remember(context) {
        ShoppingRepository(
            AtlasDatabase.getInstance(context).shoppingDao(),
            FirebaseFirestore.getInstance()
        )
    }
    val shoppingItems by shoppingRepository.observeItems().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val addedBy = remember {
        val user = FirebaseAuth.getInstance().currentUser
        user?.displayName ?: user?.email ?: "Família"
    }

    LaunchedEffect(shoppingRepository) {
        showSyncResult(
            snackbarHostState,
            "Os itens continuam disponíveis no dispositivo."
        ) {
            shoppingRepository.syncWithFirebase()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Lista de Compras", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${shoppingItems.count { !it.isBought }} itens pendentes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (shoppingItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 56.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sua lista está vazia.")
                    Text(
                        "Toque em + para adicionar o primeiro item.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shoppingItems, key = ShoppingItem::id) { item ->
                        ShoppingItemCard(
                            item = item,
                            onBoughtChanged = { isBought ->
                                coroutineScope.launch {
                                    showSyncResult(snackbarHostState, "Item atualizado localmente.") {
                                        shoppingRepository.setBought(item.id, isBought)
                                    }
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    showSyncResult(snackbarHostState, "Item removido localmente.") {
                                        shoppingRepository.deleteItem(item.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddShoppingItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, category, quantity ->
                coroutineScope.launch {
                    try {
                        val result = shoppingRepository.addItem(
                            ShoppingItem(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                category = category,
                                quantity = quantity,
                                addedBy = addedBy
                            )
                        )
                        showAddDialog = false
                        showSyncResultMessage(
                            snackbarHostState,
                            result,
                            "Item salvo no dispositivo."
                        )
                    } catch (exception: SQLiteException) {
                        snackbarHostState.showSnackbar(
                            "Não foi possível salvar o item: " +
                                (exception.localizedMessage ?: "erro no banco de dados")
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun ShoppingItemCard(
    item: ShoppingItem,
    onBoughtChanged: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isBought,
                onCheckedChange = onBoughtChanged
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.quantity} × ${item.name}",
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.isBought) TextDecoration.LineThrough else null
                )
                Text(
                    text = buildString {
                        append(item.category)
                        if (item.addedBy.isNotBlank()) append(" · ${item.addedBy}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!item.isSynced) {
                    Text(
                        text = "Pendente de sincronização",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remover ${item.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddShoppingItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String, quantity: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Geral") }
    var quantityText by remember { mutableStateOf("1") }
    val quantity = quantityText.toIntOrNull()
    val isQuantityValid = quantity != null && quantity > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantidade") },
                    singleLine = true,
                    isError = !isQuantityValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isQuantityValid) {
                    Text(
                        "Informe uma quantidade maior que zero.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        quantity?.takeIf { it > 0 }?.let { validQuantity ->
                            onAdd(
                                name.trim(),
                                category.trim().ifBlank { "Geral" },
                                validQuantity
                            )
                        }
                    }
                },
                enabled = name.isNotBlank() && isQuantityValid
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private suspend fun showSyncResult(
    snackbarHostState: SnackbarHostState,
    localSuccessMessage: String,
    block: suspend () -> ShoppingSyncResult
) {
    try {
        showSyncResultMessage(snackbarHostState, block(), localSuccessMessage)
    } catch (exception: SQLiteException) {
        snackbarHostState.showSnackbar(
            "Não foi possível salvar a alteração local: " +
                (exception.localizedMessage ?: "erro no banco de dados")
        )
    }
}

private suspend fun showSyncResultMessage(
    snackbarHostState: SnackbarHostState,
    result: ShoppingSyncResult,
    localSuccessMessage: String
) {
    if (result is ShoppingSyncResult.Pending) {
        snackbarHostState.showSnackbar(
            "$localSuccessMessage Sincronização pendente: ${result.reason}"
        )
    }
}
