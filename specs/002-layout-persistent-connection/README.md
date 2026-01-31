---
title: "Layout with Persistent Connection Pattern"
status: completed
date: 2024-01-01
priority: 20
---

# Layout with Persistent Connection Pattern

## Overview

Update the templated `{{top/ns}}.views.layout` namespace to demonstrate the persistent SSE connection pattern. Pages should open a persistent connection on load via `data-init`, and all dispatches should flow through that connection.

## Goals

- Demonstrate persistent SSE connection pattern in the template
- Use `ascolais.twk/CDN-url` for Datastar script instead of hardcoded URL
- Show how sfere stores connections for real-time broadcasts
- Enable real-time updates across multiple clients viewing the same page

## Non-Goals

- Making all pages use persistent connections (about page stays simple)
- Changing the kaiin library behavior

## Key Decisions

See [research.md](research.md) for background details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| SSE route type | Manual handler | Kaiin closes connections; persistent SSE needs manual handlers |
| Session ID generation | Server-side UUID | Unique per page load, embedded in HTML |
| Connection key pattern | `[:page "home" session-id]` | Enables scoped broadcasts |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Update Datastar CDN URL
- [x] Phase 2: Add persistent connection to home page
- [x] Phase 3: Add SSE route handler
- [x] Phase 4: Update greet action to broadcast
