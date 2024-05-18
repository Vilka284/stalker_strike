package com.project.stalker_strike.ui.detector

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
import com.project.stalker_strike.AVAILABLE_WIFI_SCANS
import com.project.stalker_strike.CACHE
import com.project.stalker_strike.MainActivity
import com.project.stalker_strike.R
import com.project.stalker_strike.databinding.FragmentDetectorBinding
import com.project.stalker_strike.refreshSignalList
import com.project.stalker_strike.signalList
import kotlin.jvm.optionals.getOrDefault
import kotlin.math.roundToInt
import kotlin.random.Random

@Suppress("DEPRECATION")
class DetectorFragment : Fragment() {

    private lateinit var startManualScanButton: Button

    private var _binding: FragmentDetectorBinding? = null
    private val viewModel: ButtonStateViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val random: Random = Random
    private var glitch: Boolean = false

    private val binding get() = _binding!!

    private var isHandlerPosted = false
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateUI(startManualScanButton)
            if (isHandlerPosted) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun startHandler() {
        if (!isHandlerPosted) {
            handler.postDelayed(updateRunnable, 200)
            isHandlerPosted = true
        }
    }

    fun stopHandler() {
        handler.removeCallbacks(updateRunnable)
        isHandlerPosted = false
    }

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

        startManualScanButton = view.findViewById(R.id.startManualScanButton)

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

        startHandler()
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

        if (radiationSignal == 0) {
            description = "Рівень радіації в нормі"
            displaySignal = random.nextInt(0, 10)
        }
        if (radiationSignal in 1..2) {
            description = "Рівень радіації підвищений"
            color = android.R.color.holo_orange_light
            displaySignal = random.nextInt(11, 20)
        }
        if (radiationSignal in 2..3) {
            description = "Рівень радіації високий"
            color = android.R.color.holo_orange_dark
            displaySignal = random.nextInt(21, 40)
        }
        if (radiationSignal >= 4) {
            description = "Рівень радіації дуже високий!"
            color = android.R.color.holo_red_light
            displaySignal = random.nextInt(41, 60)
        }

        if (anomalySignal >= 3) {
            if (anomalySignal == 3) {
                displaySignal = random.nextInt(100, 200)
            }
            if (anomalySignal == 4) {
                displaySignal = random.nextInt(200, 300)
            }
            if (anomalySignal == 5) {
                displaySignal = random.nextInt(300, 400)
            }
            description = "Невідомий сигнал, покиньте територію!"
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

    override fun onDestroyView() {
        super.onDestroyView()
        stopHandler()
    }
}