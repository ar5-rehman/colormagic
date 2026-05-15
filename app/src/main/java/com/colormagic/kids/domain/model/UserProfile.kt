package com.colormagic.kids.domain.model

// Shape of the authenticated parent. Populated by AuthRepository.
data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null
)
