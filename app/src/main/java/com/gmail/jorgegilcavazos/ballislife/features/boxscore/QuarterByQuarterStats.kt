package com.gmail.jorgegilcavazos.ballislife.features.boxscore

data class QuarterByQuarterStats(
    val homeTeamQuarterStats: TeamQuarterStats,
    val visitorTeamQuarterStats: TeamQuarterStats,
    val numPeriods: Int
)

data class TeamQuarterStats(
    val q1: Int,
    val q2: Int,
    val q3: Int,
    val q4: Int,
    val ot1: Int,
    val ot2: Int,
    val ot3: Int,
    val ot4: Int,
    val total: Int
)