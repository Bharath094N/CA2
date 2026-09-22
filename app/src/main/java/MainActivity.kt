package com.example.inventorymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ==========================================
// 1. DATA MODEL
// ==========================================
data class InventoryItem(
    val id: Int,
    val name: String,
    val stockCount: Int,
    val lowStockThreshold: Int,
) {
    // Derived property: flags item immediately if stock falls below threshold
    val isLowStock: Boolean
        get() = stockCount <= lowStockThreshold
}

// ==========================================
// 2. VIEWMODEL (Single Source of Truth)
// ==========================================
class InventoryViewModel : ViewModel() {

    private val _items = MutableStateFlow(
        listOf(
            InventoryItem(id = 1, name = "MacBook Pro", stockCount = 5, lowStockThreshold = 3),
            InventoryItem(id = 2, name = "Wireless Mouse", stockCount = 2, lowStockThreshold = 4),
            InventoryItem(id = 3, name = "Mechanical Keyboard", stockCount = 7, lowStockThreshold = 5),
            InventoryItem(id = 4, name = "USB-C Hub", stockCount = 1, lowStockThreshold = 2),
        )
    )
    val items: StateFlow<List<InventoryItem>> = _items.asStateFlow()

    fun incrementStock(itemId: Int) {
        _items.update { list ->
            list.map { item ->
                if (item.id == itemId) item.copy(stockCount = item.stockCount + 1) else item
            }
        }
    }

    fun decrementStock(itemId: Int) {
        _items.update { list ->
            list.map { item ->
                if ((item.id == itemId) && (item.stockCount > 0)) {
                    item.copy(stockCount = item.stockCount - 1)
                } else {
                    item
                }
            }
        }
    }
}

// ==========================================
// 3. UI LAYER & STATE HOISTING
// ==========================================

// Stateless, reusable counter controls
@Composable
fun StockQuantityControls(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onDecrement,
            enabled = count > 0,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("-")
        }

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        OutlinedButton(
            onClick = onIncrement,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("+")
        }
    }
}

// Single item row
@Composable
fun InventoryItemRow(
    item: InventoryItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Threshold: ${item.lowStockThreshold}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (item.isLowStock) {
                    Text(
                        text = "LOW STOCK ALERT!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Hoisted control element
            StockQuantityControls(
                count = item.stockCount,
                onIncrement = onIncrement,
                onDecrement = onDecrement
            )
        }
    }
}

// Main screen container
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = viewModel()) {
    // Lifecycle-aware observation: pauses collection when Activity goes to background
    val inventoryItems by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Stock Manager") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = inventoryItems, key = { it.id }) { item ->
                InventoryItemRow(
                    item = item,
                    onIncrement = { viewModel.incrementStock(item.id) },
                    onDecrement = { viewModel.decrementStock(item.id) }
                )
            }
        }
    }
}

// ==========================================
// 4. ACTIVITY ENTRY POINT
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InventoryScreen()
                }
            }
        }
    }
}