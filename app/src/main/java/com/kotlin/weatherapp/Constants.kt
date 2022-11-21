package com.kotlin.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants{

  const val APP_ID: String = "8abd6b679b68890e1ecacd5d29a3d617"
  const val BASE_URL: String = "https://api.openweathermap.org/data/"
  const val METRIC_UNIT: String = "metric"

  // Check if there is a network connection available.
  fun isNetworkAvailable(context: Context): Boolean{
    val connectivityManager = context.getSystemService(
      Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // Checking android Version
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
      // Newer Versions.
      val network = connectivityManager.activeNetwork?: return false
      val activeNetwork = connectivityManager.getNetworkCapabilities(network)?: return false
      return when{
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
      }
    }else{
      // Older versions.
      val networkInfo = connectivityManager.activeNetworkInfo
      return networkInfo !=null && networkInfo.isConnectedOrConnecting
    }
  }
}
