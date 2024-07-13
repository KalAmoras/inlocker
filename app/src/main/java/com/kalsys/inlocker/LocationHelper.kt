package com.kalsys.inlocker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationHelper(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationDao = PasswordDatabase.getInstance(context).locationDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Location?) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("LocationHelper", "Last location is not null: $location")
                    saveLocationToDatabase(location)
                    callback(location)
                    Log.d("LocationHelper", "latitude: ${location.latitude}  longitude:  ${location.longitude}")
                } else {
                    Log.d("LocationHelper", "Last location is null, requesting new location")
                    requestNewLocation(callback)
                }
            }.addOnFailureListener { e ->
                Log.e("LocationHelper", "Error getting last location: ${e.message}")
                requestNewLocation(callback)
            }
        } catch (e: SecurityException) {
            Log.e("LocationHelper", "Security exception: ${e.message}")
            callback(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation(callback: (Location?) -> Unit) {
        Log.d("LocationHelper", "requestNewLocation called")

        try {
            val locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        saveLocationToDatabase(location)
                        callback(location)
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    super.onLocationAvailability(locationAvailability)
                    if (!locationAvailability.isLocationAvailable) {
                        Log.d("LocationHelper", "Location services are not available.")
                        callback(null)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("LocationHelper", "Security exception: ${e.message}")
            callback(null)
        }
    }

    private fun saveLocationToDatabase(location: Location) {
        scope.launch {
            withContext(Dispatchers.IO) {
                locationDao.insertLocation(LocationEntity(latitude = location.latitude, longitude = location.longitude))
            }
        }
    }
}
