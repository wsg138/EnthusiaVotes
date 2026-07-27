package net.badgersmc.votes.application

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.domain.PlayerStats
import net.badgersmc.votes.domain.VoteParty
import net.badgersmc.votes.domain.VoteRecord
import net.badgersmc.votes.infrastructure.config.VoteConfig
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import java.util.UUID

class VoteService(
    private val repo: VoteRepository,
    private val rewardService: RewardService,
    private val broadcaster: VoteBroadcaster,
    private val goldDelivery: GoldDelivery,
    private val votePartyService: VotePartyService,
    private val config: VoteConfig,
    private val lang: LangService,
) {
    private val voteSound: Sound by lazy { parseSound(config.voteSound) }
    private val allSitesSound: Sound by lazy { parseSound(config.allSitesSound) }

    private fun parseSound(name: String): Sound = try {
        Sound.valueOf(name.uppercase())
    } catch (_: IllegalArgumentException) {
        Sound.ENTITY_EXPERIENCE_ORB_PICKUP
    }

    fun processVote(playerName: String, playerUuid: UUID, serviceName: String): VoteResult {
        val stats = repo.getStats(playerUuid)

        var gold = (config.minGold..config.maxGold).random()
        val record = VoteRecord(
            playerUuid = playerUuid,
            playerName = playerName,
            serviceName = serviceName,
            goldAwarded = gold,
        )
        repo.saveVote(record)

        val player = Bukkit.getPlayer(playerUuid)
        val allSitesComplete: Boolean
        if (player != null) {
            // Audio cue for individual vote (tabbed-out voters)
            try { player.playSound(player.location, voteSound, 1.0f, 1.0f) } catch (_: Exception) {}

            // Check if they voted on all configured sites today (match by serviceName)
            val todaysServices = repo.getTodaysServices(playerUuid)
            val matchedSites = config.voteSites.count { it.serviceName.isNotBlank() && it.serviceName in todaysServices }
            allSitesComplete = matchedSites >= config.voteSites.size && config.voteSites.isNotEmpty()
            if (allSitesComplete) {
                gold += config.allSitesBonusGold
                try { player.playSound(player.location, allSitesSound, 1.0f, 1.0f) } catch (_: Exception) {}
                val bonusMsg = lang.msg("voteparty.all_sites_bonus", "bonus" to config.allSitesBonusGold.toString())
                player.sendMessage(bonusMsg)
            }

            goldDelivery.deliver(playerUuid, gold)
        } else {
            allSitesComplete = false
            repo.queueOfflineGold(playerUuid, gold)
        }

        // VoteParty: check if party just activated (AFTER saving vote, so threshold voter gets bonus)
        val partyState = votePartyService.onVote()
        if (partyState.justActivated) {
            val partyMsg = lang.msg(
                "voteparty.broadcast",
                "minutes" to config.votePartyDurationMinutes.toString(),
            )
            broadcaster.broadcastVoteParty(partyMsg)
        }

        // Activate/refresh mining multiplier if streak is 3+ (lasts 20m)
        val newStats = repo.getStats(playerUuid)
        rewardService.tryActivateMultiplier(playerUuid, newStats.currentStreak)

        val baseMultiplier = rewardService.getMiningMultiplier(playerUuid)
        val multiplier = if (allSitesComplete) baseMultiplier + config.allSitesBonusMultiplier else baseMultiplier

        val message = rewardService.buildVoteMessage(playerName, gold, multiplier, newStats.currentStreak, serviceName)
        broadcaster.broadcastVote(message)

        return VoteResult(
            record = record,
            stats = newStats,
            gold = gold,
            multiplier = multiplier,
            streak = newStats.currentStreak,
            broadcastMessage = message,
        )
    }
}

data class VoteResult(
    val record: VoteRecord,
    val stats: PlayerStats,
    val gold: Int,
    val multiplier: Double,
    val streak: Int,
    val broadcastMessage: Component,
)
