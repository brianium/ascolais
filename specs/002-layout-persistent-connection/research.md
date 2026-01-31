# Layout with Persistent Connection Pattern - Research

## Problem Statement

The kaiin demo application demonstrates a pattern where:
1. A page loads and immediately opens a persistent SSE connection using `data-init`
2. The connection is stored by sfere under a key (e.g., `[:page "home" session-id]`)
3. All subsequent interactions dispatch effects that broadcast through stored connections
4. This enables real-time updates across multiple clients viewing the same page

Currently, the ascolais template's layout has:
- A hardcoded Datastar CDN URL
- A simple form that posts to `/api/greet` without a persistent connection
- No demonstration of the sfere connection pattern

## Requirements

### Functional Requirements

1. Use `twk/CDN-url` for Datastar script tag
2. Home page opens persistent SSE connection on load
3. Greet action broadcasts to all connected home page clients
4. Session identification via server-generated UUID

### Non-Functional Requirements

- Graceful degradation: form still works without SSE
- Clear separation between kaiin routes and manual SSE handlers

## Kaiin vs Manual Routes

**Kaiin routes** (request/response pattern):
1. Parse signals and path params from request
2. Dispatch effects via sandestin
3. Relay effects to sfere targets (if `::kaiin/target` specified)
4. **Close the connection**

**Manual SSE handlers** (persistent connection):
1. Return `::sfere/key` to store the connection
2. Return `::twk/fx` for initial effects to send
3. **Connection stays open for broadcasts**

This is why persistent SSE routes MUST be manual handlers.

## Connection Pattern Details

```clojure
;; Manual SSE handler
(defn sse-home
  [{:keys [signals]}]
  (let [session-id (:sessionId signals)]
    {::sfere/key [:page "home" session-id]
     ::twk/fx [[::twk/patch-signals {:connected true}]]}))

;; Route definition (NOT using kaiin metadata)
["/sse/home" {:get {:handler sse-home}}]

;; Broadcast pattern matches all home page connections
[:* [:page "home" :*]]
```

## References

- Kaiin library documentation
- Sfere connection management
- Datastar SSE documentation
