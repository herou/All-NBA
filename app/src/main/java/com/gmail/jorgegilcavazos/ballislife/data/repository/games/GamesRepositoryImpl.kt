package com.gmail.jorgegilcavazos.ballislife.data.repository.games

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import com.gmail.jorgegilcavazos.ballislife.data.actions.games.GamesResult
import com.gmail.jorgegilcavazos.ballislife.data.actions.games.GamesResult.LoadGamesResult
import com.gmail.jorgegilcavazos.ballislife.data.service.NbaGamesService
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesUiModel
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider
import com.google.firebase.firestore.FirebaseFirestore
import de.aaronoe.rxfirestore.getSingle
import io.reactivex.Observable
import io.reactivex.Single
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the [GamesRepository] interface.
 */
@Singleton
class GamesRepositoryImpl @Inject constructor(
    private val gamesService: NbaGamesService,
    private val firestore: FirebaseFirestore,
    private val schedulerProvider: BaseSchedulerProvider
) : GamesRepository {

  private val gamesMap = ConcurrentHashMap<String, GameV2>()

  override fun games(date: Calendar, forceNetwork: Boolean): Observable<GamesUiModel> {
    val network = networkSource(date).toObservable()
        .concatMap {
          if (it.isEmpty()) {
            Observable.just(GamesUiModel.networkSuccess(emptyList()))
          } else {
            Observable.just(GamesUiModel.networkSuccess(sortGamesByUtcThenId(it.values)))
          }
        }
        .subscribeOn(schedulerProvider.io())
        .observeOn(schedulerProvider.ui())
        .startWith(GamesUiModel.networkInProgress())

    val memory = memorySource(date).toObservable()
        .concatMap {
          if (it.isEmpty()) {
            Observable.just(GamesUiModel.memorySuccess(emptyList()))
          } else {
            Observable.just(GamesUiModel.memorySuccess(sortGamesByUtcThenId(it.values)))
          }
        }
        .subscribeOn(schedulerProvider.io())
        .observeOn(schedulerProvider.ui())
        .startWith(GamesUiModel.memoryInProgress())

    if (forceNetwork) {
      return network
    }

    return Observable.concat(memory, network)
  }

  override fun loadGames(
      date: Calendar,
      forceNetwork: Boolean): Observable<GamesResult.LoadGamesResult> {
    val network = networkSource(date).toObservable()
        .concatMap {
          if (it.isEmpty()) {
            Observable.just(LoadGamesResult.NoGames)
          } else {
            Observable.just(
                LoadGamesResult.Success(sortGamesByUtcThenId(it.values), date)
            )
          }
        }
        .onErrorReturn { LoadGamesResult.Failure(it) }
        .subscribeOn(schedulerProvider.io())
        .observeOn(schedulerProvider.ui())
        .startWith(LoadGamesResult.NetworkInProgress)

    val memory = memorySource(date).toObservable()
        .concatMap {
          if (it.isNotEmpty()) {
            Observable.just(
                LoadGamesResult.Success(sortGamesByUtcThenId(it.values), date)
            )
          } else {
            Observable.just(LoadGamesResult.NoCachedGames)
          }
        }
        .onErrorReturn { LoadGamesResult.Failure(it) }
        .subscribeOn(schedulerProvider.io())
        .observeOn(schedulerProvider.ui())
        .startWith(LoadGamesResult.MemoryInProgress)

    if (forceNetwork) {
      return network
    }

    return Observable.concat(memory, network)

    /*
    return if (!DateFormatUtil.isDateToday(date.time)) {
      // If the date isn't today there's no need to refresh from the network, since the cached
      // data won't need to be updated.
      // We return from the memory source and only if there was no cached games we start a
      // network request.
      memory.flatMap { result ->
        if (result == LoadGamesResult.NoCachedGames) {
          // Memory had no cached games, return the network results.
          network
        } else {
          // Return whatever the result was.
          Observable.just(result)
        }
      }
    } else {
      // If the date is today we want to refresh game data always. We return the cached data
      // first while we update the network, when the network data arrives we deliver those results.
      Observable.concat(memory, network)
    }
    */
  }

  private fun networkSource(date: Calendar): Single<Map<String, GameV2>> {
    return gamesService
        .getDayGames(
            "\"timeUtc\"",
            DateFormatUtil.getDateStartUtc(date),
            DateFormatUtil.getDateEndUtc(date))
        .map { it.filterNot { it.value.periodStatus == "TBD" } }
        .flatMap { map ->
          val matchUpsRef = firestore.collection("playoff_picture").document("2018").collection("1")
          matchUpsRef.getSingle<MatchUp>()
              .observeOn(schedulerProvider.ui())
              .map { matchUps ->
                Pair<Map<String, GameV2>, List<MatchUp>>(map, matchUps)
              }
        }
        .flatMap { (map, matchUps) ->
          for ((_, game) in map) {
            val team1 = game.homeTeamAbbr
            val team2 = game.awayTeamAbbr

            matchUps.firstOrNull { matchUp ->
              (matchUp.team1 == team1 || matchUp.team1 == team2)
                  && (matchUp.team2 == team1 || matchUp.team2 == team2)
            }?.let { matchUp ->
              game.seriesSummary = when {
                matchUp.team1_wins == 4 -> {
                  "${matchUp.team1} wins ${matchUp.team1_wins}-${matchUp.team2_wins}"
                }
                matchUp.team2_wins == 4 -> {
                  "${matchUp.team2} wins ${matchUp.team2_wins}-${matchUp.team1_wins}"
                }
                matchUp.team1_wins == matchUp.team2_wins -> {
                  "Series tied ${matchUp.team1_wins}-${matchUp.team2_wins}"
                }
                matchUp.team1_wins > matchUp.team2_wins -> {
                  "${matchUp.team1} leads ${matchUp.team1_wins}-${matchUp.team2_wins}"
                }
                else -> {
                  "${matchUp.team2} leads ${matchUp.team2_wins}-${matchUp.team1_wins}"
                }
              }
            }
          }
          Single.just(map)
        }
        .doOnSuccess { gamesMap.putAll(it) }

  }

  private fun sortGamesByUtcThenId(games: Collection<GameV2>): List<GameV2> {
    return games.sortedWith(Comparator { game1: GameV2, game2: GameV2 ->
      when {
        game1.timeUtc > game2.timeUtc -> 1
        game1.timeUtc < game2.timeUtc -> -1
        game1.timeUtc == game1.timeUtc -> {
          game1.id.toInt() - game2.id.toInt()
        }
        else -> throw IllegalStateException("Comparator reached invalid state " +
            "where game1.utc is " + game1.timeUtc
            + "and game2.utc is " + game2.timeUtc)
      }
    })
  }

  @SuppressLint("VisibleForTests")
  private fun memorySource(date: Calendar): Single<Map<String, GameV2>> {
    return Single.just(gamesMap
        .filterValues {
          it.timeUtc >= DateFormatUtil.getDateStartUtc(date)
              && it.timeUtc < DateFormatUtil.getDateEndUtc(date)
        })
        .doOnSuccess { saveGamesInCache(it) }
  }

  @VisibleForTesting
  fun saveGamesInCache(gamesMap: Map<String, GameV2>, clear: Boolean = false) {
    if (clear) {
      this.gamesMap.clear()
    }
    this.gamesMap.putAll(gamesMap)
  }
}
