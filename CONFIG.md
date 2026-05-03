# ⚙ RecyclePlugin — Configuration Guide

This file explains how to configure **RecyclePlugin v2.0.0**.

The plugin is almost fully config-driven (90-95%), allowing you to customize:
- GUI layout
- Recycling logic
- Rewards
- Effects
- Messages
- Update checker

---

# 🌍 Language

```yml
language: en
fallback-language: en
```

- `language` → main language file
- `fallback-language` → used if a key is missing

---

# 💬 Messages

```yml
messages:
  send-recap: true
  send-lucky-message: true
```

Controls in-game feedback:
- recap after recycling
- lucky bonus message
- all keys are linked to language files

---

# ✨ Effects (Sounds & Particles)

```yml
effects:
  success:
    sounds:
      - sound: ENTITY_PLAYER_LEVELUP
        volume: 1.0
        pitch: 1.2
```

You can configure:
- sounds per action
- particles on success

Available sections:
- `open`
- `error`
- `success`

---

# 🖥 GUI Configuration

## Title

```yml
gui:
  title-key: recycle-gui-title
  title: null
```

- `title-key` → uses lang file
- `title` → overrides lang if set

---

## Size

```yml
size: 54
```

Standard inventory size (must be multiple of 9)

---

## Deposit Slots

```yml
deposit-slots:
  from: 0
  to: 44
```

Defines where players can place items.

You can also use:

```yml
slots: [0, 1, 2]
ranges: ["0-44"]
extra: []
remove: []
```

---

## GUI Items

```yml
items:
  confirm:
    slot: 46
    material: EMERALD
    action: CONFIRM
```

Each item supports:
- `slot`
- `material`
- `name` or `name-key`
- `lore` or `lore-keys`
- `action`

---

## Available Actions

- `CONFIRM` → execute recycling
- `INFO` → preview panel
- `CLOSE_RETURN` → close and return items
- `NONE` → decorative item

---

## Dynamic Items

Some GUI items update automatically:

```yml
dynamic:
  info:
    preview-line-format: "§7- {material} x{amount}"
```

Used for:
- preview display
- stats display

---

# ♻ Recycling System

## Core Settings

```yml
recycle:
  cooldown-seconds: 0
  lucky-chance: 0.0
  lucky-multiplier: 2
```

- `cooldown-seconds` → delay between uses
- `lucky-chance` → % chance for bonus
- `lucky-multiplier` → reward multiplier

---

## Enchant XP

```yml
enchanted-xp-per-level: -1
```

- `-1` → legacy mode (flat bonus)
- any number → XP per enchant level

---

## Rarity System

```yml
default-rarity: COMMON
```

Fallback rarity if none is defined.

---

## XP Rewards

```yml
xp-rewards:
  common_xp: 10
  iron_xp: 20
```

XP is based on item rarity.

---

# 📦 Items Configuration

```yml
iron_sword:
  material: IRON_INGOT
  min: 0
  max: 2
```

- `material` → reward item
- `min` → minimum output
- `max` → maximum output

---

## Example

```yml
diamond_sword:
  material: DIAMOND
  min: 0
  max: 2
```

---

# 🧠 How It Works

1. Player opens GUI
2. Places items in deposit slots
3. Preview updates instantly
4. Confirm gives rewards

---

# ⚠ Notes

- Material names must match Bukkit enum
- Invalid materials will be ignored
- GUI slots must be valid (0–53)

---

# 🔥 Tips

✔ Use Imgur + GIFs for showcasing GUI  
✔ Customize GUI to match your server theme  
✔ Adjust XP for balance  
✔ Combine with economy plugins (future support)

---

# 💬 Support

If you encounter issues:
- Check console logs
- Verify config syntax
- Make sure materials are valid

---

Enjoy using RecyclePlugin ♻
