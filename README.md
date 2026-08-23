# EnthusiaVotes — noncanonical fork

This repository is a fork of the canonical `BadgersMC/EnthusiaVotes` project.

Do **not** use this fork's `main` branch as the primary implementation source for Enthusia wiki/player documentation. Earlier documentation on this branch incorrectly implied that the streak mining multiplier remained active for the whole streak day.

Current routing is maintained in:

- canonical code: `BadgersMC/EnthusiaVotes`
- current Enthusia deployment interpretation: `wsg138/enthusia-server-state/repo-overlays/BadgersMC-EnthusiaVotes.md`
- full current documentation candidate on this fork: branch `docs/enthusia-player-guide`

Key current facts:

- direct vote reward: **1-10 Raw Gold**
- all-sites completion: **+20 Raw Gold** for the online completion path
- Vote Party: **100 votes**, **30 minutes**, **2x** mining multiplier
- streak mining multiplier activates only at streak 3+ and lasts **20 minutes after the latest qualifying vote**
- streak tiers while active: 1.5x at 3-6 days, 2x at 7-29 days, 3x at 30+ days
- the configured all-sites `+0.5` is included in vote-result messaging but is **not applied by the actual mining-multiplier calculation**
- the mining bonus adds Raw Gold on qualifying gold ores and excludes Silk Touch

Use the canonical code plus the current server-state overlay for future wiki work.