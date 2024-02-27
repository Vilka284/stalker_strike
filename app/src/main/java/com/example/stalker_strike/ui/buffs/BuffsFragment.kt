package com.example.stalker_strike.ui.buffs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stalker_strike.R
import com.example.stalker_strike.BUFFS
import com.example.stalker_strike.Buff

class BuffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val buffText: TextView = itemView.findViewById(R.id.buffText)
    val buffBonus: TextView = itemView.findViewById(R.id.buffBonus)
    val buffImage: ImageView = itemView.findViewById(R.id.buffImage)
}

class BuffAdapter(private val buffs: List<Buff>) : RecyclerView.Adapter<BuffViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuffViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_buff, parent, false)
        return BuffViewHolder(itemView)
    }

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_buffs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewBuffs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = BuffAdapter(BUFFS.toList())
        recyclerView.adapter = adapter
    }
}

