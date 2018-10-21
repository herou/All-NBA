package com.gmail.jorgegilcavazos.ballislife.common

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RxJava wrapper of the Play Billing Library.
 */
@Singleton
open class RxPlayBilling @Inject constructor(val context: Context) : PurchasesUpdatedListener {

  private var billingClient: BillingClient? = null

  private val purchaseUpdates = PublishRelay.create<PurchaseUpdate>()

  override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {
    purchaseUpdates.accept(PurchaseUpdate(responseCode, purchases))
  }

  /**
   * Initialized the Billing Client. Must be called before interacting with the API.
   */
  open fun initialize() {
    if (billingClient == null) {
      billingClient = BillingClient.newBuilder(context).setListener(this).build()
    }
  }

  open fun isReady(): Boolean {
    return billingClient!!.isReady
  }

  /**
   * Establishes a connection with Play Services. A connection must be established before
   * attempting to make purchases, consumptions or querying items.
   */
  open fun startConnection(): Completable {
    if (billingClient!!.isReady) {
      return Completable.complete()
    }

    return Completable.create { emitter ->
      billingClient!!.startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(responseCode: Int) {
          if (responseCode == BillingClient.BillingResponse.OK) {
            Timber.i("Billing connection successfully established")
            emitter.onComplete()
          } else if (responseCode == BillingClient.BillingResponse.SERVICE_DISCONNECTED) {
            if (!emitter.isDisposed) {
              emitter.onError(
                  ServiceDisconnectedException("Billing connection failed w/code: $responseCode"))
            }
          } else {
            emitter.onError(Exception("Billing connection failed with code: $responseCode"))
          }
        }

        override fun onBillingServiceDisconnected() {
          Timber.e("Billing connection failed / disconnected")
        }
      })
    }
  }

  /**
   * Finishes the connection with Play Services and releases any allocated resources.
   */
  open fun endConnection() {
    billingClient!!.endConnection()
  }

  /**
   * Notifies subscribers of new [PurchaseUpdate]s received by the client.
   */
  open fun purchaseUpdates(): Observable<PurchaseUpdate> = purchaseUpdates

  /**
   * Launches a billing flow for an INAPP product given its skuId.
   */
  open fun purchaseItem(skuId: String, activity: Activity): Single<Int> {
    val flowParams = BillingFlowParams.newBuilder()
        .setSku(skuId)
        .setType(BillingClient.SkuType.INAPP)
        .build()

    return launchBillingFlow(flowParams, activity)
  }

  /**
   * Launches a billing flow for a SUBS product given its skuId.
   */
  open fun purchaseSubscription(skuId: String, activity: Activity): Single<Int> {
    val flowParams = BillingFlowParams.newBuilder()
        .setSku(skuId)
        .setType(BillingClient.SkuType.SUBS)
        .build()

    return launchBillingFlow(flowParams, activity)
  }

  /**
   * Consumes a purchase to allow re-purchasing it.
   */
  open fun consumePurchase(purchaseToken: String): Single<Int> {
    return Single.create { emitter ->
      billingClient!!.consumeAsync(purchaseToken, { responseCode, _ ->
        emitter.onSuccess(responseCode)
      })
    }
  }

  /**
   * Fetches the SKU details of a list skuIds of a given type (INAPP or SUBS).
   */
  open fun skuDetails(skuIds: List<String>, skuType: String): Single<List<SkuDetails>> {
    val skuDetailsParams = SkuDetailsParams
        .newBuilder()
        .setSkusList(skuIds)
        .setType(skuType)
        .build()

    return Single.create { emitter ->
      billingClient!!.querySkuDetailsAsync(skuDetailsParams, { _, details ->
        emitter.onSuccess(details)
      })
    }
  }

  /**
   * Fetches the cached purchases that the user owns.
   */
  open fun queryPurchases(skuType: String): Single<List<Purchase>> {
    val result = billingClient!!.queryPurchases(skuType)
    return if (result.responseCode == BillingClient.BillingResponse.OK) {
      Single.just(result.purchasesList)
    } else {
      Single.error(Exception("Non-success code: " + result.responseCode))
    }
  }

  private fun launchBillingFlow(flowParams: BillingFlowParams, activity: Activity): Single<Int> {
    return Single.just(billingClient!!.launchBillingFlow(activity, flowParams))
  }
}
