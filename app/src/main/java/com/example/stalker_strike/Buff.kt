package com.example.stalker_strike

data class Buff(
    val name: String,
    val type: String,
    val radiationProtection: Int,
    val anomalyProtection: Int
) {
    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Buff

        return type == other.type
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