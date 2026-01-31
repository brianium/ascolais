---
title: "Remove Redundant init-key Defmethods"
status: completed
date: 2024-01-01
priority: 10
---

# Remove Redundant init-key Defmethods

## Overview

Remove redundant `defmethod ig/init-key` implementations that merely delegate to same-named initializer functions. Integrant's [initializer functions](https://github.com/weavejester/integrant?tab=readme-ov-file#initializer-functions) feature automatically discovers and uses functions matching the keyword name, making the explicit defmethods unnecessary.

## Goals

- Eliminate redundant boilerplate code
- Leverage Integrant's built-in initializer function discovery
- Simplify the codebase without changing behavior

## Non-Goals

- Removing halt-key! defmethods (these are still necessary)
- Changing how Integrant is initialized

## Key Decisions

See [research.md](research.md) for background details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Keep halt-key! | Yes | Required for proper cleanup of resources |
| Rename dev-router | To `router` | Must match the `::router` key for auto-discovery |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Analyze affected files
- [x] Phase 2: Remove redundant init-key defmethods
- [x] Phase 3: Verification
