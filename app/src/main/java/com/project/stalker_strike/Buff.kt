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

var COMMON_BUFFS = setOf(
    Buff(
        "0",
        "Кулаки",
        "weapon",
        0,
        0,
    ),
    Buff(
        "1",
        "Куртка сталкера",
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
    ),
    Buff(
        "d3f332d8-68ab-45d8-99aa-1b3fc8fb1084",
        "Мала аптечка",
        "medkit",
        25,
        0,
    ),
    Buff(
        "d34546t8-68ab-5674-99aa-1b3fc8fb1084",
        "Мала аптечка",
        "medkit",
        25,
        0,
    ),
    Buff(
        "d367892d8-6ytb-45d8-99aa-1b3f675jhgj",
        "АнтиРад",
        "antirad",
        15,
        0,
    )
)

var BUFFS: MutableSet<Buff> = COMMON_BUFFS.toMutableSet()