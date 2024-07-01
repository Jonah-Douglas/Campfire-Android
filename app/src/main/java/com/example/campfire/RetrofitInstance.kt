package com.example.campfire

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    val api: CampfireAPI by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampfireAPI::class.java)
    }
}