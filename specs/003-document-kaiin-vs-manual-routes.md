# 003: Document Kaiin vs Manual Routes

**Status:** Complete

## Summary

Update the templated CLAUDE.md to clarify when to use kaiin-generated routes vs manual route handlers, specifically around persistent SSE connections.

## Background

Kaiin generates routes from registry metadata and handles request/response dispatch. However, it explicitly closes connections after dispatch - it only relays dispatches to sfere targets. This is by design: kaiin is for stateless request/response patterns.

For persistent SSE connections (where the connection stays open and receives broadcasts), manual route handlers are required. These handlers return `::sfere/key` to store the connection in sfere for later broadcasts.

This distinction is a key architectural decision point that developers need to understand.

## Changes Required

### Update CLAUDE.md Kaiin Section

**File:** `resources/brianium/ascolais/build/CLAUDE.md`

Add a new section after the existing Kaiin documentation:

```markdown
### Kaiin vs Manual Routes

**Use Kaiin** for stateless request/response handlers:
- Form submissions
- API endpoints that return data
- Actions that dispatch effects and close

Kaiin routes:
1. Parse signals and path params from request
2. Dispatch effects via sandestin
3. Relay effects to sfere targets (if `::kaiin/target` specified)
4. Close the connection

**Use Manual Handlers** for persistent SSE connections:
- Real-time updates
- Live dashboards
- Chat/collaboration features
- Any page that receives server-pushed updates

Manual SSE handlers:
1. Return `::sfere/key` to store the connection
2. Return `::twk/fx` for initial effects to send
3. Connection stays open for broadcasts

Example manual SSE handler:

```clojure
(defn sse-connect
  "Establish persistent SSE connection."
  [{:keys [signals]}]
  (let [session-id (:sessionId signals)]
    {::sfere/key [:page "home" session-id]
     ::twk/fx [[::twk/patch-signals {:connected true}]]}))
```

Route definition (NOT using kaiin metadata):

```clojure
["/sse/home" {:get {:handler sse-connect}}]
```

Kaiin's `::kaiin/target` broadcasts to stored connections but does NOT keep the originating connection open.
```

## Implementation Notes

1. Place this section in CLAUDE.md after the existing Kaiin routing section
2. Keep the explanation concise but clear on the key distinction
3. Include a practical example showing the handler pattern

## Verification

After changes:
1. Generate a new project from the template
2. Review CLAUDE.md for clarity
3. Verify the example code is syntactically correct
