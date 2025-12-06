package com.fairair.app.util

import org.khronos.webgl.Int32Array
import kotlin.js.Promise
import kotlinx.coroutines.await

/**
 * Location coordinates.
 */
data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double
)

/**
 * Result of a location request.
 */
sealed class LocationResult {
    data class Success(val coordinates: LocationCoordinates) : LocationResult()
    data class Error(val message: String) : LocationResult()
    data object PermissionDenied : LocationResult()
}

/**
 * Airport with its coordinates for proximity matching.
 */
data class AirportLocation(
    val code: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Known airport locations for proximity matching.
 */
private val airportLocations = listOf(
    AirportLocation("JED", "Jeddah", 21.6796, 39.1565),
    AirportLocation("RUH", "Riyadh", 24.9577, 46.6989),
    AirportLocation("DMM", "Dammam", 26.4712, 49.7979),
    AirportLocation("AHB", "Abha", 18.2404, 42.6567),
    AirportLocation("GIZ", "Jazan", 16.9011, 42.5858),
    AirportLocation("TUU", "Tabuk", 28.3654, 36.6189),
    AirportLocation("DXB", "Dubai", 25.2532, 55.3657),
    AirportLocation("CAI", "Cairo", 30.1219, 31.4056)
)

/**
 * JS function to check if geolocation is available.
 */
@JsFun("() => 'geolocation' in navigator")
private external fun jsHasGeolocation(): Boolean

/**
 * JS function result type for location.
 * We use a simple array: [success (0/1/2), latitude, longitude]
 * success: 0 = success, 1 = permission denied, 2 = error
 */
external interface JsLocationResult : JsAny

@JsFun("""
() => new Promise((resolve) => {
    if (!('geolocation' in navigator)) {
        resolve({ success: false, error: 'not_supported' });
        return;
    }
    navigator.geolocation.getCurrentPosition(
        (position) => {
            resolve({
                success: true,
                latitude: position.coords.latitude,
                longitude: position.coords.longitude
            });
        },
        (error) => {
            if (error.code === 1) {
                resolve({ success: false, error: 'permission_denied' });
            } else if (error.code === 2) {
                resolve({ success: false, error: 'unavailable' });
            } else {
                resolve({ success: false, error: 'timeout' });
            }
        },
        { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
    );
})
""")
private external fun jsGetCurrentPosition(): Promise<JsLocationResult>

/**
 * Get success status from location result.
 */
@JsFun("(result) => result.success")
private external fun jsLocationSuccess(result: JsLocationResult): Boolean

/**
 * Get latitude from location result.
 */
@JsFun("(result) => result.latitude")
private external fun jsLocationLatitude(result: JsLocationResult): Double

/**
 * Get longitude from location result.
 */
@JsFun("(result) => result.longitude")
private external fun jsLocationLongitude(result: JsLocationResult): Double

/**
 * Get error from location result.
 */
@JsFun("(result) => result.error")
private external fun jsLocationError(result: JsLocationResult): String

/**
 * IP-based geolocation result type.
 */
external interface JsIpLocationResult : JsAny

/**
 * Gets the API base URL based on the current hostname.
 * This duplicates the logic from main.kt to avoid circular dependencies.
 */
@JsFun("""
() => {
    const hostname = window.location.hostname;
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return 'http://localhost:8080';
    } else {
        return 'https://api.fairair.yousef.codes';
    }
}
""")
private external fun jsGetApiBaseUrl(): String

/**
 * Fetch location from IP using our backend proxy endpoint.
 * The backend calls ipapi.co server-to-server to avoid CORS issues.
 */
@JsFun("""
(apiBaseUrl) => fetch(apiBaseUrl + '/api/v1/location/ip')
    .then(r => r.json())
    .catch(() => ({ success: false, error: 'Network error' }))
""")
private external fun jsGetIpLocation(apiBaseUrl: String): Promise<JsIpLocationResult>

@JsFun("(result) => result.success === true && result.latitude !== undefined")
private external fun jsIpLocationSuccess(result: JsIpLocationResult): Boolean

@JsFun("(result) => result.latitude || 0")
private external fun jsIpLocationLatitude(result: JsIpLocationResult): Double

@JsFun("(result) => result.longitude || 0")
private external fun jsIpLocationLongitude(result: JsIpLocationResult): Double

@JsFun("(result) => result.city || ''")
private external fun jsIpLocationCity(result: JsIpLocationResult): String

/**
 * Service for getting user's location and finding nearest airport.
 */
object LocationService {
    
    /**
     * Requests user's location using browser Geolocation API.
     * Falls back to IP-based geolocation if browser geolocation fails.
     * Returns the coordinates or an error/permission denied result.
     */
    suspend fun requestLocation(): LocationResult {
        // First try browser geolocation
        val browserResult = tryBrowserGeolocation()
        if (browserResult is LocationResult.Success) {
            return browserResult
        }
        
        // If browser geolocation fails, try IP-based geolocation
        println("LocationService: Browser geolocation failed, trying IP-based fallback...")
        return tryIpGeolocation()
    }
    
    private suspend fun tryBrowserGeolocation(): LocationResult {
        return try {
            println("LocationService: Checking geolocation support...")
            if (!jsHasGeolocation()) {
                println("LocationService: Geolocation not supported")
                return LocationResult.Error("Geolocation not supported")
            }
            
            println("LocationService: Requesting browser location...")
            val result = jsGetCurrentPosition().await<JsLocationResult>()
            
            if (jsLocationSuccess(result)) {
                val lat = jsLocationLatitude(result)
                val lon = jsLocationLongitude(result)
                println("LocationService: Got browser location: lat=$lat, lon=$lon")
                LocationResult.Success(LocationCoordinates(lat, lon))
            } else {
                val error = jsLocationError(result)
                println("LocationService: Browser location error: $error")
                when (error) {
                    "permission_denied" -> LocationResult.PermissionDenied
                    "not_supported" -> LocationResult.Error("Geolocation not supported")
                    "unavailable" -> LocationResult.Error("Position unavailable")
                    "timeout" -> LocationResult.Error("Location request timed out")
                    else -> LocationResult.Error("Unknown error")
                }
            }
        } catch (e: Exception) {
            println("LocationService: Browser geolocation exception: ${e.message}")
            LocationResult.Error(e.message ?: "Unknown error")
        }
    }
    
    private suspend fun tryIpGeolocation(): LocationResult {
        return try {
            println("LocationService: Requesting IP-based location via backend proxy...")
            val apiBaseUrl = jsGetApiBaseUrl()
            val result = jsGetIpLocation(apiBaseUrl).await<JsIpLocationResult>()
            
            if (jsIpLocationSuccess(result)) {
                val lat = jsIpLocationLatitude(result)
                val lon = jsIpLocationLongitude(result)
                val city = jsIpLocationCity(result)
                println("LocationService: Got IP location: lat=$lat, lon=$lon, city=$city")
                LocationResult.Success(LocationCoordinates(lat, lon))
            } else {
                println("LocationService: IP geolocation failed")
                LocationResult.Error("IP geolocation failed")
            }
        } catch (e: Exception) {
            println("LocationService: IP geolocation exception: ${e.message}")
            LocationResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Finds the nearest airport to the given coordinates.
     * Uses Haversine formula to calculate distance.
     */
    fun findNearestAirport(coordinates: LocationCoordinates): AirportLocation {
        return airportLocations.minByOrNull { airport ->
            haversineDistance(
                coordinates.latitude, coordinates.longitude,
                airport.latitude, airport.longitude
            )
        } ?: airportLocations.first()
    }
    
    /**
     * Gets the nearest airport from the available airports list.
     * @param coordinates User's coordinates
     * @param availableAirportCodes List of airport codes that are available as origins
     * @return The nearest airport code from the available list, or null if none match
     */
    fun findNearestAvailableAirport(
        coordinates: LocationCoordinates,
        availableAirportCodes: List<String>
    ): String? {
        val availableAirports = airportLocations.filter { it.code in availableAirportCodes }
        if (availableAirports.isEmpty()) return null
        
        return availableAirports.minByOrNull { airport ->
            haversineDistance(
                coordinates.latitude, coordinates.longitude,
                airport.latitude, airport.longitude
            )
        }?.code
    }
    
    /**
     * Gets popular destinations from a given origin.
     * Returns up to 6 destinations for the landing page.
     */
    fun getPopularDestinations(
        originCode: String,
        routeMap: Map<String, List<String>>
    ): List<String> {
        return routeMap[originCode]?.take(6) ?: emptyList()
    }
    
    /**
     * Gets popular destinations for a given origin code from the route map.
     * Returns up to 6 destinations.
     */
    fun getPopularDestinationsForOrigin(originCode: String, routeMap: Map<String, List<String>>): List<String> {
        val destinations = routeMap[originCode] ?: emptyList()
        println("LocationService: getPopularDestinationsForOrigin($originCode) = $destinations")
        return destinations.take(6)
    }
    
    /**
     * Gets default popular destinations when location is not available.
     * Uses common Saudi domestic routes - actual availability depends on routes.
     */
    fun getDefaultPopularDestinations(): List<String> {
        return listOf("RUH", "DMM", "AHB", "GIZ", "TUU", "CAI")
    }
    
    /**
     * Haversine formula to calculate distance between two points on Earth.
     * Returns distance in kilometers.
     */
    private fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}

/**
 * Math utilities for Kotlin/Wasm.
 */
private object Math {
    fun toRadians(deg: Double): Double = deg * kotlin.math.PI / 180.0
    fun sin(x: Double): Double = kotlin.math.sin(x)
    fun cos(x: Double): Double = kotlin.math.cos(x)
    fun sqrt(x: Double): Double = kotlin.math.sqrt(x)
    fun atan2(y: Double, x: Double): Double = kotlin.math.atan2(y, x)
}
