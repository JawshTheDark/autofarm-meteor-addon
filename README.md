# AutoFarm Meteor Addon

A small Meteor Client addon that automates basic crop farming and planting. Includes two modules designed to work well in survival worlds and village farms.

Author: **j_wsh**

---

## Modules

### 🌾 AutoFarm

Automatically harvests fully-grown crops around you and replants them.

Features:

- Detects fully-grown:
    - Wheat
    - Carrots
    - Potatoes
    - Beetroot
    - Nether wart
- Replants using matching seeds/items (if available in your hotbar)
- Optional seed safety:
    - `stop-when-low-seeds` + `min-seeds` to avoid running out
- Inventory safety:
    - Optional `stop-when-full` so you don't overflow drops
- Crop filter:
    - `Off` – farm everything
    - `Whitelist` – only farm selected crops
    - `Blacklist` – farm everything except selected crops
- Activation modes:
    - `Always`
    - `WhileSneaking`
    - `WhileNotSneaking`
- Render:
    - ESP boxes around crops that will be harvested
- Requires a hoe **somewhere in your hotbar** (doesn't force your selected slot)

### 🌱 AutoPlanter

Automatically plants crops on farmland and soul sand, or can exclusively feed composters for bonemeal.

Two main modes:

- **PlantCrops**
    - Finds air blocks above:
        - Farmland → plants wheat/carrot/potato/beetroot based on what you have
        - Soul sand → plants nether wart
    - Respects the same crop filter (whitelist/blacklist) style
    - Does **not** interact with composters in this mode, so village farms work fine

- **CompostersOnly**
    - Ignores farmland completely
    - Scans for composters in range and repeatedly right-clicks them with allowed items from your hotbar
    - Great for turning extra seeds/crops into bonemeal

General options:

- `range` – horizontal range around the player
- `actions-per-tick` – how aggressive planting / composter feeding should be
- `activation-mode` – same as AutoFarm (always / while sneaking / while not sneaking)
- Shared crop filter for what can be planted or fed into composters

---

## Requirements


- Minecraft: `1.21.10` 
- Fabric Loader: `0.17.3`
- Meteor Client: `1.21.10 15` 
- Java: **21**

---

## Installation

1. Install Fabric + Meteor Client for your Minecraft version.
2. Download the latest `autofarm-*.jar` from the Releases page.
3. Put the jar into your `.minecraft/mods` folder.
4. Launch Minecraft with Fabric + Meteor.

---

## Usage

1. Open the Meteor GUI (default: **Right Shift**).
2. Go to the **Player** category (or wherever your modules show up).
3. Enable:
    - `autofarm` to harvest and replant
    - `autoplanter` to plant crops and/or feed composters

### AutoFarm tips

- Keep a hoe anywhere in your **hotbar**.
- Keep seeds/crops you want to replant in your inventory (hotbar works best).
- Tweak:
    - `range` for larger farm patches
    - `blocks-per-tick` and delays for performance vs. speed
    - Crop filter if you only want certain crops touched

### AutoPlanter tips

- **PlantCrops** mode:
    - Works well in villages and custom farms
    - Put different seeds/crops in your hotbar; it picks suitable ones for each soil
- **CompostersOnly** mode:
    - Stand near your composter setup
    - Put “trash” seeds/crops in your hotbar
    - AutoPlanter will feed composters up to `actions-per-tick` per tick

---

## Known limitations

- Both modules operate in a configurable radius around the player; they **don’t pathfind**.
- Some servers may have anticheat that dislikes very high `actions-per-tick` settings.

---

## License

Choose one and add the actual `LICENSE` file (e.g. MIT, LGPL, etc.).

Example (MIT):

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.
