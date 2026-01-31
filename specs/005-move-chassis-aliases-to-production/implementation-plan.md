# Move Chassis Aliases to Production - Implementation Plan

## Overview

Step-by-step tasks for moving chassis aliases from dev to production namespace.

## Prerequisites

- [x] Spec 004 implemented (stylesheet path in tsain.edn)

## Phase 1: Create Production Namespace

- [x] Create `src/clj/views/components.clj` with chassis require
- [x] Move example-card alias definition
- [x] Add docstring explaining component definition pattern

## Phase 2: Update Layout

- [x] Add `[{{top/ns}}.views.components]` require to layout.clj
- [x] Verify alias registration chain works

## Phase 3: Update Tsain Configuration

- [x] Change `:ui-namespace` to `{{top/ns}}.views.components`
- [x] Update `components.edn` to use new namespace in hiccup

## Phase 4: Remove Sandbox Directory

- [x] Delete `dev/src/clj/sandbox/ui.clj`
- [x] Delete `dev/src/clj/sandbox/views.clj`
- [x] Remove `sandbox/` directory

## Phase 5: Update Documentation

- [x] Update CLAUDE.md Project Structure
- [x] Update CLAUDE.md "Chassis Alias Conventions" section
- [x] Update README.md Project Structure
- [x] Update component-iterate skill references

## Verification

- [x] Generate a new project from the template
- [x] Start REPL with `clj -M:dev`
- [x] Run `(dev)` then `(start)`
- [x] Open `localhost:3000/sandbox`
- [x] Preview component with new namespace
- [x] Add new alias to views/components.clj
- [x] Run `(reload)` and verify alias available
- [x] Use alias in production view and verify rendering
