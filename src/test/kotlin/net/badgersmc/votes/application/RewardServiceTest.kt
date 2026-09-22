package net.badgersmc.votes.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.domain.PlayerStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RewardServiceTest {
    private val player = UUID.fromString("11111111-2222-3333-4444-555555555555")

    @Test
    fun `streak multiplier changes exactly at configured tiers`() {
        val service = service()

        assertEquals(1.0, service.streakMultiplier(0))
        assertEquals(1.0, service.streakMultiplier(2))
        assertEquals(1.5, service.streakMultiplier(3))
        assertEquals(1.5, service.streakMultiplier(6))
        assertEquals(2.0, service.streakMultiplier(7))
        assertEquals(2.0, service.streakMultiplier(29))
        assertEquals(3.0, service.streakMultiplier(30))
        assertEquals(3.0, service.streakMultiplier(100))
    }

    @Test
    fun `cache multiplier combines streak and active party multipliers`() {
        val repo = mockk<VoteRepository>(relaxed = true)
        val party = mockk<VotePartyService>()
        every { party.getCurrentMultiplier() } returns 2.0
        val service = RewardService(repo, party, mockk<LangService>(relaxed = true))

        service.cacheMultiplier(player, 7)

        assertEquals(4.0, service.getMiningMultiplier(player))
        verify(exactly = 0) { repo.getStats(any()) }
    }

    @Test
    fun `mining multiplier loads stats once then serves cached value`() {
        val repo = mockk<VoteRepository>()
        val party = mockk<VotePartyService>()
        every { repo.getStats(player) } returns PlayerStats(playerUuid = player, currentStreak = 3)
        every { party.getCurrentMultiplier() } returns 1.0
        val service = RewardService(repo, party, mockk<LangService>(relaxed = true))

        assertEquals(1.5, service.getMiningMultiplier(player))
        assertEquals(1.5, service.getMiningMultiplier(player))

        verify(exactly = 1) { repo.getStats(player) }
        verify(exactly = 1) { party.getCurrentMultiplier() }
    }

    @Test
    fun `explicit cache refresh replaces an older derived value`() {
        val repo = mockk<VoteRepository>()
        val party = mockk<VotePartyService>()
        every { repo.getStats(player) } returns PlayerStats(playerUuid = player, currentStreak = 0)
        every { party.getCurrentMultiplier() } returnsMany listOf(1.0, 2.0)
        val service = RewardService(repo, party, mockk<LangService>(relaxed = true))

        assertEquals(1.0, service.getMiningMultiplier(player))
        service.cacheMultiplier(player, 30)
        assertEquals(6.0, service.getMiningMultiplier(player))
    }

    private fun service(): RewardService = RewardService(
        mockk<VoteRepository>(relaxed = true),
        mockk<VotePartyService>(relaxed = true),
        mockk<LangService>(relaxed = true),
    )
}
