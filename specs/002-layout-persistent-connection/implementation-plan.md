# Layout with Persistent Connection Pattern - Implementation Plan

## Overview

Step-by-step implementation for adding persistent SSE connection pattern to the template.

## Prerequisites

- [x] Understand kaiin vs manual route distinction
- [x] Understand sfere connection storage

## Phase 1: Datastar CDN URL

- [x] Add `[ascolais.twk :as twk]` require to layout.clj
- [x] Replace hardcoded CDN URL with `twk/CDN-url`

## Phase 2: Home Page Connection

- [x] Generate server-side session ID with `(random-uuid)`
- [x] Add `data-signals` with name and sessionId
- [x] Add `data-init` to open SSE connection on load
- [x] Update form to use `data-on:submit__prevent`

## Phase 3: SSE Route Handler

- [x] Create `sse-home` handler in routes.clj
- [x] Return `::sfere/key` for connection storage
- [x] Return initial `::twk/fx` for connected signal
- [x] Add manual route `["/sse/home" {:get {:handler sse-home}}]`

## Phase 4: Broadcast Greet Action

- [x] Update `::greet` action to use `::sfere/broadcast`
- [x] Use pattern `[:* [:page "home" :*]]` to match all home connections
- [x] Add sfere require to fx/example.clj

## Verification

- [x] Generate a new project from the template
- [x] Start the dev server
- [x] Open home page in two browser tabs
- [x] Enter a name and submit in one tab
- [x] Both tabs show the greeting (broadcast working)
- [x] Check Network tab - SSE connection to `/sse/home` visible
