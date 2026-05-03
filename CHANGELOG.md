# Changelog

---

# ♻ RecyclePlugin — v2.0.0

## 🚀 MAJOR UPDATE — COMPLETE REWORK

This update is a complete rewrite of RecyclePlugin.

From a simple recycling system, it is now a scalable, modular and highly configurable plugin designed for real server environments.

---

## 🧠 Core Rewrite

- ✨ Full internal refactor
  - Modular architecture (managers, services, GUI system)
  - Clean separation of responsibilities
  - Future-proof structure

- ⚙ Config-driven system
  - GUI layout configurable
  - Slots fully configurable
  - Actions configurable
  - Recycling system driven by config.yml

- 📦 Stable and predictable system
  - Fixed inconsistent reward calculations
  - Removed random mismatches between preview and confirm
  - Deterministic output calculation

---

## 🖥 GUI Overhaul

- 🎨 Fully customizable GUI
  - Materials, slots, titles, lore
  - Layout controlled via config

- ⚡ Real-time preview system
  - Accurate reward estimation
  - No more desync issues

- 🧩 Improved interactions
  - Better click handling
  - Safe drag behavior
  - Cleaner inventory management

---

## 🧪 Bug Fixes

- 🐛 Fixed reward inconsistencies when modifying items
- 🐛 Fixed preview vs confirm mismatch
- 🐛 Fixed mixed-item calculation issues
- 🐛 Fixed durability-based edge cases

---

## 🌍 Language System

- Auto-sync missing keys
- Improved fallback handling
- Cleaner multi-language structure

---

## ⚡ Performance

- Reduced unnecessary recalculations
- Optimized GUI refresh
- Cleaner memory usage

---

## 🛠 Developer Notes

- Cleaner codebase
- Easier to maintain and extend
- Ready for API & economy integration

---

# 🔮 ROADMAP

## 🟡 v2.1.0 — Economy & API Update

- 💰 Vault integration (money rewards)
- ⚖ Reward types system:
  - XP
  - Money
  - Materials
  - Combo modes

- 🔌 Public API:
  - Recycle preview
  - Recycle execution
  - External plugin hooks

- 📡 Events:
  - RecycleEvent
  - RecyclePreviewEvent

---

## 🟠 v2.2.0 — Advanced GUI Engine

- 🧩 Dynamic GUI system (true action registry)
- 🎯 Fully configurable behaviors:
  - shift-click
  - drag
  - close behavior

- 🎨 GUI animations
- 🔄 Pagination support
- 📊 Advanced preview display

---

## 🔵 v2.3.0 — Effects & Immersion

- ✨ Configurable:
  - sounds
  - particles
  - visual feedback

- 🎬 Animation system (open / confirm)
- 🔊 Per-action feedback

---

## 🟣 v2.4.0 — Progression System

- 📈 Recycling levels
- 🧠 XP scaling
- 🎁 Bonus rewards
- ⚡ Perks system

---

## 🔴 v3.0.0 — Network & Expansion

- 🌐 Multi-server compatibility
- 🔗 Integration with economy / RPG plugins
- 🏪 Shop + recycle synergy
- 🧬 Fully extensible API

---

# 💬 Final Note

RecyclePlugin is no longer just a utility plugin.

It is evolving into a complete, configurable recycling system ready for large-scale servers.

---

🔥 Thank you for your support

---

## [1.2.0] - 01-05-2026 - Special thanks to mundodrix/wpmidia for lang contribution.

### Added
- Full 54-slot recycling GUI
- Expanded deposit area (0–44)
- Dedicated control bar (confirm, preview, stats, close)
- Shift-click support for fast item deposit
- Live preview system (materials + XP)
- Configurable cooldown system
- Support for both config formats:
  - recycle.items (new)
  - items (legacy)
- Flexible material resolver
  - Supports multiple formats (diamond_sword, diamond sword, etc.)
  - Includes common alias handling
- Automatic language file generation
- Automatic missing language key synchronization on reload

### Improved
- Recycling logic (better durability-based reward scaling)
- XP calculation consistency
- Inventory handling and stack merging
- Player feedback (messages, sounds, preview clarity)
- Error handling for invalid config entries
- GUI interaction flow and responsiveness

### Fixed
- Missing language key issues in GUI
- GUI displaying raw keys instead of messages
- Shift-click duplication edge cases
- Material parsing inconsistencies
- Unsafe usage of merge(..., Integer::sum) (null-safety warnings)

### Changed
- GUI layout redesigned from legacy format to modern 54-slot system
- Close button behavior simplified (no external menu dependency)
- Language keys standardized (recycle-* format)

### Technical
- Improved null-safety across inventory and data handling
- Refactored internal logic for better maintainability
- Cleaner separation between config, lang, and gameplay logic
- Reduced risk of runtime errors and invalid states

### Summary
This update significantly improves usability, stability, and extensibility.
