# Remove Redundant init-key Defmethods - Implementation Plan

## Overview

Step-by-step implementation tasks for removing redundant init-key defmethods.

## Prerequisites

- [x] Understand Integrant's initializer function feature
- [x] Identify all affected files

## Phase 1: Production Config

- [x] Remove init-key defmethods from config.clj (::datasource, ::store, ::dispatch, ::router, ::handler, ::server)
- [x] Keep halt-key! defmethods (::datasource, ::server)
- [x] Rename section header to "Integrant Halt Methods"

## Phase 2: Dev Config

- [x] Rename `dev-router` function to `router`
- [x] Remove init-key defmethods (::tsain-registry, ::file-watcher, ::router)
- [x] Keep halt-key! defmethod for ::file-watcher

## Phase 3: Example Registry

- [x] Remove init-key defmethod for ::registry from fx/example.clj
- [x] Remove empty Integrant Methods section if no halt-key! remains

## Phase 4: Verification

- [x] Start a generated project's REPL
- [x] Run `(start)` - system should initialize all components
- [x] Run `(restart)` - system should halt and reinitialize cleanly
- [x] Verify datasource closes properly (no connection pool warnings)
- [x] Verify server stops properly (port released)
