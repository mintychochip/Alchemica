# Brew (Alchemica)

A custom potion brewing plugin for Minecraft Paper/Spigot servers. Create custom potions using cauldrons with a flexible recipe system.

## Features

- **Cauldron-based brewing** - Add ingredients to water cauldrons, stir, and collect custom potions
- **Flexible recipe system** - Define potion recipes in YAML with parent/child relationships
- **Multiple potion types** - Base potions, custom effects, and modifiers (extended, amplified, splash, lingering)
- **Multi-version support** - Works on 1.12.2 through 1.21.4
- **SQLite storage** - Persistent cauldron state across restarts

## Installation

1. Download the latest release from [Releases](https://github.com/mintychochip/Alchemica/releases)
2. Place `Brew.jar` in your server's `plugins/` folder
3. Start the server - config files will be generated automatically
4. Customize `general.yml` to add your own recipes

## How to Use

### Brewing Potions

1. **Place a water cauldron** and fill it with water
2. **Add ingredients** by right-clicking the cauldron with items
3. **Stir** with a blaze rod or stick to complete the brew
4. **Collect** by right-clicking with a glass bottle

### Example: Speed Potion

```
1. Place water cauldron
2. Add Nether Wart → Awkward base
3. Add Sugar → Speed base
4. Add Sugar again → Speed effect
5. Stir with blaze rod
6. Collect with glass bottle → Speed Potion!
```

## Configuration

### general.yml

Defines potion recipes using a node-based system:

```yaml
nodes:
  awkward-potion:
    type: base
    item: nether_wart
    potion-type: awkward
    parents:
      - water-potion

  speed-potion:
    type: base
    item: sugar
    potion-type: swiftness
    parents:
      - awkward-potion

  speed-effect:
    type: effect
    item: sugar
    effect: speed
    parents:
      - effect[.*]
      - base[^(?!water|awkward|thick|mundane).*$]

  lesser-extension:
    type: modifier
    item: redstone
    modifiers:
      duration: 1
    parents:
      - effect[.*]
```

### Node Types

| Type | Description |
|------|-------------|
| `base` | Creates vanilla potion bases (awkward, thick, etc.) |
| `effect` | Adds status effects to potions |
| `modifier` | Modifies duration, amplifier, or potion type |

### database.yml

```yaml
type: sqlite  # or mysql
username: ""
password: ""
url: ""
```

## Commands

| Command | Description |
|---------|-------------|
| `/creload` | Hot-reload all configuration files |

## Permissions

| Permission | Description |
|------------|-------------|
| `brew.reload` | Access to /creload command |

## Supported Versions

| Version | Support |
|---------|---------|
| 1.12.2 | ✅ |
| 1.13.2 - 1.20.4 | ✅ |
| 1.21.x | ✅ |

## Building

```bash
./gradlew shadowJar
```

Output: `build/libs/Brew-1.0.4-all.jar`

## API

The plugin exposes an API for other plugins to integrate:

```java
// Get the Brew instance
IBrew brew = BrewBootstrap.getInstance();

// Access internal APIs
Internal internal = ((Brew) brew).getInternal();
```

## License

MIT License - see [LICENSE](LICENSE)
