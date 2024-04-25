package fr.aderugy.app.pages

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import fr.aderugy.app.api.ApiCart
import fr.aderugy.app.api.ApiProduct
import fr.aderugy.app.api.ApiService
import fr.aderugy.app.api.CreateOrderPayload
import fr.aderugy.app.api.OrderEntries
import fr.aderugy.app.api.ProductEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CartScreen(apiService: ApiService, context: Context) {
    var cartItems by remember { mutableStateOf<List<ApiCart>?>(null) }
    var products by remember { mutableStateOf<List<ApiProduct>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = true, key2 = refreshTrigger) {
        try {
            products = apiService.getProducts().body()?.data
            cartItems = apiService.getCartContent().body()?.data
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load data"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Cart",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = "Error: $errorMessage",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (cartItems.isNullOrEmpty() || products.isNullOrEmpty()) {
            Text("Your cart is empty!", modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.weight(4f)) {
                items(cartItems!!) { cart ->
                    val product = products!!.find { it.id == cart.product }
                    product?.let {
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
                                    Text(text = "${it.price} € (x${cart.quantity})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .weight(1f)
                            ) {
                                Text(text = "${String.format("%.2f", it.price * cart.quantity)} €", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        apiService.deleteCart(cart.id.toString())
                                        refreshTrigger++
                                    }
                                }) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().weight(1f)
            ) {
                Text(
                    text = "${cartItems!!.sumOf { it.quantity }} items - ${String.format("%.2f", cartItems!!.sumOf { cartItem ->
                        val productPrice = (products!!.find { it.id == cartItem.product }?.price ?: 0.0f).toDouble()
                        cartItem.quantity * productPrice
                    })} €",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Button(onClick = {
                    coroutineScope.launch {
                        // Create ProductEntry list from cartItems
                        val entries = cartItems!!.flatMap { cartItem ->
                            List(cartItem.quantity) { ProductEntry(product = cartItem.product) }
                        }

                        // Create the payload
                        val payload = CreateOrderPayload(OrderEntries(create = entries))

                        // Call the API
                        try {
                            apiService.createOrder(payload)

                            // If the order creation is successful, show a confirmation Toast
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Order placed successfully!", Toast.LENGTH_LONG).show()
                            }

                            // Clear the cart by deleting items
                            cartItems!!.forEach {
                                apiService.deleteCart(it.id.toString())
                            }

                            // Refresh the UI or data
                            refreshTrigger += 1
                        } catch (e: Exception) {
                            Log.e("ERROR", e.message ?: "")
                            // Handle potential errors such as network issues or API failures
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to place order: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                }) {
                    Text(text = "Order")
                }
            }
        }
    }
}
