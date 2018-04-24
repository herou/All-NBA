package com.gmail.jorgegilcavazos.ballislife.data.repository.highlights

import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository
import com.gmail.jorgegilcavazos.ballislife.features.model.Highlight
import com.gmail.jorgegilcavazos.ballislife.features.model.toMapWithNewDate
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val localRepository: LocalRepository,
    private val firestore: FirebaseFirestore
) : FavoritesRepository {

  companion object {
    val USERS_COLLECTION = "users"
    val FAV_HIGHLIGHTS_COLLECTION = "fav-highlights"
    val HIGHLIGHT_FAV_TIME_ATTR_KEY = "fav_time"
  }

  private val newlyAddedFavorites = PublishRelay.create<Highlight>()
  private val loadMoreTrigger = PublishRelay.create<Long>()
  private var lastLoadedDoc: DocumentSnapshot? = null

  override fun newlyAddedFavorites(): Observable<Highlight> = newlyAddedFavorites

  override fun favorites(): Observable<Highlight> {
    lastLoadedDoc = null
    return Observable.create { emitter ->
      loadMoreTrigger.subscribe { limit ->
        var query = firestore.collection(USERS_COLLECTION)
            .document(localRepository.username)
            .collection(FAV_HIGHLIGHTS_COLLECTION)
            .orderBy(HIGHLIGHT_FAV_TIME_ATTR_KEY, Query.Direction.DESCENDING)
            .limit(limit)

        query = lastLoadedDoc?.let { query.startAfter(lastLoadedDoc!!) } ?: query

        query.get()
            .addOnSuccessListener { querySnapshot ->
              if (!querySnapshot.isEmpty) {
                lastLoadedDoc = querySnapshot.last()
                querySnapshot.forEach { document ->
                  emitter.onNext(document.toObject(Highlight::class.java))
                }
              }
            }
            .addOnFailureListener { error ->
              emitter.onError(error)
            }
      }
    }
  }

  override fun loadMore() {
    loadMoreTrigger.accept(20)
  }

  override fun saveToFavorites(highlight: Highlight): Completable {
    return Completable.create { emitter ->
      firestore.collection(USERS_COLLECTION)
          .document(localRepository.username)
          .collection(FAV_HIGHLIGHTS_COLLECTION)
          .document(highlight.id)
          .set(highlight.toMapWithNewDate())
          .addOnCompleteListener {
            emitter.onComplete()
            newlyAddedFavorites.accept(highlight)
          }
          .addOnFailureListener { emitter.onError(it) }
    }
  }

  override fun removeFromFavorites(highlight: Highlight): Completable {
    return Completable.create { emitter ->
      firestore.collection(USERS_COLLECTION)
          .document(localRepository.username)
          .collection(FAV_HIGHLIGHTS_COLLECTION)
          .document(highlight.id)
          .delete()
          .addOnCompleteListener { emitter.onComplete() }
          .addOnFailureListener { emitter.onError(it) }
    }
  }


}