# EnthusiaVotes

EnthusiaVotes is Enthusia SMP's NuVotifier-based voting/reward system. It records votes, pays Raw Gold rewards, tracks daily vote streaks, runs server-wide Vote Parties, gives voting-based gold-mining bonuses, supports offline vote delivery, provides Java/Bedrock-friendly vote-site access, and exposes vote statistics/leaderboards.

## Current Enthusia SMP configuration

The current live server snapshot uses:

- **5 vote sites**
- direct vote reward: random **1-10 Raw Gold** per vote
- all-sites-today bonus: **+20 Raw Gold**
- vote streak mining multipliers: implemented in code (see below)
- Vote Party threshold: **100 server votes**
- Vote Party duration: **30 minutes**
- vote reminder interval: **30 minutes**
- storage: **SQLite** (`votes.db`)

Current configured vote sites are:

1. Minecraft-servers.gg
2. Minecraft.buzz
3. MCSL / minecraft-serverlist.com
4. MinecraftServers.org
5. Minecraft-server-list.com

Use the live `config.yml` for the exact current URLs; repository defaults contain placeholder/older voting links and are not the live source of truth.

## Player commands

The plugin provides player-facing vote commands for:

- viewing voting status/statistics and the current multiplier,
- seeing which configured sites have already been voted on today,
- opening/clicking vote-site links,
- viewing top voters.

The main `/vote` output includes:

- total recorded votes,
- current daily streak,
- best streak,
- current **gold-mining multiplier**,
- each configured vote site and whether it has been completed today.

The implementation also contains a dedicated vote-sites command and vote-top leaderboard command.

## What one vote gives

When NuVotifier reports a valid vote, the plugin:

1. records the vote and voting service,
2. updates the player's total/streak statistics,
3. chooses a random direct reward between the configured minimum and maximum,
4. gives/queues that Raw Gold,
5. updates Vote Party progress,
6. updates the player's voting-based mining multiplier,
7. broadcasts the vote/reward message.

On the current SMP configuration, the direct base reward is **1-10 Raw Gold**.

### Offline votes

If the player is offline when the vote arrives, the Raw Gold reward is stored in the database and delivered after the player next logs in rather than being discarded.

Current implementation caveat: the all-sites-today completion bonus is only evaluated in the online-player branch of vote processing. An offline vote receives its base queued Raw Gold, but that vote does not trigger the all-sites completion check at vote-processing time.

## Daily vote streaks

A streak counts **days**, not raw vote count.

- Additional votes on the same calendar day do not increment the streak again.
- Voting on the next calendar day increments it by one.
- Missing a day resets the current streak to 1 on the next vote.

The plugin stores both current streak and best streak.

## Voting-based mining multiplier

The streak multiplier is applied when a player mines gold ore. It does **not multiply the direct Raw Gold payout from the vote itself**.

Current code thresholds are:

| Current streak | Mining multiplier |
| --- | ---: |
| 1-2 days | 1.0x |
| 3-6 days | 1.5x |
| 7-29 days | 2.0x |
| 30+ days | 3.0x |

The multiplier works by preserving normal vanilla/Fortune drops and then dropping additional **Raw Gold** on top.

It applies to:

- Gold Ore
- Deepslate Gold Ore
- Nether Gold Ore

Fractional bonuses are probabilistic. For example, a 1.5x voting multiplier gives an additional Raw Gold on roughly half of qualifying ore breaks rather than replacing vanilla drop calculations.

## Vote Party

Every accepted server vote advances a shared Vote Party counter while a party is not already active.

Current live settings:

- threshold: **100 votes**
- duration: **30 minutes**
- active Vote Party multiplier: **2x**

When the threshold is reached, Vote Party activates and the shared counter resets. While active, the Vote Party multiplier combines multiplicatively with the player's streak multiplier.

Examples:

- 1-2 day streak during Vote Party: **2x** mining multiplier
- 3-6 day streak: **3x**
- 7-29 day streak: **4x**
- 30+ day streak: **6x**

The party state is persisted so the plugin can recover its progress/state across restart rather than treating it as memory-only.

## Voting on every site in one day

The plugin compares today's recorded NuVotifier service names against every configured vote site. When an online player's vote completes all configured sites for that day, the current live configuration grants **20 extra Raw Gold** and plays the all-sites completion sound.

### Important current implementation detail

`all-sites-bonus-multiplier` is currently configured as `0.5`, and `VoteService` adds that value to the multiplier shown in the vote result/announcement. However, `RewardService` caches/calculates the actual mining multiplier from only:

`streak multiplier × Vote Party multiplier`

Therefore the current **+0.5 all-sites multiplier is not applied to subsequent gold-ore mining by the implementation**. Do not document it as a real mining bonus unless the code is changed. The +20 Raw Gold all-sites reward is real.

## Bedrock support

Floodgate is an optional integration and the project contains a dedicated Bedrock vote form. Vote-site interaction therefore has a Bedrock-aware path rather than relying exclusively on Java chat UX.

## Vote reminders

The server can periodically remind players to vote. The current live reminder interval is **30 minutes**.

## Statistics and leaderboard

The SQLite database stores:

- every vote record,
- voting service,
- direct gold awarded for the recorded vote,
- total votes,
- current streak,
- best streak,
- last vote time,
- pending offline Raw Gold,
- Vote Party state.

The top-voters command ranks players by total votes.

## Sounds and announcements

The current live configuration uses an amethyst-block chime for a normal vote and a player-level-up sound when all vote sites are completed. Vote and Vote Party announcements are broadcast through the plugin's message layer.

## Administration

`enthusiavotes.admin` (operator by default) controls the admin command surface for vote management/testing. Ordinary vote-reward permission is enabled by default.

## Dependencies

Required:

- NuVotifier / Votifier API

Optional integrations:

- PlaceholderAPI
- Floodgate

## Documentation source note

For future player/wiki documentation:

- use current code for streak, Vote Party and mining semantics,
- use the latest `enthusia-server-state` snapshot for current vote sites, reward numbers and reminder/party duration,
- do not assume repository-default vote links are live,
- do not call the voting multiplier a direct vote-payout multiplier: in the current implementation it affects bonus Raw Gold from mining qualifying gold ores.