package com.example.campfire

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


// JD TODO: Not sure if the .client is necessary or correct, and if it should be public (private in example, but can't access here then (Do I need two classes?)
object RetrofitInstance {
    val api: CampfireAPI by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(NetworkUtil().client.build())
            .build()
            .create(CampfireAPI::class.java)
    }
}