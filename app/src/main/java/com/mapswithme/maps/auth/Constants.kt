package com.mapswithme.maps.auth

internal object Constants {
    const val REQ_CODE_PHONE_AUTH_RESULT = 102
    const val REQ_CODE_GOOGLE_SIGN_IN = 103
    const val EXTRA_SOCIAL_TOKEN = "extra_social_token"
    const val EXTRA_PHONE_AUTH_TOKEN = "extra_phone_auth_token"
    const val EXTRA_TOKEN_TYPE = "extra_token_type"
    const val EXTRA_AUTH_ERROR = "extra_auth_error"
    const val EXTRA_IS_CANCEL = "extra_is_cancel"
    const val EXTRA_PRIVACY_POLICY_ACCEPTED = "extra_privacy_policy_accepted"
    const val EXTRA_TERMS_OF_USE_ACCEPTED = "extra_terms_of_use_accepted"
    const val EXTRA_PROMO_ACCEPTED = "extra_promo_accepted"
    val FACEBOOK_PERMISSIONS =
        listOf("email")
}