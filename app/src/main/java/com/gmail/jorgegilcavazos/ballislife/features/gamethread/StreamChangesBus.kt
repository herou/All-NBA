package com.gmail.jorgegilcavazos.ballislife.features.gamethread

import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamChangesBus @Inject constructor() {
  private val changes = PublishRelay.create<StreamMode>()

  fun notifyChanged(mode: StreamMode) {
    changes.accept(mode)
  }

  fun getChanges(): Observable<StreamMode> = changes

  enum class StreamMode {
    ON, OFF
  }
}
