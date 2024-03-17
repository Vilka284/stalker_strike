package com.example.stalker_strike.ui.buffs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stalker_strike.BUFFS
import com.example.stalker_strike.Buff
import com.example.stalker_strike.COMMON_BUFFS
import com.example.stalker_strike.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BuffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val buffText: TextView = itemView.findViewById(R.id.buffText)
    val buffBonus: TextView = itemView.findViewById(R.id.buffBonus)
}

class BuffAdapter(private val buffs: List<Buff>) : RecyclerView.Adapter<BuffViewHolder>() {

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
    }

    override fun getItemCount(): Int {
        return buffs.size
    }
}

class BuffsFragment : Fragment() {

    private lateinit var adapter: BuffAdapter
    private lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        adapter = BuffAdapter(BUFFS.toList())
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

                adapter = BuffAdapter(BUFFS.toList())
                recyclerView.adapter = adapter

                dialog.dismiss()
            }
            .setNegativeButton("Ні") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

