package com.project.stalker_strike

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.project.stalker_strike.databinding.ActivityMainBinding
import com.project.stalker_strike.ui.buffs.AntiAnomalyProtector
import com.project.stalker_strike.ui.buffs.AntiRadProtector
import com.project.stalker_strike.ui.buffs.BuffsFragment
import com.project.stalker_strike.ui.buffs.HealPointsUpdater
import io.github.pavleprica.kotlin.cache.time.based.LongTimeBasedCache
import io.github.pavleprica.kotlin.cache.time.based.longTimeBasedCache
import kotlin.jvm.optionals.getOrNull


// cache 0 - HP, 1 - Radiation, 2 - Anomaly
val CACHE: LongTimeBasedCache<Int, String> = longTimeBasedCache()
var AVAILABLE_WIFI_SCANS: ArrayList<Boolean> = ArrayList()

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), HealPointsUpdater, AntiRadProtector,
    AntiAnomalyProtector {
    companion object {
        private const val TAG = "MainActivity"

        private lateinit var wifiManagerInstance: WifiManager

        fun getWifiManagerInstance(context: Context): WifiManager {
            if (!::wifiManagerInstance.isInitialized) {
                wifiManagerInstance =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            }
            return wifiManagerInstance
        }
    }

    private val disableRadiationProtector = Runnable {
        radiationProtector = false
        Log.i("RadiationProtector", "Disable radiation protector")
    }

    private val disableAnomalyProtector = Runnable {
        anomalyProtector = false
        Log.i("AnomalyProtector", "Disable anomaly protector")
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private lateinit var navView: BottomNavigationView
    private lateinit var alertDialog: AlertDialog
    private lateinit var buffsFragment: BuffsFragment
    private lateinit var soundManager: SoundManager


    private var radiationDamage: Float = 0.5F
    private var anomalyDamage: Float = 2.0F

    private var wifiScanDelaySeconds: Long = 30
    private var wifiThrottlingLimitSeconds: Long = 120
    private var healPoints: Float = 100.0F
    private var maxHealPoints: Float = 100.0F
    private var regenHealPoints: Float = 0.12F
    private var radiationProtector: Boolean = false
    private var anomalyProtector: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

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

        buffsFragment = BuffsFragment()
        buffsFragment.healPointsUpdater = this
        buffsFragment.antiRadProtector = this

        // TODO add permission check, redirect user to enable wi-fi and location
        val permissions = arrayOf(
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_WIFI_STATE,
            permission.ACCESS_BACKGROUND_LOCATION,
            permission.ACCESS_COARSE_LOCATION,
            permission.FOREGROUND_SERVICE,
            permission.CAMERA,
            permission.WAKE_LOCK,
            permission.VIBRATE
        )

        ActivityCompat.requestPermissions(this, permissions, 0)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navView = binding.navView

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
        try {
            connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(
                    this,
                    "Увімкни локацію, щоб працювати з цим додатком!",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (!wifiManager.isWifiEnabled) {
                Toast.makeText(
                    this,
                    "Увімкни WIFI, щоб працювати з цим додатком!",
                    Toast.LENGTH_LONG
                ).show()
                wifiManager.setWifiEnabled(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Виникла проблема з перевіркою наявних підключень. " +
                        "Надай дозволи додатку на використання WIFI та локації.",
                Toast.LENGTH_LONG
            ).show()
        }

        // init sound manager
        soundManager = SoundManager.getInstance(this)

        // Start foreground service
        val serviceIntent = Intent(this, StalkerStrikeService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        // Build alert
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Ти мертвий!")
        alertDialogBuilder.setMessage("Повертайся на мертвяк та віднови здоров'я там")
        alertDialogBuilder.setCancelable(false)
        alertDialog = alertDialogBuilder.create()

        mainHandler.post(object : Runnable {
            @SuppressLint("MissingPermission")
            override fun run() {
                signalList = refreshSignalList(wifiManager)
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

    private fun effectHP() {
        if (signalList.isEmpty()) {
            return
        }

        Log.i("effectHP", signalList.toString())

        val environment = signalList.filter {
            it == SIGNAL_HEAL
                    || it == SIGNAL_ANOMALY
                    || it == SIGNAL_RADIATION
        }

        val availableRadBuffs =
            BUFFS.filter { it.radiationProtection > 0 && it.type != "medkit" && it.type != "antirad" }
                .fold(0) { acc, next -> acc + next.radiationProtection }
        var availableAnomalyBuffs =
            BUFFS.filter { it.anomalyProtection > 0 && it.type != "medkit" && it.type != "antirad" }
                .fold(0) { acc, next -> acc + next.anomalyProtection }

        if (availableAnomalyBuffs == 0) {
            availableAnomalyBuffs = 1
        }

        if (availableRadBuffs == 0) {
            availableAnomalyBuffs = 1
        }

        if (environment.isNotEmpty()) {
            for (effect in environment) {
                if (effect == SIGNAL_HEAL) {
                    if (effect.waveLevel >= 3) {
                        if (healPoints < maxHealPoints) {
                            healPoints += regenHealPoints
                        }

                        if (healPoints > maxHealPoints) {
                            healPoints = maxHealPoints
                        }

                        CACHE[1] = "0"
                        CACHE[2] = "0"
                        continue
                    }
                }

                if (effect == SIGNAL_ANOMALY) {
                    if (!anomalyProtector) {
                        if (effect.waveLevel >= 3) {
                            healPoints -= (anomalyDamage - ((availableAnomalyBuffs / 100.0) * anomalyDamage)).toFloat()
                        }
                    }

                    CACHE[2] = effect.waveLevel.toString()

                    if (healPoints > 0) {
                        makeSound(SoundManager.SoundType.ANOMALY)
                    }
                }

                if (effect == SIGNAL_RADIATION) {
                    if (!radiationProtector) {
                        if (effect.waveLevel < 3) {
                            healPoints -= (radiationDamage - ((availableRadBuffs / 100.0) * radiationDamage)).toFloat()
                        }
                        if (effect.waveLevel >= 3) {
                            healPoints -= (radiationDamage - ((availableRadBuffs / 100.0) * (radiationDamage + 1))).toFloat()
                        }
                    }

                    CACHE[1] = effect.waveLevel.toString()

                    if (healPoints > 0) {
                        if (effect.waveLevel >= 4) {
                            makeSound(SoundManager.SoundType.RADIATION_STRONG)
                        } else {
                            makeSound(SoundManager.SoundType.RADIATION)
                        }
                    }
                }
            }
        } else {
            CACHE[1] = "0"
            CACHE[2] = "0"
        }

        if ((!navView.isEnabled) and (healPoints > 25)) {
            navView.isEnabled = true
            alertDialog.dismiss()
        }

        if (healPoints <= 0) {
            healPoints = 0.0F

            if (navView.isEnabled) {
                alertDialog.show()
                navView.isEnabled = false
            }
        }

        if (healPoints > 100) {
            healPoints = 100.0F
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
        this.healPoints = sharedPreferences.getFloat("healPoints", maxHealPoints)
    }

    private fun makeSound(soundType: SoundManager.SoundType) {
        if (soundManager.isSoundOnPhoneEnabled()) {
            if (soundType == SoundManager.SoundType.ANOMALY) {
                soundManager.playSoundEffect("anomaly_beep")
            }
            if (soundType == SoundManager.SoundType.RADIATION) {
                soundManager.playSoundEffect("radiation_weak")
            }
            if (soundType == SoundManager.SoundType.RADIATION_STRONG) {
                soundManager.playSoundEffect("radiation_strong")
            }
        } else {
            if (soundType == SoundManager.SoundType.ANOMALY) {
                vibratePhone(400)
            }
            if (soundType == SoundManager.SoundType.RADIATION
                || soundType == SoundManager.SoundType.RADIATION_STRONG) {
                vibratePhone(150)
            }
        }

    }

    private fun vibratePhone(milliseconds: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (vibrator.hasVibrator()) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    milliseconds,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    override fun updateHealPoints(newHealPoints: Int) {
        healPoints += newHealPoints.toFloat()
        if (healPoints > 100) {
            healPoints = 100.0F
        }
        CACHE[0] = healPoints.toString()
    }

    override fun protectFromRadiation(seconds: Int) {
        radiationProtector = true
        Log.i("RadiationProtector", "Enable radiation protector")
        mainHandler.postDelayed(disableRadiationProtector, seconds * 1000L)
    }

    override fun protectFromAnomaly(seconds: Int) {
        radiationProtector = true
        Log.i("AnomalyProtector", "Enable anomaly protector")
        mainHandler.postDelayed(disableAnomalyProtector, seconds * 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.releaseResources()
        Looper.myLooper()?.quitSafely()
    }
}