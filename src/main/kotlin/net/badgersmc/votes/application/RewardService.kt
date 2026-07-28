package net.badgersmc.votes.application

import net.badgersmc.nexus.i18n.LangService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.UUID

class RewardService(
    private val voteRepository: VoteRepository,
    private val votePartyService: VotePartyService,
    private val lang: LangService,
) {
    companion object {
        /** How long the mining multiplier lasts after activation (millis). */
        const val MULTIPLIER_DURATION_MS: Long = 20 * 60 * 1000L  // 20 minutes per daily vote
    }

    /**
     * Called after every vote. Activates (or refreshes) the mining multiplier
     * for [MULTIPLIER_DURATION_MS] whenever the player's streak is 3+.
     * Returns true if the multiplier was just activated/refreshed.
     */
    fun tryActivateMultiplier(uuid: UUID, streak: Int): Boolean {
        if (streak >= 3) {
            voteRepository.activateMultiplier(uuid)
            return true
        }
        return false
    }

    /**
     * Returns the current mining multiplier. Only active if [MULTIPLIER_DURATION_MS]
     * hasn't elapsed since the last activation. VoteParty multiplier always stacks.
     */
    fun getMiningMultiplier(uuid: UUID): Double {
        val stats = voteRepository.getStats(uuid)
        val elapsed = System.currentTimeMillis() - stats.multiplierActivatedAt
        val streakMult = if (stats.multiplierActivatedAt > 0 && elapsed < MULTIPLIER_DURATION_MS) {
            streakMultiplier(stats.currentStreak)
        } else {
            1.0
        }
        return streakMult * votePartyService.getCurrentMultiplier()
    }

    fun streakMultiplier(streak: Int): Double = when {
        streak >= 30 -> 3.0
        streak >= 7  -> 2.0
        streak >= 3  -> 1.5
        else         -> 1.0
    }

    fun buildVoteMessage(
        playerName: String,
        gold: Int,
        multiplier: Double,
        streak: Int,
        serviceName: String,
    ): Component {
        val streakText = MiniMessage.miniMessage().serialize(
            lang.msg("voteparty.streak_suffix", "streak" to streak.toString())
        )
        return if (multiplier > 1.0) {
            lang.msg(
                "voteparty.reward_message_multiplier",
                "player" to playerName,
                "service" to serviceName,
                "multiplier" to multiplier.toString(),
                "streak_text" to streakText,
            )
        } else {
            lang.msg(
                "voteparty.reward_message",
                "player" to playerName,
                "service" to serviceName,
                "streak_text" to streakText,
            )
        }
    }
}
