package com.example.campfire.auth.data.remote

import com.example.campfire.auth.data.remote.dto.request.RegisterRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyEmailRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyPhoneRequest
import com.example.campfire.auth.data.remote.dto.response.ApiResponse
import com.example.campfire.auth.data.remote.dto.response.MessageResponse
import com.example.campfire.auth.data.remote.dto.response.RegisterApiResponse
import com.example.campfire.auth.data.remote.dto.response.TokenResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface AuthApiService {
    @POST("auth/register")
    fun registerUser(@Body request: RegisterRequest): Response<RegisterApiResponse>
    
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun loginUser(
        @Field("username") username: String,
        @Field("password") password: String,
    ): Response<TokenResponse>
    
    @POST("auth/logout")
    suspend fun logoutUser(
    ): Response<Unit>
    
    @POST("auth/verify-email")
    fun verifyEmail(@Body request: VerifyEmailRequest): Call<ApiResponse<MessageResponse>>
    
    @POST("auth/verify-phone")
    fun verifyPhone(@Body request: VerifyPhoneRequest): Call<ApiResponse<MessageResponse>>
}