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
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.stalker_strike.databinding.ActivityMainBinding
import com.example.stalkerstrike.BUFFS
import com.example.stalkerstrike.WIFI_ANOMALY
import com.example.stalkerstrike.WIFI_HEAL
import com.example.stalkerstrike.WIFI_RADIATION
import com.example.stalkerstrike.Wifi
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.pavleprica.kotlin.cache.time.based.LongTimeBasedCache
import io.github.pavleprica.kotlin.cache.time.based.longTimeBasedCache
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random


// cache 0 - HP, 1 - Radiation, 2 - Anomaly
val CACHE: LongTimeBasedCache<Int, String> = longTimeBasedCache()

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

    private var HP: Float = 100.0F
    private val random: Random = Random

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CACHE[0].getOrNull() != null) {
            HP = CACHE[0].get().toFloat()
            effectHP()
        } else {
            CACHE[0] = HP.toString()
        }

        CACHE[1] = "0"
        CACHE[2] = "0"

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

        mainHandler.post(object : Runnable {
            override fun run() {
                wifiManager.startScan()
                mainHandler.postDelayed(this, 30000)
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
            val waveLevel = WifiManager.calculateSignalLevel(it.level, 5)

            networkList.add(Wifi(ssid, waveLevel))
        }

        wifiList = networkList
    }

    private fun effectHP() {
        val environment = wifiList.filter {
            it == WIFI_HEAL
                    || it == WIFI_ANOMALY
                    || it == WIFI_RADIATION
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
                        CACHE[1] = "0"
                        CACHE[2] = "0"
                    }

                    WIFI_RADIATION -> {
                        if (effect.waveLevel < 3) {
                            HP -= (radiationDamage - ((availableRadBuffs / 100.0) * radiationDamage)).toFloat()
                            CACHE[1] = random.nextInt(10, 30).toString()
                        }
                        if (effect.waveLevel >= 3) {
                            HP -= (radiationDamage - ((availableRadBuffs / 100.0) * (radiationDamage + 1))).toFloat()
                            CACHE[1] = random.nextInt(30, 50).toString()
                        }
                    }

                    WIFI_ANOMALY -> {
                        if (effect.waveLevel >= 3) {
                            HP -= (anomalyDamage - ((availableAnomalyBuffs / 100.0) * anomalyDamage)).toFloat()
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

        if (HP <= 0) {
            HP = 0.0F
            // TODO add alert and block other actions
        }

        Log.i(TAG, "Current HP level: $HP")
        CACHE[0] = HP.toString()
    }
}