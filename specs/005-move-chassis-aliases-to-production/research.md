# Move Chassis Aliases to Production - Research

## Problem Statement

Currently chassis aliases are defined in `dev/src/clj/sandbox/ui.clj`:

```
dev/src/clj/sandbox/
  ui.clj               # Chassis alias definitions (dev-only)
  views.clj            # Requires ui.clj to ensure registration
```

This creates friction:
1. Components developed in the sandbox can't be used in production without manually copying the alias definitions
2. The `sandbox.ui` namespace suggests these are sandbox-specific, not production-ready
3. Developers must maintain two copies of aliases if they want both sandbox iteration and production use

## Requirements

### Functional Requirements

1. Move alias definitions to production namespace
2. Ensure aliases are registered during normal app startup
3. Update tsain configuration to point to new namespace
4. Remove sandbox directory entirely

### Non-Functional Requirements

- No change to how aliases are defined (still `defmethod c/resolve-alias`)
- Sandbox continues to work with new namespace

## Target Structure

```
src/clj/{{top/file}}/views/
  layout.clj           # Base layout (requires components)
  components.clj       # Chassis alias definitions (NEW)
```

The `sandbox/views.clj` file existed only to ensure `sandbox/ui.clj` was loaded, but since `layout.clj` now requires `components.clj`, aliases are registered through the normal application startup chain.

## Technical Notes

1. **Alias registration**: Chassis aliases are registered globally via `defmethod`. The namespace just needs to be loaded; it doesn't matter where it lives

2. **Load order**: `views/layout.clj` requiring `views/components` ensures aliases are registered before any page rendering. Chain: routes → layout → components

3. **Template interpolation**: The `{{top/ns}}` placeholder will be replaced with the actual namespace when a project is generated

## Dependency

This spec assumes spec 004 (move public to resources) is implemented first, as it references the updated stylesheet path in tsain.edn.
