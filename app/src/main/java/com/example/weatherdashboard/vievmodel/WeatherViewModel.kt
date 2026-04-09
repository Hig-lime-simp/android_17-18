package com.example.weatherdashboard.vievmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherdashboard.data.WeatherData
import com.example.weatherdashboard.data.WeatherRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()

    private val _weatherState = MutableStateFlow(WeatherData())
    val weatherState: StateFlow<WeatherData> = _weatherState.asStateFlow()

    init {
        loadWeatherData()
        startAutoRefresh()
    }
    /**
     * Демонстрация работы диспетчеров корутин:
     *
     * ┌─ viewModelScope.launch { }
     * │   Запускается на: Dispatchers.Main (по умолчанию для ViewModel)
     * │
     * │   ┌─ coroutineScope { }
     * │   │   Группирует дочерние корутины, наследует Dispatchers.Main
     * │   │
     * │   │   ├─ async { repository.fetchTemperature() }
     * │   │   │   Выполняется на: Dispatchers.IO (внутри репозитория)
     * │   │   │   Имитация сетевого запроса (~2 секунды)
     * │   │   │
     * │   │   ├─ async { repository.fetchHumidity() }
     * │   │   │   Выполняется на: Dispatchers.IO
     * │   │   │   Имитация сетевого запроса (~1.5 секунды)
     * │   │   │
     * │   │   ├─ async { repository.fetchWindSpeed() }
     * │   │   │   Выполняется на: Dispatchers.IO
     * │   │   │   Имитация сетевого запроса (~1 секунда)
     * │   │   │
     * │   │   └─ calculateWeatherIndex(temp, humidity, wind)
     * │   │       Выполняется на: Dispatchers.Default
     * │   │       Тяжёлые вычисления (1_000_000 итераций)
     * │   │       withContext(Dispatchers.Default) внутри функции
     * │   │
     * │   │   После завершения всех async:
     * │   │   └─ _weatherState.value = WeatherData(...)
     * │   │       Выполняется на: Dispatchers.Main
     * │   │       Обновление UI (обязательно на Main!)
     * │   │
     * │   └─ Обработка ошибок в catch { }
     * │       Также выполняется на Dispatchers.Main
     * │
     * └─ Результат:
     *     • Все сетевые запросы выполняются параллельно (~2 сек вместо 4.5)
     *     • Тяжёлые вычисления не блокируют UI-поток
     *     • Обновление интерфейса происходит плавно
     *     • Приложение остаётся отзывчивым при любой нагрузке
     */
    fun loadWeatherData() {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(
                isLoading = true,
                error = null,
                loadingProgress = "Запуск загрузки..."
            )

            try {
                coroutineScope {
                    val tempDeferred = async { repository.fetchTemperature() }
                    val humDeferred = async { repository.fetchHumidity() }
                    val windDeferred = async { repository.fetchWindSpeed() }

                    val temperature = tempDeferred.await()
                    val humidity = humDeferred.await()
                    val windSpeed = windDeferred.await()

                    _weatherState.value = _weatherState.value.copy(
                        loadingProgress = "Вычисление индекса погоды..."
                    )

                    val weatherIndex = repository.calculateWeatherIndex(
                        temperature,
                        humidity,
                        windSpeed
                    )

                    _weatherState.value = WeatherData(
                        temperature = temperature,
                        humidity = humidity,
                        windSpeed = windSpeed,
                        weatherIndex = weatherIndex,
                        isLoading = false,
                        error = null,
                        loadingProgress = "Загрузка завершена!"
                    )
                }
            } catch (e: Exception) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}",
                    loadingProgress = ""
                )
            }
        }
    }

    fun toggleErrorSimulation() {
        repository.toggleErrorSimulation()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            flow {
                while (true) {
                    delay(10000) // 10 секунд
                    emit(Unit)
                }
            }.collect {
                loadWeatherData()
            }
        }
    }
}