package com.gmail.jorgegilcavazos.ballislife.features.model

import com.google.gson.annotations.SerializedName

data class BoxScoreTeam(
		val pstsg: List<StatLine>,
		@SerializedName("s") val score: Int,
		val q1: Int,
		val q2: Int,
		val q3: Int,
		val q4: Int,
		val ot1: Int,
		val ot2: Int,
		val ot3: Int,
		val ot4: Int
)
