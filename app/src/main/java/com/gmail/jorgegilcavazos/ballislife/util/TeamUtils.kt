package com.gmail.jorgegilcavazos.ballislife.util

import com.gmail.jorgegilcavazos.ballislife.R

class TeamUtils {
  companion object {
    fun getTeamLogo(abbreviation: String): Int = when(abbreviation) {
      "ATL" -> R.drawable.atl
      "BKN" -> R.drawable.bkn
      "BOS" -> R.drawable.bos
      "CHA" -> R.drawable.cha
      "CHI" -> R.drawable.chi
      "CLE" -> R.drawable.cle
      "DAL" -> R.drawable.dal
      "DEN" -> R.drawable.den
      "DET" -> R.drawable.det
      "GS", "GSW" -> R.drawable.gsw
      "HOU" -> R.drawable.hou
      "IND" -> R.drawable.ind
      "LAC" -> R.drawable.lac
      "LAL" -> R.drawable.lal
      "MEM" -> R.drawable.mem
      "MIA" -> R.drawable.mia
      "MIL" -> R.drawable.mil
      "MIN" -> R.drawable.min
      "NO", "NOP" -> R.drawable.nop
      "NY" -> R.drawable.nyk
      "OKC" -> R.drawable.okc
      "ORL" -> R.drawable.orl
      "PHI" -> R.drawable.phi
      "PHX" -> R.drawable.phx
      "POR" -> R.drawable.por
      "SAC" -> R.drawable.sac
      "SA", "SAS" -> R.drawable.sas
      "TOR" -> R.drawable.tor
      "UTAH", "UTA" -> R.drawable.uta
      "WSH", "WAS" -> R.drawable.was
      else -> R.drawable.rnbasnoo
    }

    fun getTeamId(favoriteTeam: String): String = when (favoriteTeam) {
      "atl" -> "1610612737"
      "bkn" -> "1610612751"
      "bos" -> "1610612738"
      "cha" -> "1610612766"
      "chi" -> "1610612741"
      "cle" -> "1610612739"
      "dal" -> "1610612742"
      "den" -> "1610612743"
      "det" -> "1610612765"
      "gsw" -> "1610612744"
      "hou" -> "1610612745"
      "ind" -> "1610612754"
      "lac" -> "1610612746"
      "lal" -> "1610612747"
      "mem" -> "1610612763"
      "mia" -> "1610612748"
      "mil" -> "1610612749"
      "min" -> "1610612750"
      "nop" -> "1610612740"
      "nyk" -> "1610612752"
      "okc" -> "1610612760"
      "orl" -> "1610612753"
      "phi" -> "1610612755"
      "phx" -> "1610612756"
      "por" -> "1610612757"
      "sac" -> "1610612758"
      "sas" -> "1610612759"
      "tor" -> "1610612761"
      "uta" -> "1610612762"
      "was" -> "1610612764"
      else -> throw IllegalArgumentException("Invalid favoriteTeam: " + favoriteTeam)
    }
  }
}