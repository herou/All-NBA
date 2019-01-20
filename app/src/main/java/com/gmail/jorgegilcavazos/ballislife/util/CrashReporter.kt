package com.gmail.jorgegilcavazos.ballislife.util

interface CrashReporter {
  fun log(message: String)

  fun logcat(level: Int, tag: String, message: String)

  fun report(e: Throwable)
}