package com.gmail.jorgegilcavazos.ballislife.features.model

import java.lang.IllegalArgumentException

enum class Team(val fullName: String, val key: String, val nickName: String) {
  ATL("Atlanta Hawks", "ATL", "Hawks"),
  BKN("Brooklyn Nets", "BKN", "Nets"),
  BOS("Boston Celtics", "BOS", "Celtics"),
  CHA("Charlotte Hornets", "CHA", "Hornets"),
  CHI("Chicago Bulls", "CHI", "Bulls"),
  CLE("Cleveland Cavaliers", "CLE", "Cavaliers"),
  DAL("Dallas Mavericks", "DAL", "Mavericks"),
  DEN("Denver Nuggets", "DEN", "Nuggets"),
  DET("Detroit Pistons", "DET", "Pistons"),
  GSW("Golden State Warriors", "GSW", "Warriors"),
  HOU("Houston Rockets", "HOU", "Rockets"),
  IND("Indiana Pacers", "IND", "Pacers"),
  LAC("Los Angeles Clippers", "LAC", "Clippers"),
  LAL("Los Angeles Lakers", "LAL", "Lakers"),
  MEM("Memphis Grizzlies", "MEM", "Grizzlies"),
  MIA("Miami Heat", "MIA", "Heat"),
  MIL("Milwaukee Bucks", "MIL", "Bucks"),
  MIN("Minnesota Timberwolves", "MIN", "Timberwolves"),
  NOP("New Orleans Pelicans", "NOP", "Pelicans"),
  NYK("New York Knicks", "NYK", "Knicks"),
  OKC("Oklahoma City Thunder", "OKC", "Thunder"),
  ORL("Orlando Magic", "ORL", "Magic"),
  PHI("Philadelphia 76ers", "PHI", "76ers"),
  PHX("Phoenix Suns", "PHX", "Suns"),
  POR("Portland Trail Blazers", "POR", "Blazers"),
  SAC("Sacramento Kings", "SAC", "Kings"),
  SAS("San Antonio Spurs", "SAS", "Spurs"),
  TOR("Toronto Raptors", "TOR", "Raptors"),
  UTA("Utah Jazz", "UTA", "Jazz"),
  WAS("Washington Wizards", "WAS", "Wizards");

  companion object {
    private val map = values().associateBy { it.key }
    fun fromKey(key: String) = map[key] ?: throw IllegalArgumentException("Invalid team key $key")
  }
}