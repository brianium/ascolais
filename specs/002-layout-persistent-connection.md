# 002: Layout with Persistent Connection Pattern

**Status:** Draft

## Summary

Update the templated `{{top/ns}}.views.layout` namespace to demonstrate the persistent SSE connection pattern. Pages should open a persistent connection on load via `data-init`, and all dispatches should flow through that connection. The Datastar script tag should use `ascolais.twk/CDN-url` instead of a hardcoded URL.

## Background

The kaiin demo application demonstrates a pattern where:
1. A page loads and immediately opens a persistent SSE connection using `data-init`
2. The connection is stored by sfere under a key (e.g., `[:page "home" session-id]`)
3. All subsequent interactions dispatch effects that broadcast through stored connections
4. This enables real-time updates across multiple clients viewing the same page

Currently, the ascolais template's layout has:
- A hardcoded Datastar CDN URL
- A simple form that posts to `/api/greet` without a persistent connection
- No demonstration of the sfere connection pattern

## Changes Required

### 1. Use `twk/CDN-url` for Datastar Script

**File:** `resources/brianium/ascolais/src/clj/views/layout.clj`

Replace:
```clojure
[:script {:type "module"
          :src "https://cdn.jsdelivr.net/npm/@starfederation/datastar@1.0.0-rc.2/dist/datastar.min.js"}]
```

With:
```clojure
[:script {:type "module" :src twk/CDN-url}]
```

Add require:
```clojure
[ascolais.twk :as twk]
```

### 2. Add Persistent Connection on Home Page

Update `home-page` to generate a server-side session ID and open an SSE connection on load:

```clojure
(defn home-page
  "Home page view."
  []
  (let [session-id (str (random-uuid))]
    (base-layout {:title "{{name}}"}
      [:div {:data-signals (str "{name: '', sessionId: '" session-id "'}")
             :data-init "@get('/sse/home')"}
       [:h1 "Welcome to {{name}}"]
       [:p "A Clojure web application powered by the sandestin effect ecosystem."]

       [:form {:data-on:submit__prevent "@post('/api/greet')"}
        [:input {:type "text"
                 :placeholder "Enter your name"
                 :data-bind:name true}]
        [:button {:type "submit"} "Greet"]]

       [:div#greeting]])))
```

### 3. Add SSE Connection Route (Manual Handler)

**Important:** Kaiin is NOT used for persistent SSE connections. Kaiin explicitly closes connections after dispatch - it only relays dispatches to targets via sfere. Persistent SSE routes must be regular handlers.

**File:** `resources/brianium/ascolais/src/clj/routes.clj`

Add a manual route for the SSE connection:

```clojure
(defn sse-home
  "Establish persistent SSE connection for home page."
  [{:keys [signals]}]
  (let [session-id (:sessionId signals)]
    {::sfere/key [:page "home" session-id]
     ::twk/fx [[::twk/patch-signals {:connected true}]]}))

(defn routes []
  [["/sse/home" {:get {:handler sse-home}}]
   ;; ... other routes
   ])
```

### 4. Update Greet Action to Broadcast

Modify the `::greet` action to broadcast through stored connections:

```clojure
::greet
{::s/description "Greet a user by name."
 ::s/schema [:tuple [:= ::greet] :string]
 ::s/handler
 (fn [_state name]
   ;; Broadcast greeting to all home page connections
   [[::sfere/broadcast {:pattern [:* [:page "home" :*]]}
     [::twk/patch-elements
      [:div#greeting.greeting
       [:h2 "Hello, " name "!"]
       [:p "Welcome to {{name}}"]]]]])

 ::kaiin/path "/api/greet"
 ::kaiin/method :post
 ::kaiin/signals [:map [:name :string]]
 ::kaiin/dispatch [::greet [::kaiin/signal :name]]}
```

### 5. Add sfere Require to Example Registry

**File:** `resources/brianium/ascolais/src/clj/fx/example.clj`

Add to requires:
```clojure
[ascolais.sfere :as sfere]
```

## Implementation Notes

1. **Session identification**: Generate a unique session ID server-side using `(random-uuid)` when rendering the page. This ID is embedded in the HTML and sent with the SSE connection request.

2. **Kaiin vs manual routes**: Kaiin is for request/response handlers that close after dispatch. It relays dispatches to sfere targets but does not keep connections open. Persistent SSE routes MUST be manual handlers that return `::sfere/key` to store the connection.

3. **Connection patterns**: The pattern `[:* [:page "home" :*]]` matches all connections to the home page across all scopes and sessions.

4. **Graceful degradation**: The page still works without an SSE connection - the form submission works independently. The persistent connection adds real-time broadcast capability.

5. **About page**: Leave the about page simple (no SSE) to show contrast between static and connected pages.

## Verification

After changes:
1. Generate a new project from the template
2. Start the dev server
3. Open home page in two browser tabs
4. Enter a name and submit in one tab
5. Both tabs should show the greeting (broadcast working)
6. Check browser dev tools Network tab - should see SSE connection to `/sse/home`
