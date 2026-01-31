# Unify Router Configuration - Implementation Plan

## Overview

Step-by-step tasks for unifying router configuration between prod and dev.

## Prerequisites

- [x] Understand current router duplication
- [x] Identify all affected requires

## Phase 1: Update Production Router

- [x] Add `wrap-resource` require to config.clj
- [x] Add `:extra-routes` parameter to router function
- [x] Add `wrap-resource "public"` to middleware stack
- [x] Update docstring to document options

## Phase 2: Simplify Dev Config

- [x] Remove router function from dev/config.clj
- [x] Remove unnecessary requires (reitit, ring middleware, twk, ds-hk)
- [x] Update config map to use `::app/router` directly
- [x] Pass `(tsain/routes)` as `:extra-routes`

## Phase 3: Update Documentation

- [x] Add "Router Extension" section to CLAUDE.md
- [x] Document the `:extra-routes` pattern

## Verification

- [x] Generate new project from template
- [x] **Test production mode**:
  - [x] Start without dev alias
  - [x] Verify static files served
  - [x] Verify routes work
- [x] **Test dev mode**:
  - [x] Start with dev alias
  - [x] Verify `/sandbox` works
  - [x] Verify static files work
  - [x] Verify hot-reload works
- [x] Confirm dev/config.clj has no router-related requires
