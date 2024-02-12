package com.example.stalker_strike

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.stalker_strike.databinding.ActivityMainBinding
import com.example.stalkerstrike.WIFI_ANOMALY
import com.example.stalkerstrike.WIFI_HEAL
import com.example.stalkerstrike.WIFI_RADIATION
import com.example.stalkerstrike.Wifi
import com.example.stalkerstrike.BUFFS
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.pavleprica.kotlin.cache.time.based.LongTimeBasedCache
import io.github.pavleprica.kotlin.cache.time.based.longTimeBasedCache


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiList: ArrayList<Wifi>

    private var radiationDamage: Float = 1.0F
    private var anomalyDamage: Float = 5.0F

    val cache: LongTimeBasedCache<Int, String> = longTimeBasedCache()
    var HP: Float = 100.0F

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cache[0] = HP.toString()

        val permissions = arrayOf(
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_WIFI_STATE,
            permission.ACCESS_BACKGROUND_LOCATION,
            permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(this, permissions, 0)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_scanner, R.id.navigation_detector, R.id.navigation_buffs
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Enable WiFi to work with this app", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            @SuppressLint("MissingPermission")
            override fun run() {
                refreshWifiList()
                mainHandler.postDelayed(this, 5000)
            }
        })

        mainHandler.post(object : Runnable {
            override fun run() {
                effectHP()
                mainHandler.postDelayed(this, 1000)
            }
        })
    }

    @RequiresPermission(allOf = [permission.ACCESS_WIFI_STATE, permission.ACCESS_FINE_LOCATION])
    private fun refreshWifiList() {
        Log.i(TAG, "refreshWifiList: ")

        val networkList: ArrayList<Wifi> = ArrayList()
        val scanResults: List<ScanResult> = wifiManager.scanResults
        scanResults.forEach {
            Log.i(TAG, "refreshWifiList: scanResults=$it")
            val ssid = it.SSID
            val waveLevel = WifiManager.calculateSignalLevel(it.level, 5);

            networkList.add(Wifi(ssid, waveLevel))
        }

        wifiList = networkList
    }

    private fun effectHP() {
        val environment = wifiList.filter {
            it.ssid.equals(WIFI_HEAL)
                    || it.ssid.equals(WIFI_ANOMALY)
                    || it.ssid.equals(WIFI_RADIATION)
        }


        val availableRadBuffs = BUFFS.filter { it.radiationProtection > 0 }
            .fold(0) { acc, next -> acc + next.radiationProtection }
        val availableAnomalyBuffs = BUFFS.filter { it.anomalyProtection > 0 }
            .fold(0) { acc, next -> acc + next.anomalyProtection }
        if (environment.isNotEmpty()) {
            for (effect in environment) {
                when (effect) {
                    WIFI_HEAL -> {
                        HP += 1
                    }

                    WIFI_RADIATION -> {
                        HP -= (availableRadBuffs / 100) * radiationDamage
                    }

                    WIFI_ANOMALY -> {
                        HP -= (availableAnomalyBuffs / 100) * anomalyDamage
                    }
                }
            }
        }

        Log.i(TAG, "Current HP level: $HP")
        cache[0] = HP.toString()
    }
}