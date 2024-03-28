package com.project.stalker_strike.ui.scanner

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.project.stalker_strike.BUFFS
import com.project.stalker_strike.Buff
import com.project.stalker_strike.R
import com.project.stalker_strike.databinding.FragmentScannerBinding
import com.google.gson.Gson
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import java.util.UUID

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var barcodeView: CompoundBarcodeView
    private lateinit var navController: NavController


    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            val textResult = result.text
            try {
                val buff: Buff = Gson().fromJson(textResult, Buff::class.java)

                BUFFS.removeIf { it.type == buff.type }

                if (buff.type == "medkit" || buff.type == "antirad") {
                    buff.id = generateRandomUUID()
                }

                BUFFS.add(buff)

                navController.navigate(R.id.action_scannerFragment_to_buffsFragment)
            } catch (e: Throwable) {
                showToast(requireContext(), "Невірний код!")
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            // Empty implementation
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        navController =
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main)

        barcodeView = binding.barcodeScanner
        barcodeView.decodeContinuous(barcodeCallback)

        return root
    }

    private fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }

    private fun generateRandomUUID(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}