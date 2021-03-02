package com.mrthiago.coronavirustracker.api

import com.mrthiago.coronavirustracker.data.CovidData
import retrofit2.Response
import retrofit2.http.GET

interface CovidService {

    @GET("us/daily.json")
    suspend fun getNationalData(): Response<List<CovidData>>

    @GET("states/daily.json")
    suspend fun getStatesData(): Response<List<CovidData>>
}