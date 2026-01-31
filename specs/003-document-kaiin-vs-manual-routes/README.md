---
title: "Document Kaiin vs Manual Routes"
status: completed
date: 2024-01-01
priority: 30
---

# Document Kaiin vs Manual Routes

## Overview

Update the templated CLAUDE.md to clarify when to use kaiin-generated routes vs manual route handlers, specifically around persistent SSE connections.

## Goals

- Document the key architectural distinction between kaiin and manual routes
- Provide clear guidance for when to use each approach
- Include practical code examples

## Non-Goals

- Changing kaiin's behavior
- Adding new features to the template

## Key Decisions

See [research.md](research.md) for background details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Documentation location | CLAUDE.md | Primary reference for AI-assisted development |
| Example type | SSE connection | Most common use case for manual handlers |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Write documentation section
- [x] Phase 2: Add code examples
- [x] Phase 3: Integration into CLAUDE.md
