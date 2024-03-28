package com.project.stalker_strike.ui.buffs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
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

class BuffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val buffText: TextView = itemView.findViewById(R.id.buffText)
    val buffBonus: TextView = itemView.findViewById(R.id.buffBonus)
    val useButton: Button = itemView.findViewById(R.id.useButton)
}

class BuffAdapter(
    private val buffs: List<Buff>,
    private val buffUseClickListener: BuffUseClickListener
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

        if (buff.type == "medkit" || buff.type == "antirad") {
            holder.useButton.visibility = View.VISIBLE
        } else {
            holder.useButton.visibility = View.GONE
        }

        holder.useButton.setOnClickListener {
            buffUseClickListener.onBuffUseClick(buff)
        }
    }

    override fun getItemCount(): Int {
        return buffs.size
    }
}

class BuffsFragment : Fragment(), BuffUseClickListener {

    private lateinit var adapter: BuffAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var navController: NavController

    var healPointsUpdater: HealPointsUpdater? = null
    var antiRadProtector: AntiRadProtector? = null
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
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = BuffAdapter(BUFFS.toList(), this)
        recyclerView.adapter = adapter

        val resetButton = view.findViewById<Button>(R.id.resetButton)

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

                adapter = BuffAdapter(BUFFS.toList(), this)
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
    }

    private fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }
}

