package com.kotlin.weatherapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.kotlin.weatherapp.Constants
import com.kotlin.weatherapp.R
import com.kotlin.weatherapp.models.WeatherResponse
import com.kotlin.weatherapp.network.WeatherService
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

  // Get location of long, lat.
  private lateinit var mFusedLocationClient: FusedLocationProviderClient

  private var mProgressDialog: Dialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    // Check for permission.
    if(!isLocationEnable()){
      Toast.makeText(
        this,
        "Your location provider is turned off. Please turn it on",
        Toast.LENGTH_SHORT
      ).show()
      // Redirect User to Specific Settings Menu.
      val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
      startActivity(intent)
    }else{
      // Permission With Dexter.
      Dexter.withActivity(this)
        .withPermissions(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        .withListener(object: MultiplePermissionsListener{
          // ---------------- ------------------------//
          @RequiresApi(Build.VERSION_CODES.S)
          override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
            // Check if all permission are allowed.
            if(report!!.areAllPermissionsGranted()){
              requestLocationData()
            }
            // If any of both permissions are Denied.
            if(report.isAnyPermissionPermanentlyDenied){
              Toast.makeText(
                this@MainActivity,
                "You have denied location permission",
                Toast.LENGTH_SHORT
                ).show()
            }
          }
          // ------------------- -------------------------//
          // Show functionality for Dialog permissions.
          override fun onPermissionRationaleShouldBeShown(
            permissions: MutableList<PermissionRequest>?,
            token: PermissionToken?
          ) {
            showRationalDialogForPermissions()
          }
        }).onSameThread()
        .check()
    }
  }



  @RequiresApi(Build.VERSION_CODES.S)
  @SuppressLint("MissingPermission")
  private fun requestLocationData(){
    val mLocationRequest = com.google.android.gms.location.LocationRequest()
    mLocationRequest.priority = LocationRequest.QUALITY_HIGH_ACCURACY

    mFusedLocationClient.requestLocationUpdates(
      mLocationRequest, mLocationCallback,
      Looper.myLooper()
    )
  }

  private val mLocationCallback = object: LocationCallback(){
    // Get Users location.
    override fun onLocationResult(locationResult: LocationResult){
      val mLastLocation: Location = locationResult.lastLocation
      // Take: Latitude and Longitude
      val latitude = mLastLocation.latitude
      Log.i("Current Latitude", "$latitude")
      val longitude = mLastLocation.longitude
      Log.i("Current Longitude", "$longitude")

      getLocationWeatherDetails(latitude, longitude)
    }
  }

  private fun getLocationWeatherDetails(latitude: Double, longitude: Double){
    if(Constants.isNetworkAvailable(this)){
     // Internet connection successful.
      val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

      val service: WeatherService = retrofit
        .create(WeatherService::class.java)

      val listCall: Call<WeatherResponse> = service.getWeather(
        latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
      )

      showCustomProgressDialog()

      listCall.enqueue(object: Callback<WeatherResponse>{
        override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
          if (response.isSuccessful) {
            hideProgressDialog()
            val weatherList: WeatherResponse? = response.body()
            Log.i("Response Result", "$weatherList")
          } else {
            when (response.code()) {
              400 -> {
                Log.e("Error 400", "Bad Connection")
              }
              404 -> {
                Log.e("Error 404", "Not found")
              }
              else -> {
                Log.e("Error", "Generic Error")
              }
            }
          }
        }
          override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
            Log.e("Error", t.message.toString())
            hideProgressDialog()
          }
      })

    }else{
      Toast.makeText(
        this@MainActivity,
        "No Internet",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  private fun showRationalDialogForPermissions(){
    AlertDialog.Builder(this)
      .setMessage("It Looks like you have turned off permissions")
      .setPositiveButton("Go to settings"){_, _ ->
        try{
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          val uri = Uri.fromParts("package", packageName, null)
          intent.data = uri
          startActivity(intent)
        }catch (e: ActivityNotFoundException){
          e.printStackTrace()
        }
      }
      .setNegativeButton("Cancel"){
        dialog, _ -> dialog.dismiss()
      }.show()
  }

  private fun isLocationEnable(): Boolean{
    //This provides access to the system location services.
    val locationManager: LocationManager =
      getSystemService(Context.LOCATION_SERVICE) as LocationManager

    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
  }

  // -------------------- DIALOG ------------------ //
  private fun showCustomProgressDialog(){
    mProgressDialog = Dialog(this)
    mProgressDialog!!.setContentView(R.layout.custom_dialog_progress)
    mProgressDialog!!.show()
  }

  private fun hideProgressDialog(){
    if(mProgressDialog != null){
      mProgressDialog!!.dismiss()
    }
  }
}