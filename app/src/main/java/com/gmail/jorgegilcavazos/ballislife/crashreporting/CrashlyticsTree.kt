package com.gmail.jorgegilcavazos.ballislife.crashreporting

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber.Tree

class CrashlyticsTree : Tree() {
  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    when(priority) {
      Log.ERROR -> {
        val error = t ?: Exception(message)
        Crashlytics.log(priority, tag, message)
        Crashlytics.logException(error)
      }
      else -> Crashlytics.log(priority, tag, message)
    }
  }
}