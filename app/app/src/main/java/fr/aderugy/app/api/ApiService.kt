package fr.aderugy.app.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

const val BASE_URL = "http://35.195.95.11"

interface ApiService {
    @POST("/auth/login")
    suspend fun login(@Body credentials: LoginRequest): Response<DirectusResponse<AuthData>>

    @POST("/users")
    suspend fun register(@Body credentials: CreateApiUserPayload): Response<DirectusResponse<ApiUser>>

    @POST("/auth/refresh")
    suspend fun refresh(@Body credentials: RefreshRequest): Response<DirectusResponse<AuthData>>

    @GET("/users/me?fields[]=*,address.*")
    suspend fun getUserProfile(): Response<DirectusResponse<ApiUser>>

    @PATCH("/users/me")
    suspend fun updateUser(@Body user: UpdateApiUser): Response<DirectusResponse<Any>>

    @PATCH("/items/addresses/{addressId}")
    suspend fun updateAddress(@Path("addressId") addressId: String, @Body address: UpdateApiAddress): Response<DirectusResponse<Any>>

    @GET("/items/categories")
    suspend fun getCategories(): Response<DirectusResponse<List<ApiCategory>>>

    @GET("/items/products")
    suspend fun getProducts(): Response<DirectusResponse<List<ApiProduct>>>

    @GET("/items/orders")
    suspend fun getOrders(): Response<DirectusResponse<List<ApiOrder>>>

    @GET("/items/order_entries")
    suspend fun getOrderEntries(
        @Query("filter[order][_eq]") orderId: String,
        @Query("fields[]") fields: String = "*,product.*"
    ): Response<DirectusResponse<List<ApiOrderEntryFull>>>

    @POST("/items/orders")
    suspend fun createOrder(@Body createOrderPayload: CreateOrderPayload): Response<DirectusResponse<ApiOrder>>

    @GET("/items/carts")
    suspend fun getCartContent(): Response<DirectusResponse<List<ApiCart>>>

    @POST("/items/carts")
    suspend fun addToCart(@Body data: CreateCartPayload): Response<DirectusResponse<ApiCart>>

    @PATCH("/items/carts/{cartId}")
    suspend fun updateCart(
        @Path("cartId") cartId: String,
        @Body data: UpdateCartPayload
    ): Response<DirectusResponse<ApiCart>>

    @DELETE("/items/carts/{cartId}")
    suspend fun deleteCart(
        @Path("cartId") cartId: String,
    ): Response<Any>

    companion object {
        private var tokenStorage: TokenStorage? = null

        private fun init(context: Context) {
            tokenStorage = TokenStorage(context)
        }

        fun create(context: Context): ApiService {
            if (tokenStorage == null) init(context)
            val client = createHttpClient(tokenStorage!!)
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }

        fun logout() {
            tokenStorage?.clearTokens();
        }

        suspend fun login(apiService: ApiService, loginRequest: LoginRequest): Boolean {
            tokenStorage?.clearAccessToken()
            val response = apiService.login(loginRequest)
            if (response.isSuccessful) {
                response.body()?.data?.let {
                    tokenStorage?.saveAuthData(it)
                    return true  // Token refreshed successfully
                }
            }

            return false
        }

        suspend fun refresh(apiService: ApiService): Boolean {
            tokenStorage?.clearAccessToken()
            val refreshToken = tokenStorage?.getRefreshToken()
            if (refreshToken != null) {
                val response = apiService.refresh(RefreshRequest(refresh_token = refreshToken))
                if (response.isSuccessful) {
                    response.body()?.data?.let {
                        tokenStorage?.saveAuthData(it)
                        return true  // Token refreshed successfully
                    }
                }
            }
            return false  // Refresh failed
        }
    }
}


data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val role: String = "39bed02d-9546-4271-81a5-e34effe8154b")
data class RefreshRequest(val refresh_token: String)

data class DirectusResponse<T>(val data: T)
data class AuthData(val access_token: String, val refresh_token: String, val expires: Int)

data class CreateApiUserPayload(
    val email: String,
    val password: String,
    val role: String = "39bed02d-9546-4271-81a5-e34effe8154b",
    val address: CreateApiUserAddress = CreateApiUserAddress()
)

data class CreateApiUserAddress(
    val create: List<AddressCreateUseless> = listOf(AddressCreateUseless())
)

data class AddressCreateUseless(
    val address: String = "+"
)

data class UpdateApiUser(
    var first_name: String?,
    var last_name: String?,
    var email: String?,
    var phone: String?,
)

data class UpdateApiAddress(
    val number: Int?,
    val street: String?,
    val city: String?,
    val zipcode: String?,
    val geolocation: UpdateApiGeolocation?,
)

data class UpdateApiGeolocation(
    val coordinates: List<Double>?,
    val type: String = "Point",
)

data class ApiUser(
    val id: String,
    val first_name: String,
    val last_name: String,
    val email: String,
    val phone: String?,
    val address: ApiAddress,
)

data class ApiAddress(
    val id: Int,
    val number: Int?,
    val street: String?,
    val city: String?,
    val zipcode: String?,
    val geolocation: ApiGeolocation?,
)

data class ApiGeolocation(
    val coordinates: List<Double>,
    val type: String = "Point",
)

data class ApiCategory(
    val id: Int,
    val name: String
)

data class ApiOrder(
    val id: Int,
    val date_created: String,
    val order_entries: List<Int>
)

data class ApiOrderEntryFull(
    val id: Int,
    val order: Int,
    val product: ApiProduct
)

data class ApiOrderEntry(
    val id: Int,
    val order: Int,
    val product: ApiProduct,
)

data class CreateOrderPayload(
    val order_entries: OrderEntries
)

data class OrderEntries(
    val create: List<ProductEntry>
)

data class ProductEntry(
    val product: Int
)

data class ApiProduct(
    val id: Int,
    val name: String,
    val category: Int,
    val description: String,
    val price: Float,
    val image: String
)

data class ApiCart(
    val id: Int,
    val product: Int,
    val quantity: Int,
)

data class CreateCartPayload(
    val product: Int,
    val quantity: Int,
)

data class UpdateCartPayload(
    val product: Int?,
    val quantity: Int?,
)

data class EmptyCartQuery(
    val query: NoLimitQueryParam = NoLimitQueryParam()
)

data class NoLimitQueryParam(
    val limit: Int = -1
)

fun createHttpClient(tokenStorage: TokenStorage): OkHttpClient {
    val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
    return OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request()
            if (request.url.encodedPath != "/auth/login") {
                val accessToken = tokenStorage.getAccessToken()
                if (accessToken != null) {
                    val newRequest = request.newBuilder()
                        .addHeader("Authorization", "Bearer $accessToken")
                        .build()
                    return@addInterceptor chain.proceed(newRequest)
                }
            }
            chain.proceed(request)
        }
        .build()
}