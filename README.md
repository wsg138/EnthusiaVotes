# EnthusiaVotes

EnthusiaVotes is Enthusia SMP's NuVotifier-backed voting system. It records votes, pays Raw Gold rewards, tracks daily vote streaks, runs server-wide Vote Parties, gives temporary voting-based gold-mining multipliers, supports offline vote delivery, provides Java/Bedrock vote-site UX, and exposes vote statistics/leaderboards.

This repository is a fork of the canonical `BadgersMC/EnthusiaVotes` repository. The documentation here is prepared against the current upstream implementation plus Enthusia's latest sanitized production configuration. The canonical upstream repository should receive the same documentation before future wiki automation treats it as the primary source.

## Current Enthusia SMP configuration

The latest server snapshot uses:

- **5 vote sites**
- direct vote reward: random **1-10 Raw Gold** per vote
- all-sites-today bonus: **+20 Raw Gold**
- Vote Party threshold: **100 accepted server votes**
- Vote Party duration: **30 minutes**
- Vote Party mining multiplier: **2x**
- vote reminder interval: **30 minutes**
- storage: **SQLite** (`votes.db`)

Current configured vote sites are:

1. Minecraft-servers.gg
2. Minecraft.buzz
3. MCSL / minecraft-serverlist.com
4. MinecraftServers.org
5. Minecraft-server-list.com

Use the live server snapshot for exact URLs and NuVotifier `service-name` values. Repository defaults contain example/older voting links.

## Player-facing commands

The plugin provides commands for:

- viewing vote statistics and current mining multiplier,
- seeing which configured vote sites have been completed today,
- opening/clicking vote-site links,
- viewing top voters.

The main `/vote` output includes total votes, current streak, best streak, current gold-mining multiplier, and per-site completion state for today.

The implementation also contains dedicated vote-sites and vote-top command surfaces.

## What one vote gives

For a valid NuVotifier vote, the plugin:

1. records the vote and service,
2. updates total/streak statistics,
3. chooses a random direct Raw Gold reward,
4. delivers or queues that reward,
5. updates Vote Party progress,
6. activates/refreshes the temporary streak mining multiplier when eligible,
7. broadcasts the vote result.

On the current SMP configuration, the base direct reward is **1-10 Raw Gold**.

### Offline votes

If the voter is offline, the base Raw Gold reward is queued in the database and delivered after the player next logs in rather than being discarded.

Current implementation caveat: the all-sites completion check runs only in the online-player vote branch. An offline vote receives its queued base Raw Gold, but that vote does not trigger the all-sites completion bonus at processing time.

## Daily vote streaks

A streak counts **calendar days**, not raw votes.

- Additional votes on the same day do not increase the streak again.
- Voting the next day increments the streak by one.
- Missing a day resets the streak to 1 on the next vote.

The plugin stores both current streak and best streak.

## Temporary streak mining multiplier

The streak multiplier affects **bonus Raw Gold from mining gold ore**. It does not multiply the direct Raw Gold payout from voting.

A player must have at least a **3-day streak**. Each accepted vote at streak 3+ activates or refreshes the multiplier for **20 minutes** from that vote.

Current upstream code uses:

| Current streak | Temporary mining multiplier |
| --- | ---: |
| 1-2 days | 1.0x |
| 3-6 days | 1.5x for 20 minutes after the latest vote |
| 7-29 days | 2.0x for 20 minutes after the latest vote |
| 30+ days | 3.0x for 20 minutes after the latest vote |

After 20 minutes without a qualifying refresh, the streak contribution returns to 1.0x even though the streak itself remains recorded.

### Eligible ore

The mining bonus applies to:

- Gold Ore
- Deepslate Gold Ore
- Nether Gold Ore

Silk Touch is excluded to avoid adding Raw Gold alongside an ore-block drop.

The plugin keeps normal vanilla/Fortune drops and adds extra **Raw Gold**. Fractional multipliers are probabilistic: for example 1.5x adds one extra Raw Gold on roughly half of qualifying ore breaks.

## Vote Party

Every accepted server vote advances a shared Vote Party counter while a party is not already active.

Current live settings:

- threshold: **100 votes**
- duration: **30 minutes**
- active multiplier: **2x**

When the threshold is reached, Vote Party activates and the counter resets. During the party, its 2x multiplier combines multiplicatively with the currently active streak multiplier.

Examples while the player's 20-minute streak multiplier is active:

- 3-6 day streak during Vote Party: **3x**
- 7-29 day streak: **4x**
- 30+ day streak: **6x**

A player without an active streak multiplier still receives the Vote Party's **2x** mining multiplier.

Vote Party state is persisted so progress/state can be recovered across restart rather than being memory-only.

## Voting on every configured site in one day

When an **online** player's vote causes all configured NuVotifier service names to be represented in that player's votes for the current server-local day, the current deployment grants **20 extra Raw Gold** and plays the all-sites completion sound.

### `all-sites-bonus-multiplier` caveat

The live config also sets `all-sites-bonus-multiplier: 0.5`.

Current upstream implementation adds that `+0.5` only to the multiplier value returned/shown for the vote result message. `RewardService.getMiningMultiplier()` calculates actual ore-mining behavior only from:

`active streak multiplier × Vote Party multiplier`

Therefore the configured all-sites `+0.5` is **not currently applied to later gold-ore mining**. The +20 Raw Gold completion bonus is real. Future player/wiki documentation must not advertise a persistent/real +0.5 mining bonus unless the implementation is changed.

## Bedrock support

Floodgate is an optional integration and the project contains a Bedrock-aware vote-site form path. Player instructions should not assume Java chat links are the only supported UI.

## Vote reminders

The server can periodically remind players to vote. The current production interval is **30 minutes**.

## Statistics and leaderboard

The voting database stores information including:

- vote records and service names,
- direct gold recorded for votes,
- total votes,
- current streak,
- best streak,
- last vote time,
- temporary multiplier activation time,
- pending offline Raw Gold,
- Vote Party state.

The top-voters command ranks players by total votes.

## Sounds and announcements

The current production configuration uses:

- `BLOCK_AMETHYST_BLOCK_CHIME` for a normal vote,
- `ENTITY_PLAYER_LEVELUP` for all-sites completion.

Vote and Vote Party announcements are broadcast through the plugin's message layer.

## Dependencies

Required:

- NuVotifier / Votifier API

Optional integrations include:

- Floodgate
- PlaceholderAPI / broader server presentation integrations where configured

## Source-of-truth rule

For future wiki/player documentation:

- current code in `BadgersMC/EnthusiaVotes` defines the mechanics,
- the latest `enthusia-server-state` snapshot defines live sites, reward numbers, reminder interval and Vote Party duration,
- do not use repository-default vote URLs as live values,
- do not describe the mining multiplier as a permanent streak multiplier: it lasts **20 minutes after a qualifying vote**,
- do not describe the configured all-sites `+0.5` as a real ore-mining bonus unless the code is fixed.