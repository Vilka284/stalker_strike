package com.example.stalker_strike.ui.detector

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.stalker_strike.CACHE
import com.example.stalker_strike.databinding.FragmentDetectorBinding
import kotlin.math.roundToInt

class DetectorFragment : Fragment() {

    private var _binding: FragmentDetectorBinding? = null
    private val handler = Handler(Looper.getMainLooper())

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handler.postDelayed(object : Runnable {
            override fun run() {
                updateUI()
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun updateUI() {
        val healPoints = CACHE[0].get().toFloat().roundToInt()

        binding.hpBar.progress = healPoints
        binding.hpValue.text = healPoints.toString()


        val radiationSignal = CACHE[1].get().toInt()
        val anomalySignal = CACHE[2].get().toInt()
        var description = "Рівень радіації в нормі"
        var displaySignal = radiationSignal

        if (radiationSignal < 10) {
            description = "Рівень радіації в нормі"
        }
        if (radiationSignal in 10..20) {
            description = "Рівень радіації підвищений"
        }
        if (radiationSignal in 20..39) {
            description = "Рівень радіації високий"
        }
        if (radiationSignal >= 40) {
            description = "Рівень радіації дуже високий!"
        }

        if (anomalySignal >= 60) {
            description = "Невідомий сигнал, покиньте територію!"
            displaySignal = anomalySignal
        }

        binding.detector.text = displaySignal.toString()
        binding.detectorDescription.text = description
    }

}