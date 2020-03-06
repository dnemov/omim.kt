package com.mapswithme.maps.auth

interface AuthorizationListener {
    fun onAuthorized(success: Boolean)
}