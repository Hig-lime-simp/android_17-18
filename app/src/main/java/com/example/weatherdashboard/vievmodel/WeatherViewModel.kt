package com.example.weatherdashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherdashboard.data.WeatherData
import com.example.weatherdashboard.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()

    private val _weatherState = MutableStateFlow(WeatherData())
    val weatherState: StateFlow<WeatherData> = _weatherState.asStateFlow()

    init {
        loadWeatherData()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            // Начинаем загрузку
            _weatherState.value = _weatherState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                // Загружаем по очереди (медленно!)
                val temperature = repository.fetchTemperature() // 2 сек
                _weatherState.value = _weatherState.value.copy(temperature = temperature)

                val humidity = repository.fetchHumidity() // + 1.5 сек
                _weatherState.value = _weatherState.value.copy(humidity = humidity)

                val windSpeed = repository.fetchWindSpeed() // + 1 сек
                _weatherState.value = _weatherState.value.copy(windSpeed = windSpeed)

                // Всё загружено
                _weatherState.value = _weatherState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }
}