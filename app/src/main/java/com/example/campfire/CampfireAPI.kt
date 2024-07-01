package com.example.campfire

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface CampfireAPI {
    @GET("/api/v1/users")
    suspend fun getUsers(@Query("skip") skip: Int = 0, @Query("limit") limit: Int = 100): Response<List<User>>

    @POST("/Users")
    suspend fun createUser(@Body user: User): Response<User>

    @POST("/api/v1/login/access-token")
    suspend fun login(@Body user: RequestBody): Response<Token>
}
