package fr.aderugy.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.aderugy.app.api.ApiService
import fr.aderugy.app.pages.CartScreen
import fr.aderugy.app.pages.HistoryScreen
import fr.aderugy.app.pages.HomeScreen
import fr.aderugy.app.pages.ProfileScreen

class MainActivity : ComponentActivity() {
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiService = ApiService.create(this)
        setContent {
            MyApp(apiService, this)
        }
    }
}

@Composable
fun MyApp(apiService: ApiService, context: Context) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            CustomBottomBar(navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavigationGraph(navController, apiService, context)
        }
    }
}

@Composable
fun CustomBottomBar(navController: NavHostController) {
    val items = listOf(
        "home" to Icons.Default.Home,
        "history" to Icons.Default.List,
        "cart" to Icons.Default.ShoppingCart,
        "profile" to Icons.Default.AccountCircle
    )

    val currentRoute = remember { mutableStateOf(navController.currentDestination?.route) }

    LaunchedEffect(key1 = navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentRoute.value = destination.route
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (route, icon) ->
            val isSelected = currentRoute.value == route

            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .size(if (isSelected) 30.dp else 24.dp)
                    .clickable {
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                tint = if (isSelected) Color.Blue else Color.Gray
            )
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, apiService: ApiService, context: Context) {
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(apiService) }
        composable("history") { HistoryScreen(apiService) }
        composable("cart") { CartScreen(apiService, context) }
        composable("profile") { ProfileScreen(apiService, context) }
    }
}

