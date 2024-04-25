package fr.aderugy.app.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import fr.aderugy.app.api.ApiProduct
import fr.aderugy.app.api.ApiService
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Locale

data class OrderDisplay(
    val id: Int,
    val date_created: String,
    val price: Double,
    val quantity: Int,
    val products: List<OrderProductDisplay>,
)

data class OrderProductDisplay(
    val product: ApiProduct,
    val quantity: Int,
)

@Composable
fun HistoryScreen(apiService: ApiService) {
    var currentView by remember { mutableStateOf("orders") }
    var selectedOrder by remember { mutableStateOf<OrderDisplay?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var orderDisplays by remember { mutableStateOf<List<OrderDisplay>?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        try {
            val apiOrders = apiService.getOrders().body()?.data ?: emptyList()
            val tempOrderDisplays = mutableListOf<OrderDisplay>()
            apiOrders.forEach { order ->
                val orderEntries = apiService.getOrderEntries(order.id.toString()).body()?.data ?: emptyList()
                val productsMap = orderEntries.groupBy { it.product.id }

                val orderProducts = productsMap.map { (productId, entries) ->
                    OrderProductDisplay(
                        product = entries.first().product,
                        quantity = entries.size
                    )
                }

                val orderDisplay = OrderDisplay(
                    id = order.id,
                    date_created = order.date_created,
                    price = orderEntries.sumOf { it.product.price.toDouble() },
                    quantity = orderEntries.size,
                    products = orderProducts
                )
                tempOrderDisplays.add(orderDisplay)
            }
            orderDisplays = tempOrderDisplays
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load data"
            isLoading = false
        }
    }

    if (currentView == "orders") {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "My Past Orders",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(bottom = 16.dp)
                )
            }

            orderDisplays?.let {
                LazyColumn {
                    items(it) { orderDisplay ->
                        OrderItem(orderDisplay, onClick = {
                            selectedOrder = orderDisplay
                            currentView = "productDetails"
                        })
                    }
                }
            }
        }
    } else if (currentView == "productDetails") {
        selectedOrder?.let { order ->
            Column(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()) {
                Row {
                    IconButton(onClick = { currentView = "orders"; selectedOrder = null }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(text = "My order", style = MaterialTheme.typography.headlineLarge)
                }
                ProductDetailsView(order, apiService, coroutineScope)
            }
        }
    }
}

@Composable
fun OrderItem(orderDisplay: OrderDisplay, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            modifier = Modifier.weight(2f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${"%.2f".format(orderDisplay.price)} €",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }


        Column(
            modifier = Modifier
                .weight(4f)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatDate(orderDisplay.date_created),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Quantity ${orderDisplay.quantity}",
                style = MaterialTheme.typography.bodySmall
            )
        }


        Text(
            text = "Order ID: ${orderDisplay.id}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(2f)
                .padding(start = 8.dp)
        )
    }
}

@Composable
fun ProductDetailsView(order: OrderDisplay, apiService: ApiService, coroutineScope: CoroutineScope) {
    LazyColumn {
        items(order.products) { product ->
            product.product.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(2.4f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = it.image),
                            contentDescription = it.name,
                            modifier = Modifier.size(60.dp)
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(text = it.name, style = MaterialTheme.typography.bodyLarge)
                            Text(text = "${it.price} € (x${product.quantity})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

fun formatDate(dateString: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return try {
        val parsedDate = parser.parse(dateString)
        formatter.format(parsedDate)
    } catch (e: Exception) {
        "Invalid date"
    }
}
