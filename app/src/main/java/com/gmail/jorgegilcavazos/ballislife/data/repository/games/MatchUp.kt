package com.gmail.jorgegilcavazos.ballislife.data.repository.games

data class MatchUp(
    val team1: String = "",
    val team2: String = "",
    val team1_seed: Int = 0,
    val team2_seed: Int = 0,
    val team1_wins: Int = 0,
    val team2_wins: Int = 0,
    val conference: String = "",
    val round: Int = 1
)