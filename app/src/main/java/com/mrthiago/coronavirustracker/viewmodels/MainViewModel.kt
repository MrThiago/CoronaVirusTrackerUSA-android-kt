package com.mrthiago.coronavirustracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrthiago.coronavirustracker.data.repository.BaseRepository
import com.mrthiago.coronavirustracker.data.CovidData
import com.mrthiago.coronavirustracker.utilities.DispatcherProvider
import com.mrthiago.coronavirustracker.utilities.states.NetworkState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BaseRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    sealed class CovidDataState {
        class SuccessNationalData(val data: List<CovidData>) : CovidDataState()
        class SuccessStateData(val data: Map<String, List<CovidData>>) : CovidDataState()
        class Failure(val message: String = "There was an Error") : CovidDataState()
        class NoData(val message: String = "There is No Data") : CovidDataState()
        object Empty : CovidDataState()
        object Loading : CovidDataState()
    }

    private val _serviceNationalData = MutableStateFlow<CovidDataState>(CovidDataState.Empty)
    val serviceNationalData: StateFlow<CovidDataState> = _serviceNationalData

    private val _perStateDailyData = MutableStateFlow<CovidDataState>(CovidDataState.Empty)
    val perStateDailyData: StateFlow<CovidDataState> = _perStateDailyData


    fun getNationalData() {
        viewModelScope.launch(dispatchers.io) {
            _serviceNationalData.value = CovidDataState.Loading

            when (val result = repository.getServiceNationalData()) {
                is NetworkState.Success -> {
                    if (result.data.isEmpty()) {
                        _serviceNationalData.value = CovidDataState.NoData()
                    } else {
                        _serviceNationalData.value = CovidDataState.SuccessNationalData(result.data)
                    }
                }
                is NetworkState.Error -> {
                    _serviceNationalData.value = CovidDataState.Failure(result.error)
                }
            }
        }
    }

    fun getStateData() {
        viewModelScope.launch(dispatchers.io) {
            _perStateDailyData.value = CovidDataState.Loading

            when (val result = repository.getServiceStatesData()) {
                is NetworkState.Success -> {
                    if (result.data.isEmpty()) {
                        _perStateDailyData.value = CovidDataState.NoData()
                    } else {
                        _perStateDailyData.value = CovidDataState.SuccessStateData(result.data)
                    }
                }
                is NetworkState.Error -> {
                    _perStateDailyData.value = CovidDataState.Failure(result.error)
                }
            }
        }
    }
}