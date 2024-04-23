package fr.aderugy.app.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fr.aderugy.app.api.ApiCart
import fr.aderugy.app.api.ApiCategory
import fr.aderugy.app.api.ApiProduct
import fr.aderugy.app.api.ApiService
import fr.aderugy.app.api.CreateCartPayload
import fr.aderugy.app.api.UpdateCartPayload
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(apiService: ApiService) {
    var categories by remember { mutableStateOf<List<ApiCategory>?>(null) }
    var products by remember { mutableStateOf<List<ApiProduct>?>(null) }
    var cartItems by remember { mutableStateOf(mutableStateListOf<ApiCart>()) }
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    var selectedProduct by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Use LaunchedEffect to call API when the composable first appears
    LaunchedEffect(key1 = true, key2 = refreshTrigger) {
        try {
            categories = apiService.getCategories().body()?.data
            products = apiService.getProducts().body()?.data
            val fetchedCartItems = apiService.getCartContent().body()?.data
            cartItems.clear()
            if (fetchedCartItems != null) {
                cartItems.addAll(fetchedCartItems)
            }
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
        if (selectedProduct != null) {
            ProductDetailsScreen(
                product = products?.find { it.id == selectedProduct },
                cartItems = cartItems,
                apiService = apiService,
                onBack = { selectedProduct = null },
                onCartUpdated = { refreshTrigger += 1 }  // Toggle the trigger to refresh
            )
        } else if (selectedCategory != null) {
            val category = categories?.find { it.id == selectedCategory }
            TopAppBar(
                title = {
                    Text(
                        text = category?.name ?: "Selected Category",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { selectedCategory = null }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
            if (products != null) {
                ProductList(products!!.filter { it.category == selectedCategory }) { product ->
                    selectedProduct = product
                }
            }
        } else {
            Text(
                text = "Product Categories",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.Black, fontSize = 24.sp),
                modifier = Modifier.padding(16.dp)
            )
            when {
                isLoading -> Text("Loading...")
                errorMessage != null -> Text("Error: $errorMessage")
                categories != null -> CategoryList(categories!!) { id -> selectedCategory = id }
            }
        }
    }
}


@Composable
fun CategoryList(categories: List<ApiCategory>, onSelectCategory: (Int) -> Unit) {
    LazyColumn {
        items(categories) { category ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .background(color = Color.Black)
                    .clickable { onSelectCategory(category.id) }
                    .padding(16.dp)
            ) {
                Text(
                    text = category.name,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun ProductList(products: List<ApiProduct>, onSelectProduct: (Int) -> Unit) {
    LazyColumn {
        items(products) { product ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onSelectProduct(product.id) }) {
                AsyncImage(
                    model = product.image,
                    contentDescription = product.name,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(2f)) {
                    Text(text = product.name, fontSize = 20.sp)
                    Text(text = "${product.price} €", fontSize = 20.sp)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(product: ApiProduct?, cartItems: List<ApiCart>?, apiService: ApiService, onBack: () -> Unit, onCartUpdated: () -> Unit ) {
    val coroutineScope = rememberCoroutineScope()  // Create a coroutine scope
    // Check if the product is already in the cart and set initial values
    val initialCart = cartItems?.find { it.product == product?.id }
    var quantity by remember { mutableStateOf(initialCart?.quantity ?: 0) }
    val isInCart = initialCart != null

    TopAppBar(
        title = { Text("Product Overview") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
        }
    )

    product?.let { apiProduct ->
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = apiProduct.image,
                contentDescription = apiProduct.name,
                modifier = Modifier.size(150.dp)
            )
            Text(text = apiProduct.name, style = MaterialTheme.typography.headlineMedium)
            Text(text = apiProduct.description, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { if (quantity > 0) quantity-- }) {
                    Text(text = "-")
                }
                Text("$quantity", modifier = Modifier.padding(horizontal = 8.dp))
                Button(onClick = { quantity++ }) {
                    Text(text = "+")
                }
            }
            Button(onClick = {
                coroutineScope.launch {
                    if (quantity > 0) {
                        if (isInCart) {
                            apiService.updateCart(initialCart!!.id.toString(), UpdateCartPayload(apiProduct.id, quantity))
                        } else {
                            apiService.addToCart(CreateCartPayload(apiProduct.id, quantity))
                        }
                    } else if (isInCart && quantity == 0) {
                        apiService.deleteCart(initialCart!!.id.toString())
                    }

                    onCartUpdated()
                }
            }) {
                Text(if (isInCart) "Update Cart (currently ${initialCart?.quantity})" else "Add to Cart")
            }
        }
    }
}

