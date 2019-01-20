package com.gmail.jorgegilcavazos.ballislife.common

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class ErrorType {
  object Network : ErrorType()
  data class Unknown(val t: Throwable) : ErrorType()

  companion object {
    fun fromThrowable(t: Throwable): ErrorType {
      return when (t) {
        is SocketTimeoutException,
        is UnknownHostException,
        is SocketException -> Network
        else -> Unknown(t)
      }
    }
  }
}