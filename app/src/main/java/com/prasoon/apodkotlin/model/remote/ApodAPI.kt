package com.prasoon.apodkotlin.model.remote

import com.prasoon.apodkotlin.model.ApodModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApodAPI {
    @GET("/planetary/apod")
    suspend fun getApodCurrentDate(
        @Query("api_key") api_key: String
    ) : Response<ApodModel>

    @GET("/planetary/apod")
    suspend fun getApodCustomDate(
            @Query("api_key") api_key: String,
            @Query("date") date: String?
    ) : Response<ApodModel>
}