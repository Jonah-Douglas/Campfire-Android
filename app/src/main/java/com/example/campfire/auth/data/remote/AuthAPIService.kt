package com.example.campfire.auth.data.remote

import com.example.campfire.auth.data.remote.dto.request.LoginRequest
import com.example.campfire.auth.data.remote.dto.request.RegisterRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyEmailRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyPhoneRequest
import com.example.campfire.auth.data.remote.dto.response.ApiResponse
import com.example.campfire.auth.data.remote.dto.response.LoginResponse
import com.example.campfire.auth.data.remote.dto.response.MessageResponse
import com.example.campfire.auth.data.remote.dto.response.RegisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface AuthApiService {
    // JD TODO: Remove these once I get new resources connected in FastAPI
//    @GET("/api/v1/users")
//    suspend fun getUsers(@Query("skip") skip: Int = 0, @Query("limit") limit: Int = 100): Response<List<User>>
//
//    @POST("/Users")
//    suspend fun createUser(@Body user: User): Response<User>
//
//    @POST("/api/v1/login/access-token")
//    suspend fun login(@Body user: RequestBody): Response<Token>
    
    @POST("auth/register")
    fun registerUser(@Body request: RegisterRequest): Call<ApiResponse<RegisterResponse>> // Assuming ApiResponse is a generic wrapper
    
    @POST("auth/verify-email")
    fun verifyEmail(@Body request: VerifyEmailRequest): Call<ApiResponse<MessageResponse>>
    
    @POST("auth/verify-phone")
    fun verifyPhone(@Body request: VerifyPhoneRequest): Call<ApiResponse<MessageResponse>>
    
    @POST("auth/login")
    fun loginUser(@Body request: LoginRequest): Call<ApiResponse<LoginResponse>>
}