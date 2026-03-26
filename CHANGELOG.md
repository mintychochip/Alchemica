# Changelog

## Unreleased

### Fixed
- `slow-falling-effect` was incorrectly producing `water_breathing` instead of `slow_falling`
- All brewing nodes defaulted to `OP`-only permission — non-op players could not brew anything. Default is now `TRUE` (all players allowed; revoke per-node with `alchemica.<node-name>`)
- Brewing recipe was evaluated twice on bottle fill — result is now cached on the cauldron after stirring and reused, with a fallback search only on first fill after a server restart
- `BlockBreakEvent` and `BlockPistonExtendEvent` handlers were dispatching DAO removals off the main thread via `CompletableFuture.runAsync`, risking race conditions; removal now runs synchronously on the main thread

### Removed
- Dead code: `PotionContextEvaluator` (was fully commented out)
