package com.mrthiago.coronavirustracker.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mrthiago.coronavirustracker.R
import com.mrthiago.coronavirustracker.adapters.CovidSparkAdapter
import com.mrthiago.coronavirustracker.data.CovidData
import com.mrthiago.coronavirustracker.databinding.ActivityMainBinding
import com.mrthiago.coronavirustracker.viewmodels.MainViewModel
import com.mrthiago.coronavirustracker.views.Metric
import com.mrthiago.coronavirustracker.views.TimeScale
import com.robinhood.ticker.TickerUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var rootView: View
    private lateinit var adapter: CovidSparkAdapter

    // Data
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        rootView = binding.root
        setContentView(rootView)

        // NationalData
        lifecycleScope.launchWhenStarted {
            mainViewModel.serviceNationalData.collect { dataStatus ->
                when (dataStatus) {
                    is MainViewModel.CovidDataState.SuccessNationalData -> {
                        Log.i("MainActivity", "NationalData -> SuccessNationalData")
                        setupEventListeners()
                        nationalDailyData = dataStatus.data
                        updateDisplayWithData(nationalDailyData)
                    }
                    is MainViewModel.CovidDataState.Empty -> Log.w("MainActivity", "NationalData -> Empty")
                    is MainViewModel.CovidDataState.Failure -> Log.e("MainActivity", "NationalData -> Failure ${dataStatus.message}")
                    is MainViewModel.CovidDataState.Loading -> Log.w("MainActivity", "NationalData -> Loading")
                    is MainViewModel.CovidDataState.NoData -> Log.w("MainActivity", "NationalData -> NoData")
                    is MainViewModel.CovidDataState.SuccessStateData -> Log.w("MainActivity", "NationalData -> SuccessStateData")
                }
            }
        }

        // StateData
        lifecycleScope.launch {
            mainViewModel.perStateDailyData.collect { dataStatus ->
                when (dataStatus) {
                    is MainViewModel.CovidDataState.SuccessStateData -> {
                        Log.i("MainActivity", "StateData -> SuccessStateData")
                        setupEventListeners()
                        perStateDailyData = dataStatus.data
                        updateSpinnerWithStateData(perStateDailyData.keys)
                    }
                    is MainViewModel.CovidDataState.Empty -> Log.w("MainActivity", "StateData -> Empty")
                    is MainViewModel.CovidDataState.Failure -> Log.e("MainActivity", "StateData -> Failure : ${dataStatus.message}")
                    is MainViewModel.CovidDataState.Loading -> Log.w("MainActivity", "StateData -> Loading")
                    is MainViewModel.CovidDataState.NoData -> Log.w("MainActivity", "StateData -> NoData")
                    is MainViewModel.CovidDataState.SuccessNationalData -> Log.w("MainActivity", "StateData -> SuccessNationalData")
                }
            }
        }

        // Call Apis
        mainViewModel.getNationalData()
        mainViewModel.getStateData()
    }

    private fun setupEventListeners() {
        binding.sparkView.isScrubEnabled = true
        binding.sparkView.setScrubListener { itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
        binding.tickerView.setCharacterLists(TickerUtils.provideNumberList())

        // Respond to radio button selected events
        binding.radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            // Display the last day of the metric
            updateInfoForDate(currentlyShownData.last())
            adapter.notifyDataSetChanged()
        }
        binding.radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, "All (Nationwide)")
        binding.spinnerSelect.attachDataSource(stateAbbreviationList)
        binding.spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        // Update color of the chart
        @ColorRes val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        binding.sparkView.lineColor = colorInt
        binding.tickerView.textColor = colorInt

        // Update metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // Reset number/date shown for most recent date
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        // Create a new SparkAdapter with the data
        adapter = CovidSparkAdapter(dailyData)
        binding.sparkView.adapter = adapter
        // Update radio buttons to select positive cases and max time by default
        binding.radioButtonPositive.isChecked = true
        binding.radioButtonMax.isChecked = true
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        binding.tickerView.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        binding.tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}