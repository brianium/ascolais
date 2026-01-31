---
title: "Move Chassis Aliases to Production"
status: completed
date: 2024-01-01
priority: 50
---

# Move Chassis Aliases to Production

## Overview

Move chassis alias definitions from the dev-only `sandbox.ui` namespace to a production namespace `{{top/ns}}.views.components`. This allows components developed in the sandbox to be used directly in production without copying code.

## Goals

- Enable components developed in sandbox to be used in production
- Eliminate need to maintain two copies of alias definitions
- Make component namespace naming clearer (not "sandbox-specific")

## Non-Goals

- Changing how chassis aliases work
- Moving components.edn (stays in dev/resources)

## Key Decisions

See [research.md](research.md) for background details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| New namespace | views/components.clj | Follows views namespace convention |
| Load chain | routes → layout → components | Automatic via normal app startup |
| Remove sandbox/ | Yes | No longer needed |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Create production components namespace
- [x] Phase 2: Update tsain configuration
- [x] Phase 3: Remove sandbox directory
- [x] Phase 4: Update documentation
