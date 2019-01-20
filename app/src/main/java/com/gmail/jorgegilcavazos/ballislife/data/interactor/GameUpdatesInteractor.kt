package com.gmail.jorgegilcavazos.ballislife.data.interactor

import android.annotation.SuppressLint
import com.gmail.jorgegilcavazos.ballislife.common.ErrorType
import com.gmail.jorgegilcavazos.ballislife.data.service.NbaGamesService
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject

class GameUpdatesInteractor @Inject constructor(
    private val gamesService: NbaGamesService,
    private val schedulerProvider: BaseSchedulerProvider
) {
  private val _gameUpdates = PublishRelay.create<Result>()

  val updates: Observable<Result>
    get() = _gameUpdates

  @SuppressLint("CheckResult")
  fun refresh(gameId: String) {
    gamesService.getGame(gameId)
        .toObservable()
        .map { game -> Result.Update(game) as Result }
        .doOnError { e -> logErrorIfUnknown(e, gameId) }
        .onErrorReturn { e -> Result.Failure(ErrorType.fromThrowable(e)) }
        .subscribeOn(schedulerProvider.io())
        .observeOn(schedulerProvider.ui())
        .subscribe(_gameUpdates)
  }

  private fun logErrorIfUnknown(t: Throwable, gameId: String) {
    if (ErrorType.fromThrowable(t) is ErrorType.Unknown) {
      Timber.e(t, "An unknown error ocurred while refreshing game: $gameId")
    }
  }

  sealed class Result {
    data class Update(val game: GameV2) : Result()
    data class Failure(val e: ErrorType) : Result()
  }
}