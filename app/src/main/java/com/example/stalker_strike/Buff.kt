package com.example.stalkerstrike

data class Buff(
    val name: String,
    val type: String,
    val radiationProtection: Int,
    val anomalyProtection: Int
) {
    override fun hashCode(): Int {
        return type.hashCode()
    }
}

val BUFFS: Set<Buff> = setOf(
    Buff(
        "Куртка сталкера",
        "clothes",
        10,
        0,
    ),
    Buff(
        "Потерта шапка",
        "headwear",
        0,
        0,
    )
)