package net.badgersmc.votes.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import net.badgersmc.votes.domain.VotePartyState
import net.badgersmc.votes.infrastructure.bukkit.EnthusiaVotesPlugin
import net.badgersmc.votes.infrastructure.config.VoteConfig
import org.bukkit.Server
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VotePartyServiceTest {
    @Test
    fun `load restores active state multiplier counter and timestamp`() {
        val repo = mockk<VoteRepository>(relaxed = true)
        val service = service(VoteConfig(votePartyThreshold = 10), repo)
        val startedAt = Instant.parse("2026-01-01T12:00:00Z")

        service.loadFrom(
            VotePartyState(
                active = true,
                currentVotes = 7,
                threshold = 10,
                justActivated = false,
                startedAt = startedAt,
            )
        )

        assertTrue(service.isPartyActive())
        assertEquals(2.0, service.getCurrentMultiplier())
        assertEquals(7, service.getCurrentVotes())
        assertEquals(3, service.getRemainingVotes())
        assertEquals(startedAt, service.getState().startedAt)
    }

    @Test
    fun `vote below threshold increments and persists exact state`() {
        val repo = mockk<VoteRepository>(relaxed = true)
        val service = service(VoteConfig(votePartyThreshold = 3), repo)
        val saved = slot<VotePartyState>()

        val result = service.onVote()

        assertFalse(result.active)
        assertFalse(result.justActivated)
        assertEquals(1, result.currentVotes)
        assertEquals(3, result.threshold)
        verify(exactly = 1) { repo.savePartyState(capture(saved)) }
        assertEquals(result, saved.captured)
    }

    @Test
    fun `active party votes do not advance counter or repersist state`() {
        val repo = mockk<VoteRepository>(relaxed = true)
        val service = service(VoteConfig(votePartyThreshold = 5), repo)
        service.loadFrom(VotePartyState(true, 4, 5, false, Instant.EPOCH))

        val result = service.onVote()

        assertTrue(result.active)
        assertFalse(result.justActivated)
        assertEquals(0, result.currentVotes)
        assertEquals(4, service.getCurrentVotes())
        verify(exactly = 0) { repo.savePartyState(any()) }
    }

    @Test
    fun `threshold vote activates schedules deactivation and resets cleanly`() {
        val repo = mockk<VoteRepository>(relaxed = true)
        val speaker = mockk<VotePartySpeaker>(relaxed = true)
        val plugin = mockk<EnthusiaVotesPlugin>()
        val server = mockk<Server>()
        val scheduler = mockk<BukkitScheduler>()
        val task = mockk<BukkitTask>(relaxed = true)
        val runnable = slot<Runnable>()

        every { plugin.server } returns server
        every { server.scheduler } returns scheduler
        every { scheduler.runTaskLater(plugin, capture(runnable), 6_000L) } returns task

        val service = VotePartyService(
            VoteConfig(votePartyThreshold = 1, votePartyDurationMinutes = 5),
            plugin,
            repo,
            speaker,
        )

        val activated = service.onVote()

        assertTrue(activated.active)
        assertTrue(activated.justActivated)
        assertEquals(0, service.getCurrentVotes())
        assertEquals(2.0, service.getCurrentMultiplier())
        verify(exactly = 1) { speaker.onPartyActivated() }
        verify(exactly = 1) { scheduler.runTaskLater(plugin, any<Runnable>(), 6_000L) }

        runnable.captured.run()

        assertFalse(service.isPartyActive())
        assertEquals(0, service.getCurrentVotes())
        assertEquals(1.0, service.getCurrentMultiplier())
        assertEquals(null, service.getState().startedAt)
        verify(exactly = 1) { speaker.onPartyDeactivated() }
        verify(atLeast = 2) { repo.savePartyState(any()) }
    }

    @Test
    fun `remaining votes never goes negative for restored oversized counters`() {
        val service = service(VoteConfig(votePartyThreshold = 3), mockk(relaxed = true))
        service.loadFrom(VotePartyState(false, 99, 3, false))

        assertEquals(0, service.getRemainingVotes())
    }

    private fun service(config: VoteConfig, repo: VoteRepository): VotePartyService = VotePartyService(
        config,
        mockk<EnthusiaVotesPlugin>(relaxed = true),
        repo,
        mockk<VotePartySpeaker>(relaxed = true),
    )
}
