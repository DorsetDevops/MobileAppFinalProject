package fr.aderugy.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import fr.aderugy.app.api.ApiService
import fr.aderugy.app.api.CreateApiUserPayload
import fr.aderugy.app.api.LoginRequest
import fr.aderugy.app.api.RegisterRequest
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiService = ApiService.create(this)
        setContent {
            LoginScreen(apiService, this)
        }
    }
}

@Composable
fun LoginScreen(apiService: ApiService, activity: ComponentActivity) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf("Please login") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(true) {
        if (ApiService.refresh(apiService)) {
            val intent = Intent(activity, MainActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        } else {
            responseMessage = "Session expired. Please login again."
        }
    }

    Column {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password)
        )
        Button(onClick = {
            coroutineScope.launch {
                responseMessage = if (ApiService.login(apiService, LoginRequest(email, password))) {
                    // Start MainActivity on successful login
                    val intent = Intent(activity, MainActivity::class.java)
                    activity.startActivity(intent)
                    // Finish the current activity
                    activity.finish()
                    "Authenticated"
                } else {
                    "Error"
                }
            }
        }) {
            Text("Log In")
        }
        Button(onClick = {
            coroutineScope.launch {
                val registered = apiService.register(CreateApiUserPayload(email, password))

                if (registered.isSuccessful && ApiService.login(apiService, LoginRequest(email, password))) {
                    // Start MainActivity on successful login
                    val intent = Intent(activity, MainActivity::class.java)
                    activity.startActivity(intent)
                    // Finish the current activity
                    activity.finish()
                }
                else
                    responseMessage = "Error"
            }
        }) {
            Text("Sign In")
        }
        Text(responseMessage, style = MaterialTheme.typography.bodyLarge)
    }
}


