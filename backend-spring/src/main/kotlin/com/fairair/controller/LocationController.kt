package com.fairair.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * Controller for location-related endpoints.
 * Provides IP-based geolocation as a proxy to avoid CORS issues in the browser.
 */
@RestController
@RequestMapping("/api/v1/location")
class LocationController {
    
    private val log = LoggerFactory.getLogger(LocationController::class.java)
    private val webClient = WebClient.builder()
        .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(256 * 1024) }
        .build()
    
    /**
     * GET /api/v1/location/ip
     * 
     * Returns the geolocation of the client's IP address.
     * Uses ipwho.is as a backend service (server-to-server call avoids CORS).
     * 
     * @return IpLocationResponse with coordinates and city
     */
    @GetMapping("/ip")
    suspend fun getIpLocation(
        @RequestHeader("X-Forwarded-For", required = false) forwardedFor: String?,
        @RequestHeader("X-Real-IP", required = false) realIp: String?
    ): ResponseEntity<IpLocationResponse> {
        // Get client IP from headers (Cloud Run uses X-Forwarded-For)
        val clientIp = forwardedFor?.split(",")?.firstOrNull()?.trim()
            ?: realIp
            ?: "unknown"
        
        log.info("GET /location/ip for client IP: $clientIp")
        
        return try {
            // Call ipwho.is with the client IP (free, no rate limits, no API key needed)
            val response = webClient.get()
                .uri("https://ipwho.is/$clientIp")
                .retrieve()
                .awaitBody<Map<String, Any?>>()
            
            val success = response["success"] as? Boolean ?: false
            
            if (success) {
                val latitude = (response["latitude"] as? Number)?.toDouble()
                val longitude = (response["longitude"] as? Number)?.toDouble()
                val city = response["city"] as? String
                val country = response["country"] as? String
                
                if (latitude != null && longitude != null) {
                    ResponseEntity.ok(IpLocationResponse(
                        success = true,
                        latitude = latitude,
                        longitude = longitude,
                        city = city,
                        country = country
                    ))
                } else {
                    log.warn("IP geolocation returned no coordinates for IP: $clientIp")
                    ResponseEntity.ok(IpLocationResponse(
                        success = false,
                        error = "Could not determine location"
                    ))
                }
            } else {
                val errorMessage = response["message"] as? String ?: "Unknown error"
                log.warn("IP geolocation failed for IP $clientIp: $errorMessage")
                ResponseEntity.ok(IpLocationResponse(
                    success = false,
                    error = errorMessage
                ))
            }
        } catch (e: Exception) {
            log.warn("IP geolocation failed for IP $clientIp: ${e.message}")
            ResponseEntity.ok(IpLocationResponse(
                success = false,
                error = e.message ?: "Geolocation service unavailable"
            ))
        }
    }
}

/**
 * Response DTO for IP geolocation.
 */
data class IpLocationResponse(
    val success: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    val country: String? = null,
    val error: String? = null
)
