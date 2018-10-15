package com.gmail.jorgegilcavazos.ballislife.util

import com.crashlytics.android.Crashlytics
import javax.inject.Inject

class CrashReporterImpl @Inject constructor() : CrashReporter {

  override fun log(message: String) {
    Crashlytics.log(message)
  }

  override fun logcat(level: Int, tag: String, message: String) {
    Crashlytics.log(level, tag, message)
  }

  override fun report(e: Throwable) {
    Crashlytics.logException(e)
  }
}