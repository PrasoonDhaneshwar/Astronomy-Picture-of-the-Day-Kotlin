package com.prasoon.apodkotlin.model

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
// Display date, explanation, Title and the image / video of the day
// Default usage
// date format is YYYY-MM-DD
// https://api.nasa.gov/planetary/apod?api_key=XqN37uhbQmRUqsm2nTFk4rsugtM2Ibe0YUS9HDE3

// https://api.nasa.gov/planetary/apod?api_key=XqN37uhbQmRUqsm2nTFk4rsugtM2Ibe0YUS9HDE3&date=2021-01-01

// private const val BASE_URL = "https://api.nasa.gov/planetary/apod"

/*
{
   "date":"2021-12-29",
   "explanation":"What and where are these large ovals? They are rotating storm clouds on Jupiter imaged last month by NASA's Juno spacecraft. In general, higher clouds are lighter in color, and the lightest clouds visible are the relatively small clouds that dot the lower oval. At 50 kilometers across, however, even these light clouds are not small.  They are so high up that they cast shadows on the swirling oval below. The featured image has been processed to enhance color and contrast. Large ovals are usually regions of high pressure that span over 1000 kilometers and can last for years. The largest oval on Jupiter is the Great Red Spot (not pictured), which has lasted for at least hundreds of years. Studying cloud dynamics on Jupiter with Juno images enables a better understanding of dangerous typhoons and hurricanes on Earth.   Follow APOD in English on: Facebook,  Instagram, Podcast, Reddit, or Twitter",
   "hdurl":"https://apod.nasa.gov/apod/image/2112/JupiterStorms_JunoGill_1024.jpg",
   "media_type":"image",
   "service_version":"v1",
   "title":"Giant Storms and High Clouds on Jupiter",
   "url":"https://apod.nasa.gov/apod/image/2112/JupiterStorms_JunoGill_1024.jpg"
}
*/

interface ApodInterface {
    @GET("/planetary/apod")
    fun getApodCurrentDate(
        @Query("api_key") api_key: String
    ) : Call<ApodModel>

    @GET("/planetary/apod")
    fun getApodCustomDate(
            @Query("api_key") api_key: String,
            @Query("date") date: String
    ) : Call<ApodModel>
}