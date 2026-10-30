package com.example.atlas.ui.screens.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.atlas.data.local.FamilyDocumentEntity
import com.example.atlas.data.repository.AtlasDatabase
import com.example.atlas.data.repository.DocumentSyncResult
import com.example.atlas.data.repository.FamilyDocumentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen() {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appContext = context.applicationContext
    val documentRepository = remember(appContext) {
        FamilyDocumentRepository(
            AtlasDatabase.getInstance(appContext).familyDocumentDao(),
            FirebaseFirestore.getInstance()
        )
    }
    val documents by documentRepository.observeDocuments().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val addedBy = remember {
        val user = FirebaseAuth.getInstance().currentUser
        user?.displayName ?: user?.email ?: "Família"
    }

    LaunchedEffect(documentRepository) {
        showDocumentSyncResult(
            snackbarHostState,
            "Os registros permanecem disponíveis no dispositivo."
        ) {
            documentRepository.syncWithFirebase()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar documento")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Documentos da família", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Salve links e informações; os arquivos não são enviados pelo aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (documents.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 56.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhum documento cadastrado.")
                    Text(
                        "Toque em + para salvar um link.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(documents, key = FamilyDocumentEntity::id) { document ->
                        FamilyDocumentCard(
                            document = document,
                            onOpenLink = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(document.url))
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Não há aplicativo disponível para abrir este link."
                                        )
                                    }
                                } catch (exception: SecurityException) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Não foi possível abrir o link: " +
                                                (exception.localizedMessage ?: "acesso negado")
                                        )
                                    }
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    showDocumentSyncResult(
                                        snackbarHostState,
                                        "Registro removido do dispositivo."
                                    ) {
                                        documentRepository.deleteDocument(document.id)
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
        AddFamilyDocumentDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, description, url ->
                coroutineScope.launch {
                    try {
                        val result = documentRepository.addDocument(
                            FamilyDocumentEntity(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                url = url,
                                addedBy = addedBy
                            )
                        )
                        showAddDialog = false
                        showDocumentSyncResultMessage(
                            snackbarHostState,
                            result,
                            "Registro salvo no dispositivo."
                        )
                    } catch (exception: SQLiteException) {
                        snackbarHostState.showSnackbar(
                            "Não foi possível salvar o registro: " +
                                (exception.localizedMessage ?: "erro no banco de dados")
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun FamilyDocumentCard(
    document: FamilyDocumentEntity,
    onOpenLink: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(document.name, style = MaterialTheme.typography.titleMedium)
                if (document.description.isNotBlank()) {
                    Text(
                        text = document.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = document.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(document.addedBy)
                        if (!document.isSynced) append(" · Sincronização pendente")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenLink) {
                    Text("Abrir link")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remover ${document.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddFamilyDocumentDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, description: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val parsedUri = remember(url) { Uri.parse(url.trim()) }
    val isValidWebUrl = parsedUri.scheme in setOf("http", "https") &&
        !parsedUri.host.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo link") },
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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Link (https://...)") },
                    singleLine = true,
                    isError = url.isNotBlank() && !isValidWebUrl,
                    modifier = Modifier.fillMaxWidth()
                )
                if (url.isNotBlank() && !isValidWebUrl) {
                    Text(
                        "Informe um link HTTP ou HTTPS válido.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && isValidWebUrl) {
                        onAdd(name.trim(), description.trim(), parsedUri.toString())
                    }
                },
                enabled = name.isNotBlank() && isValidWebUrl
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private suspend fun showDocumentSyncResult(
    snackbarHostState: SnackbarHostState,
    localSuccessMessage: String,
    sync: suspend () -> DocumentSyncResult
) {
    try {
        showDocumentSyncResultMessage(snackbarHostState, sync(), localSuccessMessage)
    } catch (exception: SQLiteException) {
        snackbarHostState.showSnackbar(
            "Não foi possível acessar os registros locais: " +
                (exception.localizedMessage ?: "erro no banco de dados")
        )
    }
}

private suspend fun showDocumentSyncResultMessage(
    snackbarHostState: SnackbarHostState,
    result: DocumentSyncResult,
    localSuccessMessage: String
) {
    if (result is DocumentSyncResult.Pending) {
        snackbarHostState.showSnackbar(
            "$localSuccessMessage Sincronização pendente: ${result.reason}"
        )
    }
}
