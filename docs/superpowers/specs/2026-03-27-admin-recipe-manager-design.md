# Admin Recipe Manager — Design Spec

**Date:** 2026-03-27
**Branch:** feature/custom-potions
**Status:** Approved

---

## Overview

A chest GUI-based admin toolset for Alchemica that lets server owners create, browse, edit, and delete potion recipes entirely in-game without touching yml files. All changes persist to `general.yml` (vanilla recipes) or `potions.yml` (custom potions) and hot-reload the registry immediately on save.

**Out of scope:** Concurrent multi-admin edit conflicts. The last save wins. Operators coordinate admin access.

---

## Entry Point

**Command:** `/brew` (requires `alchemica.admin` permission)

Opens a 3-row chest GUI hub with two options:

- **Create Recipe** — launches the recipe wizard
- **Browse Recipes** — opens the recipe browser

If the admin opens `/brew` while a wizard session is already active **and not in lore chat capture**, the existing session is discarded, a warning is sent, and the hub opens fresh.

If the admin is in lore chat capture, `/brew` is blocked: `"Finish entering lore first (send a blank line to stop)."` The lore listener takes priority until capture ends or the player disconnects.

---

## Permissions

`plugin.yml` gains a `permissions:` block. `alchemica.admin.reload` is the permission for the existing `/creload` command (previously only checked in code, not declared in `plugin.yml`). Server operators who have explicitly granted `alchemica.admin` to non-op players should be aware that they will now also receive `alchemica.admin.reload` via child inheritance:

```yaml
permissions:
  alchemica.admin:
    description: Full admin access to Alchemica recipe management
    default: op
    children:
      alchemica.admin.reload: true
  alchemica.admin.reload:
    description: Allows use of /creload (hot-reload configs)
    default: op
```

---

## Recipe Wizard

A step-by-step flow across multiple chest GUIs. Every screen has a **Back** button and a **Cancel** button. Back at Step 1 returns to the hub (equivalent to Cancel). Cancel at any step discards the session and returns to the hub.

The wizard has two paths:

**Vanilla path (5 steps):** Key → Ingredients → Result Type → Modifiers → Confirm

**Custom path (6 steps):** Key → Ingredients → Result Type → Custom Properties + Effects → Modifiers → Confirm

The GUI title shows `"Create Recipe — Step N / T"`. Before Step 3 the total is `"?"`. When the admin selects Vanilla or Custom at Step 3, the total is immediately set: `5` if vanilla, `6` if custom — regardless of whether modifiers are configured. The total then decreases by 1 (to `4` or `5`) only when the wizard actually reaches the Modifiers step and determines it should be skipped (i.e., `modifiers:` is empty or absent in `general.yml`). This means the title may briefly show `5` or `6` before dropping to `4` or `5` as the admin moves past Step 3. Back button navigation always preserves all session state. The Modifiers step is skipped in both directions: navigating Back from Confirm also skips Modifiers when it was skipped going forward.

Wizard state is held in a `WizardSession` per-player. On disconnect (`PlayerQuitEvent`) the session is discarded. On reconnect the admin starts fresh.

**Edit mode:** `WizardSession` carries `boolean editMode` and `String originalKey`. In edit mode the duplicate-key check at Step 1 exempts `originalKey`. Changing the key to any other already-in-use key is still rejected.

**`resultType`** is stored as a `RecipeResultType` enum: `VANILLA` or `CUSTOM`. For vanilla, the session also carries a `PotionType potionType` field.

**`&` colour codes** in name and lore are stored as raw `&`-prefixed strings in yml (e.g. `&6My Brew`). Translation to section-sign codes (`§6`) happens at item-meta build time, not at input or storage time. `RecipeWizardGui` and `RecipeBrowserGui` are responsible for translating when constructing `ItemMeta`.

---

### Step 1 — Recipe Key

An anvil GUI. The admin renames a paper item. The key is lowercased and spaces replaced with hyphens. Clicking **Next** validates:

- Non-empty
- Matches `^[a-z0-9-]+$`
- Not already in use in the live registry — **except** `originalKey` in edit mode

Invalid input: error item (red wool + explanation lore), screen stays open.

---

### Step 2 — Ingredients

A 3-row chest GUI. The top two rows show placed ingredients (maximum **9** unique items; the **Add Ingredient** button is hidden once 9 are placed). The bottom row controls:

- **Add Ingredient** — opens a paginated material-picker GUI (see below)
- **Clear All** — removes all placed ingredients
- **Next** — disabled until at least one ingredient is placed

**Removing an ingredient:** Shift-left-click a placed slot to remove it. All other click types on ingredient slots are cancelled.

**Uniqueness:** Ingredients are unique by `Material` type. Only plain materials are valid ingredients (no NBT, no potion metadata). If the admin selects a material already in the list, the add is a no-op and the existing slot briefly flashes (replaced with red stained glass for 10 ticks, then restored) to signal the duplicate.

**Material picker GUI:** A paginated 6-row chest showing one item per eligible `Material` value (plain item stack, no NBT). Eligible materials are those that are physical (`material.isItem()`), not `AIR`, and not legacy (`!material.isLegacy()` on API versions that expose this flag). Admin left-clicks a material to add it to the ingredient list and return to Step 2. A **Back** button returns without adding. No inventory interaction required — items are displayed as icons, not taken from the admin's inventory.

---

### Step 3 — Result Type

A 2-row chest GUI with two buttons:

- **Vanilla Potion** — opens a paginated grid of all `PotionType` values, each displayed as a potion item with `PotionMeta` applied. Clicking one stores the type as `VANILLA` + `potionType` in the session and advances to Modifiers (Step 4, vanilla path — total becomes 5 or 4 if modifiers absent).
- **Custom Potion** — stores `CUSTOM` in the session and advances to Custom Properties + Effects (Step 4, custom path — total becomes 6 or 5 if modifiers absent).

---

### Step 4 — Custom Properties + Effects *(custom path only)*

A 6-row chest GUI.

**Row 1 — Property buttons:**

| Slot | Button | Input | Stored as | Default |
|------|--------|-------|-----------|---------|
| 1 | Set Name | Anvil: rename a name tag | `String` (`&`-codes preserved) | `null` |
| 2 | Set Color | Anvil: rename a dye item | `String` (6-char hex, uppercase, no `#`) | `null` |
| 3 | Set Lore | Chat input | `List<String>` (`&`-codes preserved) | `null` |

All three optional. Omitted from `potions.yml` when null.

**Hex color validation:** Accepts exactly 6 chars matching `[0-9a-fA-F]`. Leading `#` is rejected (not stripped). Value uppercased before storage. Invalid input: error item in anvil result slot, anvil stays open.

**Lore chat input flow:**
1. Admin clicks Set Lore → GUI closes, `AsyncPlayerChatEvent` listener registered.
2. Admin receives: `"[Alchemica] Type lore lines. Send a blank message to finish."`
3. Each non-blank message appended to lore list; message cancelled (not shown in chat).
4. Blank message ends capture → listener removed → Step 4 GUI reopens with lore reflected in the Set Lore button's item lore.
5. During capture, `/` commands fire `PlayerCommandPreprocessEvent` (not `AsyncPlayerChatEvent`) and pass through automatically — no special handling needed in the chat listener. The one exception is `/brew`: its block is implemented in `BrewCommand`'s executor by checking whether the player has an active lore capture listener, returning the `"Finish entering lore first."` message without discarding the session.
6. On disconnect during capture: listener removed, session discarded.

**Rows 2–4 — Effects list:** 27 slots total (maximum **27** effects). Each slot shows: effect type name as display name; duration formatted as `Xm Ys`, amplifier as lore lines. Shift-left-click a slot to remove that effect. All other click types on effect slots cancelled.

**Row 5 — Empty / decorative padding.**

**Row 6 — Controls:**
- Slot 46: **Back**
- Slot 48: **Add Effect** (opens effect-type picker; see below)
- Slot 50: **Cancel**
- Slot 53: **Next**
- All other row-6 slots: filled with gray glass pane (non-interactive)

**Effect-type picker flow:**
1. Paginated grid of all `PotionEffectType` values. Admin clicks one.
2. Amplifier anvil opens (pre-filled `"0"`). Admin enters an integer ≥ 0 and clicks **Next**.
3. Duration anvil opens. Pre-filled tick value is resolved by: look up the effect type name (lowercase) in `effect_durations` map → get a stage key (e.g. `medium`) → look up that stage key in `duration-stages` → get the tick integer. If the effect type is absent from `effect_durations`, fall back to `600`. Admin enters an integer > 0 and clicks **Confirm**.
4. Effect is added to the session and the Step 4 GUI reopens.

---

### Step 5 — Modifiers *(both paths; skipped if `modifiers:` in `general.yml` is empty or absent)*

A chest GUI with one slot per modifier key. Each slot toggles: green stained glass pane = enabled, red stained glass pane = disabled. New recipes default all to enabled. Edited recipes load their `disabled-modifiers` list from yml.

If no modifiers are configured, this step is skipped entirely (wizard proceeds directly to Confirm).

Serialised in `general.yml` per recipe as optional `disabled-modifiers` list. Omitted when all are enabled.

**Runtime enforcement:** `RecipeRegistryFactory` must be updated to read `disabled-modifiers` when loading each recipe and store the set on the recipe object. `RecipeRegistry.search()` must skip any modifier whose key appears in the recipe's `disabled-modifiers` set when building the result. This applies to both vanilla and custom recipes.

---

### Confirm & Save *(last step)*

A summary GUI:

- Paper icon — recipe key as display name
- One item icon per ingredient
- Result potion icon — for vanilla: `PotionMeta` applied; for custom: name, color (if set), lore (if set) applied
- One slot per effect (custom only) — type/duration/amplifier in lore
- One item summarising modifier states — enabled/disabled keys in lore

Buttons:
- **Save** — writes yml, hot-reloads registry, closes GUI, sends success message
- **Back** — goes to the immediately preceding step (Modifiers if it was shown; Custom Properties + Effects if on custom path with no modifiers; Result Type if on vanilla path with no modifiers). Going back to Result Type from Confirm (vanilla, no modifiers) preserves `potionType` in the session; admin can re-select a different type or click Back again to reach Ingredients.

**On save, vanilla:** writes `general.yml` recipe entry with `potion-type`.

**On save, custom:** writes `potions.yml` definition, then writes `general.yml` recipe entry with `result: alchemica:<key>`. If `potions.yml` write fails, `general.yml` is not written and the registry is not reloaded. If `general.yml` write fails after `potions.yml` has already been written, the registry is not reloaded and the admin receives an error noting that `potions.yml` was updated but `general.yml` was not; they can run `/creload` after manually fixing `general.yml`.

---

## Recipe Browser

A paginated chest GUI, 5 rows of recipe slots + 1 controls row (up to 45 recipes per page). Each slot:

- Icon: potion item with `PotionMeta` for vanilla; potion item with `ItemMeta` (name, color, lore from `potions.yml`) for custom
- Display name: recipe key
- Lore: comma-separated ingredient material names, result type label

Controls: **Previous Page**, **Next Page**, **Back to Hub**.

Clicking a slot opens `RecipeDetailGui` — read-only, same layout as Confirm, plus:

- **Edit** — `WizardSessionFactory#fromYml(recipeKey)` populates a session with `editMode=true`, `originalKey=recipeKey`; wizard launches at Step 1.
- **Delete** — opens `RecipeDeleteConfirmGui` (Confirm Delete / Cancel). On confirm: `RecipeYmlWriter#delete` removes the entry, hot-reloads registry, returns to browser.

---

## Data Flow

**Save (new or edit, same key):**
```
RecipeYmlWriter#write(session)  [session.key == session.originalKey, or new recipe]

  For vanilla:
    1. Read general.yml; add/replace recipe entry under key
    2. Write general.yml atomically

  For custom:
    1. Read potions.yml; add/replace definition under key
    2. Write potions.yml atomically  → if fails: abort
    3. Read general.yml; add/replace recipe entry under key
    4. Write general.yml atomically  → if fails: report error (potions.yml written; admin fixes general.yml manually)
  │
  ▼
Brew#refresh()
```

**Save (edit, key renamed):**
When `session.key != session.originalKey`, the rename is implemented as a write-then-delete sequence to ensure no data is lost if a step fails:
```
RecipeYmlWriter#writeRename(session)  [session.key != session.originalKey]

  For vanilla:
    1. Read general.yml; add new entry under session.key; remove entry under originalKey
    2. Write general.yml atomically
       → if fails: abort; both keys still present as before

  For custom:
    1. Read potions.yml; add new definition under session.key
    2. Write potions.yml atomically  → if fails: abort
    3. Read general.yml; add new entry under session.key; remove entry under originalKey
    4. Write general.yml atomically
       → if fails: report error (new potions.yml definition written, new general.yml entry not written;
          admin adds the general.yml recipe entry manually, then runs /creload)
    5. Read potions.yml; remove old definition under originalKey
    6. Write potions.yml atomically
       → if fails: report warning (old definition is an orphaned but harmless entry; admin removes manually)
  │
  ▼
Brew#refresh()
```

**Note on atomicity:** True cross-file atomicity is not achievable on most filesystems. The ordered write strategy above minimises the blast radius of partial failures. On Windows, `File.renameTo()` to an existing target may not be atomic; implementations should use `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)` with a fallback to `REPLACE_EXISTING` only if `AtomicMoveNotSupportedException` is thrown.

**Edit pre-population:**
```
WizardSessionFactory#fromYml(recipeKey) → WizardSession or null
  - Read general.yml, locate recipe entry
  - If result = alchemica:<key>: read potions.yml for definition
  - Construct WizardSession (editMode=true, originalKey=recipeKey)
  - Return null if recipe not found or yml is malformed
```
`RecipeDetailGui` checks the return value: if null, sends chat error and does not launch the wizard. No exception is thrown or expected.

**Delete:**
```
RecipeYmlWriter#delete(recipeKey, isCustom)
  For vanilla:
    1. Read + remove from general.yml, write atomically

  For custom (general.yml first to avoid dangling references):
    1. Read + remove from general.yml, write atomically
       → if fails: abort, report error; potions.yml untouched
    2. Read + remove from potions.yml, write atomically
       → if fails: report error; general.yml entry already removed;
         potions.yml definition is orphaned but harmless (not reachable);
         admin can manually remove the orphaned entry from potions.yml
  │
  ▼
Brew#refresh()
```

---

## Components

| Component | Responsibility |
|-----------|---------------|
| `BrewCommand` | `/brew` executor; permission check; lore-capture guard; opens hub |
| `RecipeHubGui` | Hub — two entry buttons |
| `WizardSession` | Per-player state: `editMode`, `originalKey`, `key`, `ingredients`, `resultType`, `potionType`, `name`, `color`, `lore`, `effects`, `modifierToggles` |
| `WizardSessionFactory` | Blank session creation; yml-load for edit flows |
| `RecipeWizardGui` | All wizard steps; path branching; back/forward; step title |
| `RecipeBrowserGui` | Paginated recipe list |
| `RecipeDetailGui` | Read-only detail with Edit / Delete |
| `RecipeDeleteConfirmGui` | Delete confirmation |
| `RecipeYmlWriter` | Read + mutate + atomic-write yml files; delete entries |

All GUI classes implement `InventoryHolder`. All `InventoryClickEvent` handlers filter by `instanceof` check. All unhandled click types in all GUIs are cancelled.

---

## Error Handling

| Scenario | Behaviour |
|----------|-----------|
| Invalid anvil input | Error item (red wool + lore); admin stays on that step |
| Recipe key in use (not `originalKey` in edit mode) | Error item at Step 1 |
| `WizardSessionFactory#fromYml` — recipe not found or malformed yml | Chat error; wizard not launched |
| `potions.yml` write fails | Abort; `general.yml` not written; registry unchanged; chat error |
| `general.yml` write fails after `potions.yml` already written | Registry not reloaded; chat error with instructions to run `/creload` after fixing `general.yml` |
| `Brew#refresh()` fails | Chat error; yml already written; admin can `/creload` manually |
| Player disconnects mid-wizard | Session discarded on `PlayerQuitEvent` |
| Player disconnects during lore capture | Chat listener removed; session discarded |
| Admin opens `/brew` with active session (not lore capture) | Session discarded; hub opens with warning |
| Admin opens `/brew` during lore capture | Blocked: `"Finish entering lore first."` |

---

## yml Schema Reference

**`general.yml` — vanilla recipe:**
```yaml
recipes:
  my-weakness-potion:
    ingredients:
      - fermented_spider_eye
    potion-type: weakness
    disabled-modifiers:      # omitted if all enabled
      - lingering-potion
```

**`general.yml` — custom potion recipe:**
```yaml
recipes:
  my-custom-brew:
    ingredients:
      - nether_wart
      - blaze_rod
    result: alchemica:my-custom-brew
    disabled-modifiers:      # omitted if all enabled
      - lingering-potion
```

**`potions.yml` — custom potion definition:**
```yaml
my-custom-brew:
  name: "&6My Custom Brew"   # omitted if unset
  color: "FF4500"            # omitted if unset; uppercase, no leading #
  lore:                      # omitted if unset
    - "&7First lore line."
  effects:                   # empty list if no effects
    - type: speed
      duration: 3600
      amplifier: 1
```

**`general.yml` — `effect_durations` and `duration-stages` (existing structure, for reference):**
```yaml
duration-stages:
  instant: 1
  short: 600
  medium: 1200
  standard: 3600
  long: 9600

effect_durations:
  speed: standard       # → look up "standard" in duration-stages → 3600 ticks
  regeneration: medium  # → look up "medium" in duration-stages → 1200 ticks
  # one entry per effect type; absent effects fall back to 600 ticks
```

---

## Testing

**Unit tests:**
- `RecipeYmlWriter#write`: vanilla (all modifiers enabled — no `disabled-modifiers` field), vanilla (one modifier disabled), custom (all fields set), custom (no optional fields, empty effects list)
- `RecipeYmlWriter#write`: preserves all other existing recipes when writing one new entry
- `RecipeYmlWriter#write`: replaces existing entry on edit (same key, updated content)
- `RecipeYmlWriter#write`: key-rename edit — old key removed, new key written, no orphaned entries
- `RecipeYmlWriter#write`: simulate `potions.yml` rename failure — assert `general.yml` unchanged
- `RecipeYmlWriter#write`: simulate `general.yml` rename failure after `potions.yml` success — assert error reported (tested via mock, not file system)
- `RecipeYmlWriter#delete`: vanilla — removes from `general.yml` only
- `RecipeYmlWriter#delete`: custom — removes from both files
- `WizardSessionFactory#fromYml`: vanilla round-trip (write then load, assert fields equal, `editMode=true`, `originalKey` set)
- `WizardSessionFactory#fromYml`: custom round-trip (all fields set; all optional fields unset)
- `WizardSessionFactory#fromYml`: recipe not found → returns null
- Key validation: rejects empty; rejects special chars; rejects duplicate (new mode); accepts valid; accepts `originalKey` duplicate in edit mode
- Hex validation: rejects 5-char; rejects non-hex chars; rejects leading `#`; accepts uppercase; accepts lowercase (stored as uppercase)
- Effect duration pre-fill: two-step lookup — `effect_durations` stage key → `duration-stages` tick value; falls back to 600 for unknown effect type
- Step count: vanilla = 5, custom = 6; each decreases by 1 when modifiers step skipped

**MockBukkit integration tests:**
- `/brew` denied without `alchemica.admin`
- `/brew` opens hub with `alchemica.admin`

**Manual UAT checklist:**
- Create vanilla recipe end-to-end; verify `general.yml` entry and in-game brewing
- Create custom potion (name, color, lore, two effects, one disabled modifier); verify `potions.yml` and `general.yml` match expected schema
- Create custom potion with no optional fields; verify minimal yml (no name/color/lore fields; empty effects list present)
- Edit existing recipe: change an ingredient; verify yml updated, other recipes unchanged
- Edit existing recipe: change the recipe key to a new unique value; verify old key removed, new key written
- Edit existing recipe: attempt to change key to an already-in-use key; verify rejected at Step 1
- Delete custom recipe; verify both `general.yml` and `potions.yml` entries removed; other recipes unaffected
- Attempt `/brew` without `alchemica.admin`; verify denied
- Disconnect mid-wizard; reconnect; verify no session active
- Start lore capture; send two lore lines; send `/brew`; verify blocked with correct message; send blank line; verify lore saved and GUI reopens with lore shown in button
- Start lore capture; send a regular chat message; verify not shown in public chat; verify captured as lore
- Start lore capture; send `/say hello`; verify command executes; verify lore capture still active; send blank line to end
