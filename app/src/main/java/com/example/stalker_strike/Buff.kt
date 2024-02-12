package com.example.stalkerstrike

data class Buff(
    val name: String,
    val type: String,
    val radiationProtection: Int,
    val anomalyProtection: Int,
    val HPBonus: Int
)

val BUFFS: Set<Buff> = setOf(
    Buff(
        "Куртка сталкера",
        "clothes",
        10,
        0,
        0
    )
)