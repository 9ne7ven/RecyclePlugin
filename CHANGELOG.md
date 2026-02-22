# Changelog

All notable changes to this project will be documented in this file.

This project follows semantic versioning.

---

## [1.1.0] - 2025-01-XX

### Added
- Robust language fallback system
- Automatic configuration correction for invalid language values
- `activeLanguage` tracking for accurate console reporting
- Styled console logs (startup, reload, shutdown)
- Permission validation for `/recycle`
- Protection against recycling unregistered items
- Improved null-safety in inventory handling

### Improved
- Overall stability and production readiness
- Console clarity and structured logging
- Reload behavior consistency

### Technical
- Refactored language loading logic
- Secured configuration handling
- Enhanced error resilience