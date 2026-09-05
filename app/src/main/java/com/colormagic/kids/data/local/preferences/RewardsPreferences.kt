package com.colormagic.kids.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rewardsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "rewards_prefs")

data class Badge(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val threshold: Int
)

object Badges {
    val firstArtwork = Badge("first_artwork", "First Masterpiece", "🎨", "Create your first artwork", 1)
    val fiveArtworks = Badge("five_artworks", "Art Explorer", "🗺️", "Create 5 artworks", 5)
    val tenArtworks = Badge("ten_artworks", "Art Star", "⭐", "Create 10 artworks", 10)
    val twentyFiveArtworks = Badge("twentyfive_artworks", "Art Champion", "🏆", "Create 25 artworks", 25)
    val fiftyArtworks = Badge("fifty_artworks", "Art Legend", "👑", "Create 50 artworks", 50)
    val colorExplorer = Badge("color_explorer", "Color Explorer", "🌈", "Use 10 different colors", 10)
    val stickerFan = Badge("sticker_fan", "Sticker Fan", "✨", "Place 10 stickers", 10)
    val stickerMaster = Badge("sticker_master", "Sticker Master", "🦄", "Place 50 stickers", 50)
    val weekStreak = Badge("week_streak", "Week Warrior", "🔥", "7-day coloring streak", 7)
    val monthStreak = Badge("month_streak", "Monthly Master", "💪", "30-day coloring streak", 30)
    val challengeStar = Badge("challenge_star", "Challenge Star", "🌟", "Complete 5 challenges", 5)

    val all: List<Badge> = listOf(
        firstArtwork, fiveArtworks, tenArtworks, twentyFiveArtworks, fiftyArtworks,
        colorExplorer, stickerFan, stickerMaster, weekStreak, monthStreak, challengeStar
    )
}

data class RewardsState(
    val totalArtworks: Int = 0,
    val totalColorsUsed: Int = 0,
    val totalStickersPlaced: Int = 0,
    val bestStreak: Int = 0,
    val totalChallenges: Int = 0,
    val earnedBadgeIds: Set<String> = emptySet()
) {
    val level: Int get() = (totalArtworks / 5) + 1
    val artworksToNextLevel: Int get() = 5 - (totalArtworks % 5)

    fun earnedBadges(): List<Badge> = Badges.all.filter { it.id in earnedBadgeIds }
    fun unearnedBadges(): List<Badge> = Badges.all.filter { it.id !in earnedBadgeIds }
}

@Singleton
class RewardsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.rewardsDataStore

    val state: Flow<RewardsState> = store.data.map { prefs ->
        val earnedStr = prefs[KEY_EARNED_BADGES] ?: ""
        RewardsState(
            totalArtworks = prefs[KEY_TOTAL_ARTWORKS] ?: 0,
            totalColorsUsed = prefs[KEY_TOTAL_COLORS] ?: 0,
            totalStickersPlaced = prefs[KEY_TOTAL_STICKERS] ?: 0,
            bestStreak = prefs[KEY_BEST_STREAK] ?: 0,
            totalChallenges = prefs[KEY_TOTAL_CHALLENGES] ?: 0,
            earnedBadgeIds = if (earnedStr.isEmpty()) emptySet() else earnedStr.split(",").toSet()
        )
    }

    suspend fun recordArtworkSaved(colorsUsed: Int, stickersPlaced: Int, currentStreak: Int): List<Badge> {
        val newBadges = mutableListOf<Badge>()
        store.edit { prefs ->
            val artworks = (prefs[KEY_TOTAL_ARTWORKS] ?: 0) + 1
            val colors = (prefs[KEY_TOTAL_COLORS] ?: 0) + colorsUsed
            val stickers = (prefs[KEY_TOTAL_STICKERS] ?: 0) + stickersPlaced
            val streak = maxOf(prefs[KEY_BEST_STREAK] ?: 0, currentStreak)

            prefs[KEY_TOTAL_ARTWORKS] = artworks
            prefs[KEY_TOTAL_COLORS] = colors
            prefs[KEY_TOTAL_STICKERS] = stickers
            prefs[KEY_BEST_STREAK] = streak

            val earned = (prefs[KEY_EARNED_BADGES] ?: "").let {
                if (it.isEmpty()) mutableSetOf() else it.split(",").toMutableSet()
            }

            fun check(badge: Badge, value: Int) {
                if (badge.id !in earned && value >= badge.threshold) {
                    earned.add(badge.id)
                    newBadges.add(badge)
                }
            }

            check(Badges.firstArtwork, artworks)
            check(Badges.fiveArtworks, artworks)
            check(Badges.tenArtworks, artworks)
            check(Badges.twentyFiveArtworks, artworks)
            check(Badges.fiftyArtworks, artworks)
            check(Badges.colorExplorer, colors)
            check(Badges.stickerFan, stickers)
            check(Badges.stickerMaster, stickers)
            check(Badges.weekStreak, streak)
            check(Badges.monthStreak, streak)

            prefs[KEY_EARNED_BADGES] = earned.joinToString(",")
        }
        return newBadges
    }

    suspend fun recordChallengeCompleted(): List<Badge> {
        val newBadges = mutableListOf<Badge>()
        store.edit { prefs ->
            val total = (prefs[KEY_TOTAL_CHALLENGES] ?: 0) + 1
            prefs[KEY_TOTAL_CHALLENGES] = total

            val earned = (prefs[KEY_EARNED_BADGES] ?: "").let {
                if (it.isEmpty()) mutableSetOf() else it.split(",").toMutableSet()
            }
            if (Badges.challengeStar.id !in earned && total >= Badges.challengeStar.threshold) {
                earned.add(Badges.challengeStar.id)
                newBadges.add(Badges.challengeStar)
            }
            prefs[KEY_EARNED_BADGES] = earned.joinToString(",")
        }
        return newBadges
    }

    private companion object {
        val KEY_TOTAL_ARTWORKS = intPreferencesKey("rewards_total_artworks")
        val KEY_TOTAL_COLORS = intPreferencesKey("rewards_total_colors")
        val KEY_TOTAL_STICKERS = intPreferencesKey("rewards_total_stickers")
        val KEY_BEST_STREAK = intPreferencesKey("rewards_best_streak")
        val KEY_TOTAL_CHALLENGES = intPreferencesKey("rewards_total_challenges")
        val KEY_EARNED_BADGES = stringPreferencesKey("rewards_earned_badges")
    }
}
