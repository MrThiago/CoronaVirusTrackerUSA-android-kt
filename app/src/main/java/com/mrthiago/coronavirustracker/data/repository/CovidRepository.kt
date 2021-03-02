package com.mrthiago.coronavirustracker.data.repository

import com.mrthiago.coronavirustracker.api.CovidService
import com.mrthiago.coronavirustracker.data.CovidData
import com.mrthiago.coronavirustracker.utilities.states.NetworkState
import javax.inject.Inject

class CovidRepository @Inject constructor(private val service: CovidService) : BaseRepository {

    override suspend fun getServiceNationalData(): NetworkState<List<CovidData>> {
        return try {
            val response = service.getNationalData()
            val result = response.body()

            if (response.isSuccessful && result != null) {
                NetworkState.Success(result.reversed())
            } else {
                NetworkState.Error(response.message())
            }
        } catch (e: Exception) {
            NetworkState.Error(e.message ?: "There was an Error")
        }
    }

    override suspend fun getServiceStatesData(): NetworkState<Map<String, List<CovidData>>> {
        return try {
            val response = service.getStatesData()
            val result = response.body()

            if (response.isSuccessful && result != null) {
                val stateData: Map<String, List<CovidData>> = result
                    .filter { it.dateChecked != null }
                    .map { // State data may have negative deltas, which don't make sense to graph
                        CovidData(
                            it.dateChecked,
                            it.positiveIncrease.coerceAtLeast(0),
                            it.negativeIncrease.coerceAtLeast(0),
                            it.deathIncrease.coerceAtLeast(0),
                            it.state
                        )
                    }
                    .reversed()
                    .groupBy { it.state }

                NetworkState.Success(stateData)
            } else {
                NetworkState.Error(response.message())
            }
        } catch (e: Exception) {
            NetworkState.Error(e.message ?: "There was an Error")
        }
    }
}