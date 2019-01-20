package com.gmail.jorgegilcavazos.ballislife.features.model

enum class GameStatus(val code: String) {
  PRE("1"), LIVE("2"), POST("3");

  companion object {
    private val map = values().associateBy { it.code }

    fun fromCode(code: String): GameStatus = map[code]!!
  }
}