package com.example.stalker_strike

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.stalker_strike.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.pavleprica.kotlin.cache.time.based.LongTimeBasedCache
import io.github.pavleprica.kotlin.cache.time.based.longTimeBasedCache
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random


// cache 0 - HP, 1 - Radiation, 2 - Anomaly
val CACHE: LongTimeBasedCache<Int, String> = longTimeBasedCache()
var AVAILABLE_WIFI_SCANS: ArrayList<Boolean> = ArrayList()

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        private lateinit var wifiManagerInstance: WifiManager

        fun getWifiManagerInstance(context: Context): WifiManager {
            if (!::wifiManagerInstance.isInitialized) {
                wifiManagerInstance = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            }
            return wifiManagerInstance
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private lateinit var signalList: ArrayList<Signal>

    private var radiationDamage: Float = 1.0F
    private var anomalyDamage: Float = 5.0F

    private var wifiScanDelaySeconds: Long = 30
    private var wifiThrottlingLimitSeconds: Long = 120
    private var healPoints: Float = 100.0F
    private val random: Random = Random

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CACHE[0].getOrNull() != null) {
            healPoints = CACHE[0].get().toFloat()
            effectHP()
        } else {
            loadHealPoints()
            CACHE[0] = healPoints.toString()
        }

        CACHE[1] = "0"
        CACHE[2] = "0"

        val permissions = arrayOf(
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_WIFI_STATE,
            permission.ACCESS_BACKGROUND_LOCATION,
            permission.ACCESS_COARSE_LOCATION,
            permission.FOREGROUND_SERVICE,
            permission.CAMERA,
            // permission.BLUETOOTH_CONNECT,
            // permission.BLUETOOTH_SCAN
        )

        ActivityCompat.requestPermissions(this, permissions, 0)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController: NavController = navHostFragment.navController

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
            Toast.makeText(this, "Enable WiFi to work with this app", Toast.LENGTH_LONG).show()
            wifiManager.setWifiEnabled(true)
        }

        // Start foreground service
        val serviceIntent = Intent(this, StalkerStrikeService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            @SuppressLint("MissingPermission")
            override fun run() {
                refreshSignalList()
                mainHandler.postDelayed(this, 5000)
            }
        })

        mainHandler.post(object : Runnable {
            override fun run() {
                effectHP()
                mainHandler.postDelayed(this, 1000)
            }
        })

        // Empty available scans
        mainHandler.post(object : Runnable {
            override fun run() {
                AVAILABLE_WIFI_SCANS = ArrayList()
                mainHandler.postDelayed(this, wifiThrottlingLimitSeconds * 1000)
            }
        })

        // default WIFI scan once in 30 seconds
        mainHandler.post(object : Runnable {
            override fun run() {
                if (AVAILABLE_WIFI_SCANS.size <= 4) {
                    AVAILABLE_WIFI_SCANS.add(true)
                    wifiManager.startScan()
                    mainHandler.postDelayed(this, wifiScanDelaySeconds * 1000)
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [permission.ACCESS_WIFI_STATE, permission.ACCESS_FINE_LOCATION])
    private fun refreshSignalList() {
        Log.i(TAG, "refreshWifiList: ")

        val networkList: ArrayList<Signal> = ArrayList()
        val scanResults: List<ScanResult> = wifiManager.scanResults
        scanResults.forEach {
            Log.i(TAG, "refreshWifiList: scanResults=$it")
            val ssid = it.SSID
            val waveLevel = WifiManager.calculateSignalLevel(it.level, 5)

            networkList.add(Signal(ssid, waveLevel))
        }

        // TODO try scan for BLE
        // bluetoothLeScanner?.startScan(bleScanCallback)

        this.signalList = networkList
    }

    private fun effectHP() {
        if (signalList.isEmpty()) {
            return
        }

        val environment = signalList.filter {
            it == SIGNAL_HEAL
                    || it == SIGNAL_ANOMALY
                    || it == SIGNAL_RADIATION
        }

        val availableRadBuffs = BUFFS.filter { it.radiationProtection > 0 }
            .fold(0) { acc, next -> acc + next.radiationProtection }
        val availableAnomalyBuffs = BUFFS.filter { it.anomalyProtection > 0 }
            .fold(0) { acc, next -> acc + next.anomalyProtection }

        if (environment.isNotEmpty()) {
            for (effect in environment) {
                when (effect) {
                    SIGNAL_HEAL -> {
                        healPoints += 1
                        CACHE[1] = "0"
                        CACHE[2] = "0"
                    }

                    SIGNAL_RADIATION -> {
                        if (effect.waveLevel < 3) {
                            healPoints -= (radiationDamage - ((availableRadBuffs / 100.0) * radiationDamage)).toFloat()
                            CACHE[1] = random.nextInt(10, 30).toString()
                        }
                        if (effect.waveLevel >= 3) {
                            healPoints -= (radiationDamage - ((availableRadBuffs / 100.0) * (radiationDamage + 1))).toFloat()
                            CACHE[1] = random.nextInt(30, 50).toString()
                        }
                    }

                    SIGNAL_ANOMALY -> {
                        if (effect.waveLevel >= 3) {
                            healPoints -= (anomalyDamage - ((availableAnomalyBuffs / 100.0) * anomalyDamage)).toFloat()
                            val anomalyLevel = random.nextInt(60, 100)
                            CACHE[2] = anomalyLevel.toString()
                        }
                    }
                }
            }
        } else {
            CACHE[1] = random.nextInt(0, 9).toString()
            CACHE[2] = "0"
        }

        if (healPoints <= 0) {
            healPoints = 0.0F
            // TODO add alert and block other actions until healed
        }

        Log.i(TAG, "Current HP level: $healPoints")
        CACHE[0] = healPoints.toString()
        saveHealPoints(healPoints)
    }

    private fun saveHealPoints(healPoints: Float) {
        val sharedPreferences = this.getSharedPreferences("hp_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("healPoints", healPoints)
        editor.apply()
    }

    private fun loadHealPoints() {
        val sharedPreferences = this.getSharedPreferences("hp_prefs", Context.MODE_PRIVATE)
        this.healPoints = sharedPreferences.getFloat("healPoints", 100.0f)
    }
}