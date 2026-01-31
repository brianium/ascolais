# Move Public to Resources - Implementation Plan

## Overview

Step-by-step tasks for moving public directory to standard location.

## Prerequisites

- [x] Understand Ring resource middleware behavior
- [x] Identify all file references

## Phase 1: Move Files

- [x] Create `resources/brianium/ascolais/resources/public/` directory
- [x] Move `styles.css` to new location
- [x] Remove empty `dev/resources/public/` directory

## Phase 2: Update Configuration

- [x] Update `tsain.edn` stylesheet path to `resources/public/styles.css`
- [x] Verify file watcher path `["resources/public"]` is correct (no change needed)
- [x] Verify `wrap-resource "public"` works (no change needed)

## Phase 3: Update Documentation

- [x] Update CLAUDE.md Project Structure section
- [x] Update CLAUDE.md "Component Styling Conventions" section
- [x] Update README.md Project Structure section
- [x] Update component-iterate skill documentation
- [x] Update root README.md reference

## Verification

- [x] Generate a new project from the template
- [x] Start REPL with `clj -M:dev`
- [x] Run `(dev)` then `(start)`
- [x] Open `localhost:3000/sandbox`
- [x] Edit `resources/public/styles.css` and verify hot-reload
- [x] Verify static assets served at `/styles.css`
- [x] Confirm `dev/resources/` only contains `components.edn`
