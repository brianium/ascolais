# Specifications

This directory contains living specifications for ascolais features and concepts.

## Current Priorities

| Priority | Spec | Reason |
|----------|------|--------|
| 1 | [004-move-public-to-resources](./004-move-public-to-resources.md) | Fixes file watcher path mismatch, consolidates assets in standard location |
| 2 | [005-move-chassis-aliases-to-production](./005-move-chassis-aliases-to-production.md) | Enables sandbox components to be used in production (depends on 004) |
| 3 | [006-unify-router-configuration](./006-unify-router-configuration.md) | Eliminates router duplication, adds static file serving to production |

## Spec Index

| Spec | Status | Description |
|------|--------|-------------|
| [001-remove-redundant-init-key](./001-remove-redundant-init-key.md) | Complete | Remove redundant init-key defmethods in favor of Integrant initializer functions |
| [002-layout-persistent-connection](./002-layout-persistent-connection.md) | Draft | Update layout to use twk/CDN-url and demonstrate persistent SSE connection pattern |
| [003-document-kaiin-vs-manual-routes](./003-document-kaiin-vs-manual-routes.md) | Draft | Document when to use kaiin routes vs manual handlers for persistent SSE |
| [004-move-public-to-resources](./004-move-public-to-resources.md) | Active | Move dev/resources/public to resources/public for standard asset location |
| [005-move-chassis-aliases-to-production](./005-move-chassis-aliases-to-production.md) | Draft | Move chassis aliases from sandbox.ui to production namespace |
| [006-unify-router-configuration](./006-unify-router-configuration.md) | Draft | Unify router config so dev extends production via :extra-routes parameter |

Status values: Draft, Active, Complete, Archived
