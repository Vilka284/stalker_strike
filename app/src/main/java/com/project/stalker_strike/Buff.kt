package com.project.stalker_strike

data class Buff(
    var id: String,
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

        return (id == other.id)
    }
}

var COMMON_BUFFS = listOf(
    Buff(
        "0",
        "Кулаки",
        "weapon",
        0,
        0,
    ),
    Buff(
        "1",
        "Куртка",
        "clothes",
        10,
        0,
    ),
    Buff(
        "2",
        "Потерта шапка",
        "headwear",
        0,
        0,
    )
)

var BUFFS: MutableList<Buff> = COMMON_BUFFS.toMutableList()