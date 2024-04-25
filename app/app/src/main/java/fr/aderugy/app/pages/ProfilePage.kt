package fr.aderugy.app.pages

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.aderugy.app.LoginActivity
import fr.aderugy.app.api.ApiService
import fr.aderugy.app.api.ApiUser
import fr.aderugy.app.api.UpdateApiAddress
import fr.aderugy.app.api.UpdateApiGeolocation
import fr.aderugy.app.api.UpdateApiUser
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(apiService: ApiService, context: Context) {
    var showAboutScreen by remember { mutableStateOf(false) }
    if (showAboutScreen) {
        AboutAppScreen { showAboutScreen = false }
    } else {
        UserProfileForm(apiService, context) { showAboutScreen = true }
    }
}

@Composable
fun UserProfileForm(apiService: ApiService, context: Context, onAboutClick: () -> Unit) {
    var user by remember { mutableStateOf<ApiUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = true, key2 = refreshTrigger) {
        try {
            user = apiService.getUserProfile().body()?.data
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load data"
            isLoading = false
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (errorMessage != null) {
        Text("Error: $errorMessage")
    } else {
        user?.let { apiUser ->
            var firstName by remember { mutableStateOf(apiUser.first_name) }
            var lastName by remember { mutableStateOf(apiUser.last_name) }
            var email by remember { mutableStateOf(apiUser.email) }
            var phone by remember { mutableStateOf(apiUser.phone) }
            var addressNumber by remember { mutableStateOf(apiUser.address.number?.toString()) }
            var addressStreet by remember { mutableStateOf(apiUser.address.street) }
            var addressCity by remember { mutableStateOf(apiUser.address.city) }
            var addressZipcode by remember { mutableStateOf(apiUser.address.zipcode) }
            var geolocationLat by remember { mutableStateOf(
                apiUser.address.geolocation?.coordinates?.get(0).toString()) }
            var geolocationLon by remember { mutableStateOf(
                apiUser.address.geolocation?.coordinates?.get(1).toString()) }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Edit Profile", style = MaterialTheme.typography.headlineLarge)
                Row(modifier = Modifier.fillMaxWidth()) {
                    UserTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "First Name",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    UserTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "Last Name",
                        modifier = Modifier.weight(1f)
                    )
                }
                UserTextField(value = email, onValueChange = { email = it }, label = "Email")
                UserTextField(value = phone, onValueChange = { phone = it }, label = "Phone")

                Spacer(Modifier.height(16.dp))


                Text("Address", style = MaterialTheme.typography.headlineMedium)
                UserTextField(value = addressNumber, onValueChange = { addressNumber = it }, label = "Number")
                UserTextField(value = addressStreet, onValueChange = { addressStreet = it }, label = "Street")
                Row(modifier = Modifier.fillMaxWidth()) {
                    UserTextField(
                        value = addressCity,
                        onValueChange = { addressCity = it },
                        modifier = Modifier.weight(1f),
                        label = "City"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    UserTextField(
                        value = addressZipcode,
                        onValueChange = { addressZipcode = it },
                        modifier = Modifier.weight(1f),
                        label = "Zipcode"
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    UserTextField(
                        value = geolocationLat,
                        onValueChange = { geolocationLat = it },
                        modifier = Modifier.weight(1f),
                        label = "Latitude"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    UserTextField(
                        value = geolocationLon,
                        onValueChange = { geolocationLon = it },
                        modifier = Modifier.weight(1f),
                        label = "Longitude"
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    onClick = {
                    coroutineScope.launch {
                        val updatedAddress = UpdateApiAddress(
                            number = addressNumber?.toIntOrNull(),
                            street = addressStreet,
                            city = addressCity,
                            zipcode = addressZipcode,
                            geolocation = UpdateApiGeolocation(
                                coordinates = listOf(geolocationLat.toDoubleOrNull() ?: 0.0, geolocationLon.toDoubleOrNull() ?: 0.0)
                            )
                        )

                        val updatedUser = UpdateApiUser(
                            first_name = firstName,
                            last_name = lastName,
                            email = email,
                            phone = phone,
                        )
                        apiService.updateUser(updatedUser)
                        apiService.updateAddress(user?.address?.id.toString(), updatedAddress)
                        refreshTrigger++
                    }
                }) {
                    Text("Apply Changes")
                }
                Button(
                    onClick = { onAboutClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text("About This App")
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    onClick = {
                    coroutineScope.launch {
                        ApiService.logout()
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                    }
                }
                ) {
                    Text("Log out")
                }
            }
        }
    }
}


@Composable
fun UserTextField(value: String?, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    TextField(
        value = value ?: "",
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
}


@Composable
fun AboutAppScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("About This App", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("NO COPYRIGHTS", style = MaterialTheme.typography.bodyLarge)
        Text("@AUTHOR ADERUGY", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text("Version 1.0.0", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back to Profile")
        }
    }
}
