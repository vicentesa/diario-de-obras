package com.example.diarioobras.data

import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String,
        @Query("language") language: String = "pt-BR"
    ): GeocodingResponse
}