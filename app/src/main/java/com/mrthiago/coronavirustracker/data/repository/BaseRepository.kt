package com.mrthiago.coronavirustracker.data.repository

import com.mrthiago.coronavirustracker.data.CovidData
import com.mrthiago.coronavirustracker.utilities.states.NetworkState

interface BaseRepository {

    suspend fun getServiceNationalData(): NetworkState<List<CovidData>>

    suspend fun getServiceStatesData(): NetworkState<Map<String, List<CovidData>>>
}