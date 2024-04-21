package com.project.stalker_strike.ui.buffs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.stalker_strike.BUFFS
import com.project.stalker_strike.Buff
import com.project.stalker_strike.COMMON_BUFFS
import com.project.stalker_strike.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.nio.charset.StandardCharsets

class BuffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val menuRadioButton: Button = itemView.findViewById(R.id.menuRadioButton)
    val buffText: TextView = itemView.findViewById(R.id.buffText)
    val buffBonus: TextView = itemView.findViewById(R.id.buffBonus)
}

class BuffAdapter(
    private var buffs: List<Buff>,
    private val buffUseClickListener: BuffUseClickListener,
    private val buffGiveClickListener: BuffGiveClickListener
) : RecyclerView.Adapter<BuffViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuffViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_buff, parent, false)
        return BuffViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BuffViewHolder, position: Int) {
        val buff = buffs[position]

        holder.buffText.text = buff.name

        if (buff.anomalyProtection == 0 && buff.radiationProtection == 0) {
            holder.buffBonus.text = "Жодних бонусів"
        } else {
            holder.buffBonus.text = "+${buff.radiationProtection}% захист від " +
                    "радіації, +${buff.anomalyProtection}% захист від аномалій"
        }

        if (buff.type == "medkit") {
            holder.buffBonus.text = "Відновлює ${buff.radiationProtection} здоров\'я"
        }
        if (buff.type == "antirad") {
            holder.buffBonus.text =
                "Надає захист від радіації на ${buff.radiationProtection} секунд"
        }
        if (buff.type == "antianomaly") {
            holder.buffBonus.text =
                "Надає захист від аномалій на ${buff.anomalyProtection} секунд"
        }

        if (buff.type == "medkit" || buff.type == "antirad" || buff.type == "antianomaly") {
            holder.menuRadioButton.visibility = View.VISIBLE
        } else {
            holder.menuRadioButton.visibility = View.GONE
        }

        holder.menuRadioButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.dropdown_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
                when (menuItem.itemId) {
                    R.id.useAction -> {
                        buffUseClickListener.onBuffUseClick(buff)
                        true
                    }

                    R.id.giveAction -> {
                        buffGiveClickListener.onBuffGiveClick(buff)
                        true
                    }

                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    override fun getItemCount(): Int {
        return buffs.size
    }

    fun updateBuffList() {
        buffs = BUFFS.toList()
        notifyDataSetChanged()
    }
}

class BuffsFragment : Fragment(), BuffUseClickListener, BuffGiveClickListener {

    private lateinit var adapter: BuffAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var navController: NavController
    private lateinit var qrCodeImageView: ImageView
    private lateinit var closeQRCodeButton: Button
    private lateinit var resetButton: Button

    var healPointsUpdater: HealPointsUpdater? = null
    var antiRadProtector: AntiRadProtector? = null
    var antiAnomalyProtector: AntiAnomalyProtector? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        navController =
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main)
        return inflater.inflate(R.layout.fragment_buffs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            loadBuffs()
        } catch (e: Throwable) {
            saveBuffs()
        }

        recyclerView = view.findViewById(R.id.recyclerViewBuffs)
        qrCodeImageView = view.findViewById(R.id.qrCodeImageView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = BuffAdapter(BUFFS.toList(), this, this)
        recyclerView.adapter = adapter

        resetButton = view.findViewById(R.id.resetButton)
        closeQRCodeButton = view.findViewById(R.id.closeButton)

        resetButton.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun saveBuffs() {
        context?.let { context ->
            val sharedPreferences =
                context.getSharedPreferences("buffs_prefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val json = Gson().toJson(BUFFS)

            editor.putString("buffs", json)
            editor.apply()
        }
    }

    private fun loadBuffs() {
        context?.let { context ->
            val sharedPreferences =
                context.getSharedPreferences("buffs_prefs", Context.MODE_PRIVATE)
            val json = sharedPreferences.getString("buffs", null)

            val type = object : TypeToken<MutableSet<Buff>>() {}.type
            val savedBuffs: MutableSet<Buff> = (Gson().fromJson(json, type) ?: mutableSetOf())
            val commonBuffs: MutableSet<Buff> = COMMON_BUFFS.toMutableSet()

            // First load
            if (savedBuffs.isEmpty()) {
                BUFFS = commonBuffs
                return
            }

            if (commonBuffs.containsAll(BUFFS) && BUFFS.containsAll(commonBuffs)) {
                BUFFS = savedBuffs
                return
            } else {
                // if new buffs incoming
                if (!(savedBuffs.containsAll(BUFFS) && BUFFS.containsAll(savedBuffs))) {
                    saveBuffs()
                    return
                }
            }

            // if no new buffs return saved
            if (!(commonBuffs.containsAll(savedBuffs) && savedBuffs.containsAll(commonBuffs))) {
                BUFFS = savedBuffs
            }
        }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Ця дія скине всі наявні речі")
            .setMessage("Впевнений?")
            .setPositiveButton("Так") { dialog, _ ->
                BUFFS = COMMON_BUFFS.toMutableSet()

                saveBuffs()

                adapter = BuffAdapter(BUFFS.toList(), this, this)
                recyclerView.adapter = adapter

                dialog.dismiss()
            }
            .setNegativeButton("Ні") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onBuffUseClick(buff: Buff) {
        if (buff.type == "medkit") {
            healPointsUpdater?.updateHealPoints(buff.radiationProtection)
        }
        if (buff.type == "antirad") {
            antiRadProtector?.protectFromRadiation(buff.radiationProtection)
        }
        if (buff.type == "antianomaly") {
            antiAnomalyProtector?.protectFromAnomaly(buff.anomalyProtection)
        }
        BUFFS.removeIf { it.id == buff.id }
        saveBuffs()
        showToast(requireContext(), "Ти використав \"${buff.name}\"!")
        navController.navigate(R.id.action_buffsFragment_to_detectorFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is HealPointsUpdater) {
            healPointsUpdater = context as HealPointsUpdater
        }
        if (context is AntiRadProtector) {
            antiRadProtector = context as AntiRadProtector
        }
        if (context is AntiRadProtector) {
            antiAnomalyProtector = context as AntiAnomalyProtector
        }
    }

    private fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }

    @SuppressLint("DetachAndAttachSameFragment")
    override fun onBuffGiveClick(buff: Buff) {
        val json = Gson().toJson(buff)

        try {
            val bitMatrix: BitMatrix = encodeAsBitmap(json)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
            qrCodeImageView.setImageBitmap(bitmap)
            qrCodeImageView.visibility = View.VISIBLE
            closeQRCodeButton.visibility = View.VISIBLE
            BUFFS.removeIf { it.id == buff.id }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(requireContext(), "Виникла проблема із генерацією QR-коду")
        }

        closeQRCodeButton.setOnClickListener {
            qrCodeImageView.visibility = View.GONE
            closeQRCodeButton.visibility = View.GONE
            adapter.updateBuffList()
        }
    }

    private fun encodeAsBitmap(contents: String): BitMatrix {
        val multiFormatWriter = MultiFormatWriter()
        return multiFormatWriter.encode(
            contents,
            BarcodeFormat.QR_CODE,
            250,
            250,
            hashMapOf(com.google.zxing.EncodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name())
        )
    }
}

