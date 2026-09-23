package net.badgersmc.votes

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FullFeatureCoverageContractTest {
    @Test
    fun `established feature families retain concrete regression evidence`() {
        val root = repositoryRoot()
        coverage().forEach { (feature, evidence) ->
            evidence.forEach { relative ->
                assertTrue(
                    Files.isRegularFile(root.resolve(relative)),
                    "$feature lost required regression evidence: $relative",
                )
            }
        }
    }

    private fun coverage(): Map<String, List<String>> = linkedMapOf(
        "reward streak and mining multipliers" to listOf(
            "src/test/kotlin/net/badgersmc/votes/application/RewardServiceTest.kt",
        ),
        "vote party lifecycle and persistence calls" to listOf(
            "src/test/kotlin/net/badgersmc/votes/application/VotePartyServiceTest.kt",
        ),
        "SQLite vote statistics offline rewards and party snapshots" to listOf(
            "src/test/kotlin/net/badgersmc/votes/infrastructure/persistence/SqliteVoteRepositoryTest.kt",
        ),
        "architecture boundaries" to listOf(
            "src/test/kotlin/architecture/LayerRulesTest.kt",
        ),
    )

    private fun repositoryRoot(): Path {
        val current = Path.of("").toAbsolutePath().normalize()
        if (Files.isRegularFile(current.resolve("settings.gradle.kts"))) return current
        val parent = current.parent
        if (parent != null && Files.isRegularFile(parent.resolve("settings.gradle.kts"))) return parent
        error("Could not locate EnthusiaVotes repository root from $current")
    }
}
