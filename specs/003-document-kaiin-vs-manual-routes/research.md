# Document Kaiin vs Manual Routes - Research

## Problem Statement

Kaiin generates routes from registry metadata and handles request/response dispatch. However, it explicitly closes connections after dispatch - it only relays dispatches to sfere targets. This is by design: kaiin is for stateless request/response patterns.

For persistent SSE connections (where the connection stays open and receives broadcasts), manual route handlers are required. These handlers return `::sfere/key` to store the connection in sfere for later broadcasts.

This distinction is a key architectural decision point that developers need to understand.

## Requirements

### Functional Requirements

1. Clear explanation of when to use kaiin routes
2. Clear explanation of when to use manual handlers
3. Code examples for both patterns

### Non-Functional Requirements

- Concise but complete documentation
- Syntactically correct examples

## Kaiin Routes - Use Cases

- Form submissions
- API endpoints that return data
- Actions that dispatch effects and close

Kaiin routes workflow:
1. Parse signals and path params from request
2. Dispatch effects via sandestin
3. Relay effects to sfere targets (if `::kaiin/target` specified)
4. Close the connection

## Manual Handlers - Use Cases

- Real-time updates
- Live dashboards
- Chat/collaboration features
- Any page that receives server-pushed updates

Manual SSE handlers workflow:
1. Return `::sfere/key` to store the connection
2. Return `::twk/fx` for initial effects to send
3. Connection stays open for broadcasts

## Documentation Content

```markdown
### Kaiin vs Manual Routes

**Use Kaiin** for stateless request/response handlers:
- Form submissions
- API endpoints that return data
- Actions that dispatch effects and close

**Use Manual Handlers** for persistent SSE connections:
- Real-time updates
- Live dashboards
- Chat/collaboration features

Example manual SSE handler:

```clojure
(defn sse-connect
  [{:keys [signals]}]
  (let [session-id (:sessionId signals)]
    {::sfere/key [:page "home" session-id]
     ::twk/fx [[::twk/patch-signals {:connected true}]]}))
```

Route definition (NOT using kaiin metadata):

```clojure
["/sse/home" {:get {:handler sse-connect}}]
```
```

## References

- Spec 002 (Layout Persistent Connection) - implementation details
- Kaiin library source
