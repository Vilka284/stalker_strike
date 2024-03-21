package com.example.stalker_strike.ui.detector

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.stalker_strike.AVAILABLE_WIFI_SCANS
import com.example.stalker_strike.CACHE
import com.example.stalker_strike.MainActivity
import com.example.stalker_strike.R
import com.example.stalker_strike.databinding.FragmentDetectorBinding
import com.example.stalker_strike.refreshSignalList
import com.example.stalker_strike.signalList
import kotlin.jvm.optionals.getOrDefault
import kotlin.math.roundToInt
import kotlin.random.Random

class DetectorFragment : Fragment() {

    private var _binding: FragmentDetectorBinding? = null
    private val viewModel: ButtonStateViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val random: Random = Random
    private var glitch: Boolean = false

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val healPoints = CACHE[0].getOrDefault("100.0").toFloat().roundToInt()
        binding.hpBar.progress = healPoints
        binding.hpValue.text = healPoints.toString()

        val startManualScanButton = view.findViewById<Button>(R.id.startManualScanButton)

        if (!viewModel.buttonEnabled) {
            startManualScanButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
        } else {
            startManualScanButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
            startManualScanButton.isEnabled = true
        }

        startManualScanButton.setOnClickListener {
            startManualScanButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            startManualScanButton.isEnabled = false
            viewModel.buttonEnabled = false

            AVAILABLE_WIFI_SCANS.add(true)

            Handler().postDelayed({
                startManualScanButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                startManualScanButton.isEnabled = true
                viewModel.buttonEnabled = true
                if (AVAILABLE_WIFI_SCANS.size <= 4) {
                    val wifiManager = MainActivity.getWifiManagerInstance(requireContext())
                    wifiManager.startScan()
                    signalList = refreshSignalList(wifiManager)
                }
            }, 15000)
        }

        handler.postDelayed(object : Runnable {
            override fun run() {
                updateUI(startManualScanButton)
                handler.postDelayed(this, 600)
            }
        }, 600)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(startManualScanButton: Button) {
        val healPoints = CACHE[0].get().toFloat().roundToInt()

        binding.hpBar.progress = healPoints
        binding.hpValue.text = healPoints.toString()

        val radiationSignal = CACHE[1].get().toInt()
        val anomalySignal = CACHE[2].get().toInt()
        var description = "Рівень радіації в нормі"
        var displaySignal = radiationSignal
        var color = android.R.color.holo_green_light

        if (radiationSignal < 10) {
            description = "Рівень радіації в нормі"
        }
        if (radiationSignal in 10..20) {
            description = "Рівень радіації підвищений"
        }
        if (radiationSignal in 20..39) {
            description = "Рівень радіації високий"
            color = android.R.color.holo_orange_dark
        }
        if (radiationSignal >= 40) {
            description = "Рівень радіації дуже високий!"
            color = android.R.color.holo_red_light
        }

        if (anomalySignal >= 60) {
            description = "Невідомий сигнал, покиньте територію!"
            displaySignal = anomalySignal
            color = android.R.color.holo_red_light
            glitch = true
        } else {
            glitch = false
        }

        val randomDec = random.nextInt(10, 99)

        binding.detector.text = "$displaySignal.$randomDec"
        binding.detector.setTextColor(resources.getColor(color))
        binding.detectorDescription.text = description

        if (!viewModel.buttonEnabled) {
            startManualScanButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
        } else {
            startManualScanButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
            startManualScanButton.isEnabled = true
        }

        if (AVAILABLE_WIFI_SCANS.size > 4) {
            startManualScanButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            startManualScanButton.isEnabled = false
            viewModel.buttonEnabled = false
        }

        if (AVAILABLE_WIFI_SCANS.size == 0) {
            startManualScanButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
            startManualScanButton.isEnabled = true
            viewModel.buttonEnabled = true
        }
    }
}