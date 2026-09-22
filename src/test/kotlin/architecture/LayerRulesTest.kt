package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LayerRulesTest {
    @Test
    fun `domain layer must not depend on infrastructure`() {
        productionFilesIn("net.badgersmc.votes.domain")
            .assertFalse {
                it.hasImport { imported -> imported.name.startsWith("net.badgersmc.votes.infrastructure") }
            }
    }

    @Test
    fun `application infrastructure dependencies are limited to reviewed legacy exceptions`() {
        val offenders = productionFilesIn("net.badgersmc.votes.application")
            .filter {
                it.hasImport { imported -> imported.name.startsWith("net.badgersmc.votes.infrastructure") }
            }
            .map { it.name.removeSuffix(".kt") }
            .toSet()

        assertEquals(
            setOf("VotePartyService", "VoteService"),
            offenders,
            "new application-to-infrastructure dependencies require an explicit architecture decision",
        )
    }

    @Test
    fun `domain layer must stay independent of Bukkit and Exposed`() {
        productionFilesIn("net.badgersmc.votes.domain")
            .assertFalse {
                it.hasImport { imported ->
                    imported.name.startsWith("org.bukkit") || imported.name.startsWith("org.jetbrains.exposed")
                }
            }
    }

    private fun productionFilesIn(packagePrefix: String): List<KoFileDeclaration> =
        Konsist
            .scopeFromProduction()
            .files
            .filter { it.packagee?.name?.startsWith(packagePrefix) == true }
}
