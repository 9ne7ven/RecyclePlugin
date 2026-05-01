# Changelog

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
