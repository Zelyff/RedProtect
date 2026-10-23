# Bedrock Forms UX and permission design

## Scope
A **supplementary native Bedrock menu** using Floodgate/Cumulus, not an alternate region engine. Java keeps existing commands and GUIs. Detection must be feature-gated and fail closed when Floodgate absent/old. Do not make Floodgate a hard dependency for Java players.

## Navigation
```
Main Claim Menu
  Current location → Claim Summary (owner, size, flags summary)
                    → Inspect Border (read-only)
                    → Rename [if allowed]
                    → Manage Members [if allowed]
                    → Flags [only keys exposed by existing RP permission]
                    → Resize (corner selection guidance → confirm)
                    → Delete (danger confirmation, double-check)
  My Claims → Page list → Claim Summary
  Claim Help → Tool interaction guide
```
Use the *existing* RP action path for `claim`, `rename`, `addmember`, `removemember`, `flag`, and `delete`; if upstream does not expose a safe service path for an operation, **omit that button** rather than hacking data directly.

## Mobile-friendly design decisions
- Small screens: concise headings and descriptions, use native Simple/Custom/Modal Forms, avoid very long choice lists; paginate claim lists.
- Give commands as fallback for forms that unexpectedly close.
- Do not ask to type precise coordinates; use a selection mode where a player taps blocks. Forms for choosing mode, preview, confirmation, settings.
- If opening a Form from a shovel interaction is unreliable, require command `/rp` and a Bedrock-specific form entry trigger configurable after checking command conflicts.
- No custom icon/resource pack needed for v1.

## Security & consistency
1. Identify Bedrock via Floodgate API using UUID, never through username prefix guesses.
2. On each Form request, fetch live player/context from Bukkit; derive permissions on server.
3. Render only allowed actions and validate them **again** upon response.
4. Opaque session tokens bound to actor, region ID and revision/version, nonce, world, expiry; reject stale/double-click/replayed submits.
5. Sanitize any free-text input (names, messages); follow existing RP naming and length limits. Never interpolate raw text into commands.
6. Display regional flag keys/options from the upstream allowlist; no bypass for admin-only flags.
7. Missing dependencies or packet errors: disable Forms adapter and keep legacy RedProtect functional.
8. For deletes, double-confirm by re-fetching region and showing name+world; no default destructive selection.

## Platform matrix
| Client | Preferred | Fallback |
|---|---|---|
| Java | Original RP commands/GUI + tools | Original commands |
| Bedrock/Floodgate | Native Forms + tools | Original `/rp` commands and virtual blocks |
| Bedrock via non-Floodgate setup | No automatic Forms assumption | Commands and vanilla-compatible inputs |
| Java with Floodgate installed | Still Java UI | Commands |

## Implementation tasks
- Detect exact Floodgate and Cumulus API versions compatible with locked Paper target.
- Add bridge behind `classPresent + pluginEnabled` detection or Maven provided API; avoid classloader failure when absent.
- Query original RP permissions/regions, do not introduce a duplicate database or ownership cache.
- Add functional tests for role lost between open and response, region deleted by another player, click twice, disconnect, world switch, invalid values, simultaneous edits, and arbitrary crafted response.

## Sources
https://geysermc.org/wiki/floodgate/api/
https://github.com/GeyserMC/Floodgate/wiki/Forms
