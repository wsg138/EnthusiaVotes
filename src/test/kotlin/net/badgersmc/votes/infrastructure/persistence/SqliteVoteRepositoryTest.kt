package net.badgersmc.votes.infrastructure.persistence

import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import net.badgersmc.votes.domain.VotePartyState
import net.badgersmc.votes.domain.VoteRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SqliteVoteRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `votes persist totals streaks top voters and todays services`() {
        val repo = repository()
        val zone = ZoneId.systemDefault()
        val todayNoon = LocalDate.now(zone).atTime(12, 0).atZone(zone).toInstant()
        val yesterdayNoon = LocalDate.now(zone).minusDays(1).atTime(12, 0).atZone(zone).toInstant()
        val alice = UUID.fromString("11111111-2222-3333-4444-555555555555")
        val bob = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")

        repo.saveVote(VoteRecord(playerUuid = alice, playerName = "Alice", serviceName = "SiteA", timestamp = yesterdayNoon, goldAwarded = 2))
        repo.saveVote(VoteRecord(playerUuid = alice, playerName = "Alice", serviceName = "SiteB", timestamp = todayNoon, goldAwarded = 3))
        repo.saveVote(VoteRecord(playerUuid = alice, playerName = "Alice", serviceName = "SiteC", timestamp = todayNoon.plusSeconds(60), goldAwarded = 4))
        repo.saveVote(VoteRecord(playerUuid = bob, playerName = "Bob", serviceName = "SiteA", timestamp = todayNoon, goldAwarded = 5))

        val aliceStats = repo.getStats(alice)
        assertEquals(3, aliceStats.totalVotes)
        assertEquals(2, aliceStats.currentStreak)
        assertEquals(2, aliceStats.bestStreak)
        assertEquals(todayNoon.plusSeconds(60).epochSecond, aliceStats.lastVoteAt?.epochSecond)
        assertEquals(3, repo.getTotalVotes(alice))
        assertEquals(4, repo.getTotalServerVotes())
        assertEquals(setOf("SiteB", "SiteC"), repo.getTodaysServices(alice))

        val top = repo.getTopVoters(2)
        assertEquals(listOf(alice, bob), top.map { it.playerUuid })
        assertEquals(listOf(3, 1), top.map { it.totalVotes })
    }

    @Test
    fun `missing player stats are safe zero defaults`() {
        val repo = repository()
        val player = UUID.randomUUID()

        val stats = repo.getStats(player)

        assertEquals(player, stats.playerUuid)
        assertEquals(0, stats.totalVotes)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)
        assertNull(stats.lastVoteAt)
    }

    @Test
    fun `offline gold accumulates and can be cleared`() {
        val repo = repository()
        val player = UUID.randomUUID()

        assertNull(repo.getPendingOfflineGold(player))
        repo.queueOfflineGold(player, 4)
        repo.queueOfflineGold(player, 7)
        assertEquals(11, repo.getPendingOfflineGold(player))

        repo.clearOfflineGold(player)
        assertNull(repo.getPendingOfflineGold(player))
    }

    @Test
    fun `vote party state round trips including nullable start time`() {
        val repo = repository()
        val startedAt = java.time.Instant.parse("2026-01-02T03:04:05Z")

        assertNull(repo.loadPartyState())

        repo.savePartyState(VotePartyState(active = true, currentVotes = 0, threshold = 25, justActivated = false, startedAt = startedAt))
        val active = repo.loadPartyState()
        assertTrue(active!!.active)
        assertEquals(0, active.currentVotes)
        assertEquals(25, active.threshold)
        assertFalse(active.justActivated)
        assertEquals(startedAt, active.startedAt)

        repo.savePartyState(VotePartyState(active = false, currentVotes = 9, threshold = 25, justActivated = true, startedAt = null))
        val inactive = repo.loadPartyState()
        assertFalse(inactive!!.active)
        assertEquals(9, inactive.currentVotes)
        assertEquals(25, inactive.threshold)
        assertFalse(inactive.justActivated, "justActivated is transient and must not persist")
        assertNull(inactive.startedAt)
    }

    private fun repository(): SqliteVoteRepository {
        val factory = LocalDatabaseFactory(tempDir.toFile(), "votes-${UUID.randomUUID()}.db")
        Migrations.run(factory.database)
        return SqliteVoteRepository(factory)
    }
}
