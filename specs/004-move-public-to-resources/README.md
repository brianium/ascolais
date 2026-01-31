---
title: "Move Public to Resources"
status: completed
date: 2024-01-01
priority: 40
---

# Move Public to Resources

## Overview

Move the `dev/resources/public/` directory (containing `styles.css`) to the template's root `resources/public/` directory. This consolidates static assets in the standard location and fixes the file watcher path mismatch.

## Goals

- Consolidate static assets in standard `resources/public/` location
- Fix file watcher path mismatch
- Separate dev-only config from production assets

## Non-Goals

- Moving `components.edn` (stays in dev/resources as dev-only)
- Changing how static files are served

## Key Decisions

See [research.md](research.md) for background details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| CSS location | resources/public/ | Standard Ring middleware path |
| components.edn | Keep in dev/resources | Dev-only configuration |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Move files
- [x] Phase 2: Update configuration paths
- [x] Phase 3: Update documentation
