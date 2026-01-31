---
title: "Unify Router Configuration"
status: completed
date: 2024-01-01
priority: 60
---

# Unify Router Configuration

## Overview

Refactor the router configuration so that the dev router extends the production router through parameters rather than duplicating the implementation. This eliminates the separate `router` function in dev/config.clj.

## Goals

- Eliminate middleware stack duplication between prod and dev
- Add static file serving to production router
- Make the dev-extends-prod relationship explicit
- Reduce maintenance burden

## Non-Goals

- Changing routing behavior
- Modifying how reitit works

## Key Decisions

See [research.md](research.md) for background details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Extension mechanism | `:extra-routes` parameter | Simple, explicit, no magic |
| Static files in prod | Yes, via wrap-resource | Standard web app pattern |
| Route order | extra-routes first | Allows dev routes to shadow prod if needed |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Update production router
- [x] Phase 2: Simplify dev config
- [x] Phase 3: Update documentation
