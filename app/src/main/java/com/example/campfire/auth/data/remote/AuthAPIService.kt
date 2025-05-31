package com.example.campfire.auth.data.remote

import com.example.campfire.auth.data.remote.dto.request.RegisterRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyEmailRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyPhoneRequest
import com.example.campfire.auth.data.remote.dto.response.ApiResponse
import com.example.campfire.auth.data.remote.dto.response.MessageResponse
import com.example.campfire.auth.data.remote.dto.response.RegisterResponse
import com.example.campfire.auth.data.remote.dto.response.TokenResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface AuthApiService {
    // JD TODO: Remove these once I get new resources connected in FastAPI
    // JD TODO: Alter responses to include generic information about request instead of simply the header (might be ok for logging in- double check, others def not tho I think)
//    @GET("/api/v1/users")
//    suspend fun getUsers(@Query("skip") skip: Int = 0, @Query("limit") limit: Int = 100): Response<List<User>>
//
//    @POST("/Users")
//    suspend fun createUser(@Body user: User): Response<User>
    
    @POST("auth/register")
    fun registerUser(@Body request: RegisterRequest): Call<ApiResponse<RegisterResponse>> // Assuming ApiResponse is a generic wrapper
    
    @POST("auth/verify-email")
    fun verifyEmail(@Body request: VerifyEmailRequest): Call<ApiResponse<MessageResponse>>
    
    @POST("auth/verify-phone")
    fun verifyPhone(@Body request: VerifyPhoneRequest): Call<ApiResponse<MessageResponse>>
    
    @FormUrlEncoded
    @POST("/api/v1/auth/login")
    suspend fun loginUser(
        @Field("username") username: String,
        @Field("password") password: String,
    ): Response<TokenResponse>
}