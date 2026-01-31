# {{top/ns}}/{{main}}

## Project Overview

This is a Clojure web application powered by the sandestin effect dispatch ecosystem with Datastar for reactive frontend components.

### Key Libraries

| Library | Purpose | Provides |
|---------|---------|----------|
| **sandestin** | Effect dispatch with schema-driven discoverability | `s/create-dispatch`, discovery API |
| **twk** | Datastar SSE integration | `twk/registry`, `twk/with-datastar` middleware |
| **sfere** | Connection management and broadcasting | `sfere/registry`, `sfere/store` |
| **manse** | Database effects with next.jdbc | `manse/registry`, execute effects |
| **tsain** | Component development sandbox | `tsain/registry`, preview effects |
| **html.yeah** | Schema-validated chassis alias elements | `hy/defelem`, discovery API |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                            │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    twk/with-datastar middleware                 │
│  - Parses Datastar signals from headers                         │
│  - Dispatches ::twk/fx effects via sandestin                    │
│  - Returns SSE responses                                        │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      sandestin dispatch                         │
│  - Interpolates placeholders                                    │
│  - Expands actions → effect vectors                             │
│  - Executes effects with interceptors                           │
└──────────────────────────────┬──────────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
    ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
    │  twk effects │   │ sfere effects│   │  app effects │
    │ patch-elements   │ broadcast    │   │  manse/db    │
    │ patch-signals    │ with-connection  │              │
    └──────────────┘   └──────────────┘   └──────────────┘
```

### Router Extension

The production router accepts an `:extra-routes` parameter, allowing dev to extend it without duplication:

```clojure
;; Production
::router {:dispatch (ig/ref ::dispatch)
          :routes (routes/routes)}

;; Dev adds tsain sandbox routes via component
::tsain-routes {:dispatch (ig/ref ::app/dispatch)
                :tsain-registry (ig/ref ::tsain-registry)}

::app/router {:dispatch (ig/ref ::app/dispatch)
              :routes (routes/routes)
              :extra-routes (ig/ref ::tsain-routes)}
```

## Technology Stack

- **Clojure 1.12** with deps.edn
- **Integrant** for system lifecycle
- **clj-reload** for namespace reloading
- **Portal** for data inspection (tap>)
- **PostgreSQL** with HikariCP connection pool
- **charred** for JSON parsing/writing
- **Cognitect test-runner** for tests

---

## Integrant Patterns

### Initializer Functions (Preferred)

For simple component initialization, define a function that takes a config map. Integrant infers the initializer from the namespaced key:

```clojure
;; In {{top/ns}}/example.clj
(defn client
  "Create an API client. Integrant calls this via ::client key."
  [{:keys [api-key]}]
  (if api-key
    (-> (SomeClient/builder) (.apiKey api-key) (.build))
    (SomeClient/fromEnv)))

;; In config.clj - no defmethod needed
{::example/client {:api-key (get secrets "EXAMPLE_API_KEY")}}
```

### When defmethod Is Still Required

Use explicit `defmethod` for: `ig/halt-key!`, `ig/suspend-key!`, `ig/resume-key`, complex initialization, or refs.

---

## JSON with Charred

This project uses [charred](https://github.com/cnuernber/charred) for JSON parsing and writing. It's a zero-dependency, high-performance library.

### Reading JSON

```clojure
(require '[charred.api :as charred])

;; Parse JSON string to Clojure data
(charred/read-json "{\"name\": \"Alice\", \"age\": 30}")
;; => {"name" "Alice", "age" 30}

;; Convert keys to keywords
(charred/read-json "{\"name\": \"Alice\"}" :key-fn keyword)
;; => {:name "Alice"}
```

### Writing JSON

```clojure
;; Serialize to JSON string
(charred/write-json-str {:name "Alice" :age 30})
;; => "{\"name\":\"Alice\",\"age\":30}"
```

### High-Performance Patterns

For repeated operations, create specialized parse/write functions:

```clojure
;; Create optimized parser (thread-safe)
(def parse-json (charred/parse-json-fn {:key-fn keyword}))
(parse-json "{\"count\": 42}")
;; => {:count 42}

;; Create optimized writer (thread-safe)
(def write-json (charred/write-json-fn))
(write-json {:status "ok"})
```

---

## html.yeah Components

Define schema-validated [Chassis](https://github.com/onionpancakes/chassis) alias elements with `defelem`:

```clojure
(require '[html.yeah :as hy :refer [defelem]])

(defelem my-card
  [:map {:doc "A card with title and optional description"
         :my-card/keys [title description]
         :as attrs}
   [:my-card/title :string]
   [:my-card/description {:optional true} :string]]
  [:div.my-card attrs
   [:h3.my-card-title title]
   (when description
     [:p.my-card-description description])])
```

**Key concepts:**
- **Schema properties** - `:doc` for documentation, `:<component>/keys` for destructuring
- **`:as attrs`** - Bind full attribute map to pass through HTML attrs to root element
- **`(hy/children)`** - Placeholder for child elements passed to the component

### CRITICAL: Single Attribute Map Rule

Hiccup elements accept only ONE attribute map. If you pass multiple maps, the extras render as text:

```clojure
;; WRONG - two attribute maps, second renders as text "{:class ...}"
[:div.my-card {:class "extra"} attrs (hy/children)]

;; CORRECT - merge computed classes into the single attrs map
[:div.my-card (update attrs :class #(into ["extra"] (if (coll? %) % (when % [%]))))
 (hy/children)]
```

**Symptom of violation:** Raw maps like `{:button/variant :secondary}` appearing as text in rendered output.

### Children Support

```clojure
(defelem container
  [:map {:doc "A container with optional padding"
         ::hy/children [:* :any]
         :container/keys [padded]
         :as attrs}
   [:container/padded {:optional true} :boolean]]
  [:div (assoc attrs :class ["container" (when padded "p-4")])
   (hy/children)])
```

### Discovery API

```clojure
(hy/element :myapp.ui/my-card)      ;; Lookup component by keyword
(hy/elements)                        ;; List all elements
(hy/elements {:ns 'myapp.ui})        ;; Filter by namespace
(hy/search-elements "button")        ;; Search by documentation
```

### Attribute Transformation

For complex transformations (e.g., converting maps to JSON):

```clojure
(require '[html.yeah.attrs :as attrs])

(defn jsonify-signals [tag attrs]
  (if (map? (:data-signals attrs))
    (update attrs :data-signals charred/write-json-str)
    attrs))

(defelem signal-div
  [:map {::attrs/transform jsonify-signals :as attrs}]
  [:div attrs (hy/children)])
```

---

## Development Setup

### Prerequisites

```bash
docker compose up -d    # Start PostgreSQL
```

### Starting the REPL

```bash
clj -M:dev
```

### Development Workflow

1. Start REPL with `clj -M:dev`
2. Load dev namespace: `(dev)`
3. Start the system: `(start)`
4. Make changes to source files
5. Reload: `(reload)`

The `dev` namespace provides:
- `(start)` / `(stop)` / `(reload)` / `(restart)` - System lifecycle
- `(dispatch effects)` - Dispatch sandestin effects
- `(dispatch)` - Get the raw dispatch function (for discovery)
- `(describe (dispatch))` - List all registered effects/actions
- `(sample (dispatch) key)` - Generate sample invocations
- `(grep (dispatch) pattern)` - Search registry

### Portal

Portal opens automatically. **Note for Claude Code:** Portal output is not accessible programmatically. Use `prn` for debugging during automated sessions instead of `tap>`.

### Component Preview (Tsain Sandbox)

Open `localhost:3000/sandbox` then use tsain effects:

```clojure
(require '[ascolais.tsain :as tsain])

(dispatch [[::tsain/preview [:h1 "Hello World"]]])
(dispatch [[::tsain/preview-clear]])
(dispatch [[::tsain/preview-append [:div.card "Content"]]])

;; Commit component to library (MUST include :examples with hiccup!)
(dispatch [[::tsain/commit :{{top/ns}}.views.components/my-card
  {:description "Card component"
   :category "Layout"
   :examples [{:label "Default"
               :hiccup [:{{top/ns}}.views.components/my-card
                        {:my-card/title "Example"}]}]}]])
```

**Categories:** `Actions`, `Forms`, `Layout`, `Feedback`

**CRITICAL: Every new or modified `defelem` component MUST be committed to the tsain library (`::tsain/commit`) before the work is considered complete. A component that exists in code but not in the sandbox library is unfinished. After committing to the tsain library, restart the web server with `(dev/restart)` to persist the component database.**

### Design System Reference

- **Display font:** IBM Plex Serif (headings)
- **Body font:** System sans-serif
- **Accent:** `--accent-primary` (teal-600 #0d9488)
- **Backgrounds:** `--bg-primary`, `--bg-secondary`, `--bg-tertiary`
- **Spacing:** 4px base unit (--space-1 through --space-16)
- **Border Radius:** --radius-sm (4px) through --radius-xl (12px)

---

## Database Migrations

Migrations use ragtime with SQL files in `resources/migrations/`.

**Naming:** `001-description.up.sql` and `001-description.down.sql`

**Pattern:** Always use `IF NOT EXISTS` / `IF EXISTS` for idempotent migrations:
```sql
-- up
CREATE TABLE IF NOT EXISTS foo (...);
CREATE INDEX IF NOT EXISTS idx_foo ON foo(...);

-- down
DROP TABLE IF EXISTS foo;
```

```clojure
(migrate!)    ;; Apply pending migrations
(rollback!)   ;; Undo last migration
```

```bash
docker compose down -v    # Reset database (delete volume)
```

---

## Project Structure

```
src/clj/{{top/file}}/
  {{main/file}}.clj        # Application entry point
  config.clj               # Integrant system configuration
  secrets.clj              # .env file loader for secrets
  auth.clj                 # Google OAuth authentication
  routes.clj               # Ring route handlers
  fx/                      # Effect registries

dev/src/clj/
  user.clj                 # REPL initialization
  dev.clj                  # Dev namespace

resources/
  migrations/              # SQL migration files
  public/styles.css        # Component CSS (hot-reloadable)

components.db              # Tsain component library (SQLite) - COMMIT THIS FILE
```

---

## REPL Evaluation

Use the clojure-eval skill to evaluate code via nREPL.

**Always discover the port first** - never hardcode or guess ports:

```bash
clj-nrepl-eval --discover-ports    # Find running nREPL servers
clj-nrepl-eval -p <PORT> "(dev)"   # Use discovered port
clj-nrepl-eval -p <PORT> "(dev/reload)"
```

Do NOT use `lsof`, `netstat`, or other manual methods to find nREPL ports.

### Accessing the Running System

The running Integrant system is available at `dev/*system*`:

```clojure
(keys dev/*system*)                                    ;; List all components
(:{{top/ns}}.config/store dev/*system*)                ;; Get sfere connection store
(:{{top/ns}}.config/datasource dev/*system*)           ;; Get database connection
```

### Testing UI Updates from REPL

**Don't dispatch twk effects directly** - they require an SSE context and will fail:

```clojure
;; WRONG - fails with "not a SSEGenerator"
(dev/dispatch [[::twk/patch-signals {:foo "bar"}]])
```

**Use sfere broadcast** to push updates to connected browsers:

```clojure
(require '[ascolais.sfere :as sfere])

;; List active connections
(sfere/list-connections (:{{top/ns}}.config/store dev/*system*))
;; => ([:ascolais.sfere/default-scope [:page "home" "session-id"]] ...)

;; Broadcast to all home page connections
(dev/dispatch
  [[:ascolais.sfere/broadcast
    {:pattern [:ascolais.sfere/default-scope [:page :*]]}
    [:ascolais.twk/patch-signals {:testSignal true}]
    [:ascolais.twk/patch-elements
     [:div#test-banner [:p "Hello from REPL!"]]
     {:ascolais.twk/selector "#test-banner"
      :ascolais.twk/patch-mode :ascolais.twk/pm-outer}]]])
```

For testing business logic without UI, extract pure functions and test those directly.

---

## Hypermedia-First Development

This app uses Datastar's hypermedia approach: **the server drives UI state**.

**Prefer server-driven updates:**
- `::twk/patch-signals` - update client state from server
- `::twk/patch-elements` - ship HTML fragments from server

**Avoid client-side JavaScript:**
- `::twk/execute-script` - only for things that MUST run client-side (clipboard, scroll, focus)
- Don't use `execute-script` to modify signals - it can't access Datastar's signal context

**Example - modal auto-dismiss:**
```clojure
;; WRONG - execute-script can't access signals
[::twk/execute-script "setTimeout(() => { $showModal = false }, 1500)"]

;; CORRECT - server sends signals when ready (e.g., after LLM responds)
[::twk/patch-signals {:showModal false :hasResult true}]
```

The server decides WHEN to update UI. Let async operations (LLM calls, DB queries) complete, then send the appropriate signals/elements.

---

## Sandestin Effect System

### Registry Authoring

```clojure
(require '[ascolais.sandestin :as s])

{::s/effects      {qualified-keyword -> EffectRegistration}
 ::s/actions      {qualified-keyword -> ActionRegistration}
 ::s/placeholders {qualified-keyword -> PlaceholderRegistration}
 ::s/interceptors [Interceptor ...]
 ::s/system-schema {keyword -> MalliSchema}
 ::s/system->state (fn [system] state)}
```

### Effect Structure

Effects are the boundary layer - they wrap external calls (databases, APIs, libraries) directly. **Don't dispatch other effects from within an effect handler** - call the library instead.

```clojure
{::s/effects
 {:app/save-user
  {::s/description "Save user to database"
   ::s/schema [:tuple [:= :app/save-user] :map]
   ::s/handler (fn [{:keys [dispatch]} _system user]
                 ;; Call library directly, not (dispatch [[::other-effect ...]])
                 (db/save! datasource user))}}}
```

### Action Structure

Actions are pure functions that return effect vectors:

```clojure
{::s/actions
 {:app/update-profile
  {::s/description "Update user profile and notify"
   ::s/schema [:tuple [:= :app/update-profile] :string :map]
   ::s/handler (fn [state user-id changes]
                 [[:app/save-user (merge (:user state) changes)]
                  [::twk/patch-elements [:div#status "Saved!"]]])}}}
```

### Creating a Dispatch

```clojure
(def dispatch
  (s/create-dispatch
    [(twk/registry)
     (sfere/registry store)
     (manse/registry {:datasource datasource})
     app-registry]))
```

### Invoking a Dispatch

```clojure
(dispatch [[:app/log "hello"]])                          ;; 1-arity: effects only
(dispatch {:db connection} [[:app/save-user {:name "Alice"}]])  ;; 2-arity: system + effects
(dispatch {:db conn} {:current-user {:id 1}} [[:app/greet]])    ;; 3-arity: + dispatch-data
```

### Placeholder Structure

Placeholders enable continuations - they're interpolated when effects dispatch continuation effect vectors.

```clojure
{::s/placeholders
 {::result
  {::s/description "Access result in continuations. Self-preserving."
   ::s/schema [:or [:tuple [:= ::result]] [:tuple [:= ::result] :keyword]]
   ::s/handler
   (fn [dispatch-data & [key]]
     (if (contains? dispatch-data ::result)
       (let [res (::result dispatch-data)]
         (if key (get res key) res))
       ;; Self-preserve if data not available yet
       (if key [::result key] [::result])))}}}
```

**Key points:**
- Handler receives `dispatch-data` (first arg to dispatch) plus optional args from the placeholder form
- **Self-preservation:** Return the placeholder form itself if data isn't in dispatch-data yet
- Schema validates the placeholder's usage forms (e.g., `[::result]` or `[::result :key]`)

### Continuation Pattern

**Use continuations for effects that produce results needed by subsequent effects.** This keeps route handlers pure - they return effect vectors without needing dispatch access.

```clojure
{::s/effects
 {::generate
  {::s/handler
   (fn [{:keys [dispatch]} _ {:keys [options on-success on-error]}]
     (try
       (let [result (my-library/call options)]  ; Call library directly
         (dispatch {::result result} on-success))
       (catch Exception e
         (when on-error
           (dispatch {::error (ex-message e)} on-error)))))}}}

;; Route handler stays pure - returns effect vector, no dispatch needed
(defn my-handler [{:keys [signals]}]
  {::twk/fx
   [[::my/generate
     {:options (:opts signals)
      :on-success [[::twk/patch-elements (view [::my/result])]]
      :on-error [[::twk/patch-elements [:div "Error"]]]}]]})
```

Dispatch interpolates `[::my/result]` using the placeholder handler with the provided dispatch-data.

**Important:** Embed placeholders directly in data structures. Don't pass them to functions:

```clojure
;; WRONG - view-fn receives placeholder before interpolation, destructures to nils
:on-success [[::twk/patch-elements (my-view-fn [::my/result])]]

;; CORRECT - placeholder stays in hiccup, gets interpolated
:on-success [[::twk/patch-elements [:my-component {:data [::my/result :field]}]]]
```

---

## REPL Discovery API

**Critical:** Use these functions to explore available effects, actions, and their schemas.

```clojure
(s/describe dispatch)                       ;; List all items
(s/describe dispatch :effects)              ;; List effects only
(s/describe dispatch ::twk/patch-elements)  ;; Inspect specific effect
(s/sample dispatch ::twk/patch-elements)    ;; Generate example invocation
(s/grep dispatch "message")                 ;; Search by pattern
```

---

## Manse Database Effects

| Effect | Purpose |
|--------|---------|
| `::manse/execute` | Execute query, return all rows |
| `::manse/execute-one` | Execute query, return first row |
| `::manse/execute-one!` | Execute query, throw if no row |

```clojure
(fn [state user-id]
  [[::manse/execute-one ["SELECT * FROM users WHERE id = ?" user-id]]])
```

---

## Datastar Frontend Framework

Datastar is a lightweight frontend framework combining backend-driven reactivity with frontend interactivity.

### Core Concepts

1. **Backend drives state** - Server pushes state via SSE
2. **Signals** - Reactive variables prefixed with `$`
3. **Attributes** - `data-*` attributes declare reactive behavior
4. **Actions** - `@get()`, `@post()` send requests that return SSE

### Gathering User Input

**Signal-based** - Bind inputs to signals with `data-bind`. Values are sent automatically with `@post()`:
```clojure
[:input {:data-bind:email true :placeholder "Email"}]
[:button {:data-on:click "@post('/api/subscribe')"} "Subscribe"]
;; Handler reads from (:email signals)
```

**Form-based** - Use `{contentType: 'form'}` for traditional HTML form submission:
```clojure
[:form {:data-on:submit__prevent "@post('/api/submit', {contentType: 'form'})"}
 [:input {:name "email"}]
 [:button "Submit"]]
;; Handler reads from (get form-params "email")
```

**Key difference:** Signals are NOT sent with `{contentType: 'form'}` requests. Choose form-based when you need native form features (validation, field grouping) or want to avoid signal synchronization complexity. Use hidden inputs if you need extra state with form submissions.

### Hypermedia-First Approach

**The server owns data state** (lists, records, etc.) while **signals handle UI state** (open/closed, selected, form inputs).

```clojure
;; Signals for UI interactions
[:div {:data-signals:open "false"}
 [:button {:data-on:click "$open = !$open"} "Toggle"]
 [:div {:data-show "$open"} "Dropdown content"]]

;; Server renders data, patches via twk (avoid client-side data-for)
(defn render-tags [tags]
  [:div#tags (for [tag tags] [:span.tag tag])])
```

**Signal access**: Signals (`$signalName`) can only be read/written from within Datastar expressions (`data-*` attributes), **never from global JavaScript or `::twk/execute-script`**. Common mistakes:
- `::twk/execute-script "setTimeout(() => { $mySignal = false }, 1000)"` — **won't work**, signals undefined in global JS
- Instead, use `::twk/patch-signals` from server, or define a global function that accepts signal proxies and call it from a `data-on:*` expression

### Attribute Plugins

| Attribute | Purpose | Example |
|-----------|---------|---------|
| `data-text` | Set text content | `[:span {:data-text "$count"}]` |
| `data-show` | Toggle visibility | `[:div {:data-show "$isVisible"}]` |
| `data-class` | Toggle CSS classes | `[:div {:data-class:active "$isActive"}]` |
| `data-attr` | Set HTML attributes | `[:button {:data-attr:disabled "$loading"}]` |
| `data-bind` | Two-way form binding | `[:input {:data-bind:email true}]` |
| `data-on` | Event handlers | `[:button {:data-on:click "$count++"}]` |

### Event Modifiers

Modifiers use **double underscore (`__`)** as delimiter. Dots (`.`) provide arguments:

```clojure
[:div {:data-on:click__outside "$open = false"}]
[:input {:data-on:input__debounce.300ms "@get('/search')"}]
[:button {:data-on:click__once__prevent "handle()"}]
```

| Modifier | Purpose |
|----------|---------|
| `__debounce.Nms` | Delay until idle |
| `__throttle.Nms` | Rate limit |
| `__outside` | Trigger when clicking outside element |
| `__window` | Attach listener to window |
| `__once` | Only trigger once |
| `__prevent` | Call `preventDefault()` |
| `__stop` | Call `stopPropagation()` |

### Preventing FOUC (Flash of Unstyled Content)

**CRITICAL:** Elements using `data-show` will flash visible before Datastar initializes. This is because `data-show` only sets `display: none` when false—it cannot override CSS.

**Understanding `data-show` behavior:**
- When expression is **false**: sets inline `style="display: none"`
- When expression is **true**: **removes** inline style (does NOT set `display: block`)
- Before Datastar runs: no inline style exists, so element is visible

**Pattern 1: Inline style for initially-hidden elements**

For elements that should start hidden (loading states, error states, etc.), add `style="display:none"` directly:

```clojure
;; WRONG - flashes visible before Datastar hides it
[:div {:data-show "$isLoading"} "Loading..."]

;; CORRECT - hidden immediately, Datastar takes over
[:div {:style {:display "none"} :data-show "$isLoading"} "Loading..."]
```

**Pattern 2: CSS + data-class for modals/dropdowns**

For modals and dropdowns, use CSS `display: none` by default with `data-class:is-shown`:

```css
.my-modal { display: none; }
.my-modal.is-shown { display: flex; }
```

```clojure
[:div.my-modal {"data-class:is-shown" "$showModal"} ...]
```

This works because `data-class` **adds** the class when true, and CSS `.is-shown` overrides the base `display: none`.

**Pattern 3: Wait for SSE connection for auth-dependent UI**

For elements that depend on server state (like auth), wait for `$connected` to avoid showing wrong state:

```clojure
;; WRONG - shows sign-in button briefly even when logged in
[:div {:data-show "!$user"} [auth-button]]
[:div {:data-show "$user"} [account-menu]]

;; CORRECT - neither shows until SSE establishes actual state
[:div {:style {:display "none"} :data-show "$connected && !$user"} [auth-button]]
[:div {:style {:display "none"} :data-show "$connected && $user"} [account-menu]]
```

**Summary:**
| Scenario | Solution |
|----------|----------|
| Element starts hidden, signal starts false | `{:style {:display "none"} :data-show "$signal"}` |
| Modal/dropdown with toggle | CSS `display: none` + `data-class:is-shown` |
| Depends on SSE state | Add `$connected &&` to expression |

---

## TWK (Datastar) Patterns

### Hiccup in TWK

Return hiccup data structures directly - TWK renders them automatically.

**Important:** This is server-side hiccup, not Reagent. No `:<>` fragments:

```clojure
;; CORRECT
[:div [:h1 "Hello"] [:p "World"]]
[[:h1 "Hello"] [:p "World"]]  ;; nested vectors for siblings

;; WRONG - no React fragments
[:<> [:h1 "Hello"] [:p "World"]]
```

### Available TWK Effects

| Effect | Purpose |
|--------|---------|
| `::twk/patch-elements` | Update DOM elements with hiccup |
| `::twk/patch-signals` | Update Datastar client signals |
| `::twk/execute-script` | Run JavaScript in browser |
| `::twk/close-sse` | Close SSE connection |

### Patch Modes

```clojure
(require '[ascolais.twk :as twk])

twk/pm-outer  twk/pm-inner  twk/pm-append  twk/pm-prepend
twk/pm-before  twk/pm-after  twk/pm-remove

[::twk/patch-elements [:div "content"]
 {twk/selector "#target" twk/patch-mode twk/pm-append}]
```

---

## Sfere (Connection Management)

```clojure
;; Development (in-memory)
(def store (sfere/store {:type :atom}))

;; Production with TTL
(def store (sfere/store {:type :caffeine :duration-ms 30000}))

;; Broadcast to pattern (uses :* wildcards)
[[:ascolais.sfere/broadcast {:pattern [:* [:room "lobby" :*]]}
  [::twk/patch-elements [:div "announcement"]]]]
```

---

## SSE Route Handlers

For persistent SSE connections (real-time updates, live dashboards, chat), use manual handlers that return `::sfere/key` to store the connection:

```clojure
(defn sse-connect
  "Establish persistent SSE connection."
  [{:keys [signals]}]
  (let [session-id (:sessionId signals)]
    {::sfere/key [:page "home" session-id]
     ::twk/fx [[::twk/patch-signals {:connected true}]]}))
```

Route definition:

```clojure
["/sse/home" {:get {:handler sse-connect}}]
```

SSE handlers:
1. Return `::sfere/key` to store the connection
2. Return `::twk/fx` for initial effects to send
3. Connection stays open for broadcasts

---

## Static Page Routes

For routes needing synchronous data access (e.g., server-rendered pages with OG meta tags), access dispatch from reitit match data:

```clojure
(defn my-handler [request]
  (let [dispatch (-> request :reitit.core/match :data :dispatch)
        result (dispatch [[::manse/execute-one ["SELECT * FROM foo WHERE id = ?" id]]])
        data (-> result :results first :res)]
    {:body (my-view data)}))
```

Dispatch is available because it's added to the router's `:data` map in `config.clj`. This pattern is useful when the page needs data before rendering (not suitable for SSE streaming).

---

## Effect Organization

Each domain gets its own namespace in `fx/` exporting a `registry` function:

```clojure
(ns {{top/ns}}.fx.users
  (:require [ascolais.sandestin :as s]
            [ascolais.manse :as manse]))

(defn registry [{:keys [datasource]}]
  {::s/effects
   {::create-user
    {::s/description "Create a new user in the database."
     ::s/schema [:tuple [:= ::create-user] [:map [:email :string] [:name :string]]]
     ::s/handler (fn [{:keys [dispatch]} _system user-data]
                   (dispatch [[::manse/execute-one
                               ["INSERT INTO users (email, name) VALUES (?, ?) RETURNING *"
                                (:email user-data) (:name user-data)]]]))}}})
```

**Conventions:** One registry per domain, namespaced effect keys, descriptions always.

---

## Adding Dependencies

```clojure
;; Add to running REPL first, then add to deps.edn once confirmed
(clojure.repl.deps/add-lib 'metosin/malli {:mvn/version "0.20.0"})
```

---

## Secrets Management

Secrets via `.env` file in development, environment variables in production.

| File | Purpose | Committed? |
|------|---------|------------|
| `.env` | Local development secrets | No (gitignored) |
| `.env.example` | Template showing required secrets | Yes |

**Adding a new secret:**
1. Add placeholder to `.env.example`
2. Add real value to `.env`
3. Use in `config.clj`: `(get secrets "NEW_SERVICE_API_KEY")`
4. Run `(restart)` in REPL

| Secret | Purpose |
|--------|---------|
| `ANTHROPIC_API_KEY` | Claude API access |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret |
| `SESSION_SECRET` | 16-character key for session encryption |

### Production OAuth Setup

**Google Cloud Console Configuration:**

1. Create a project at [console.cloud.google.com](https://console.cloud.google.com)
2. Enable the Google+ API (for user profile access)
3. Configure OAuth consent screen:
   - User type: External
   - App name, logo, support email
   - Scopes: `openid`, `email`, `profile`
4. Create OAuth 2.0 credentials:
   - Application type: Web application
   - Authorized redirect URIs: `https://your-domain.com/auth/callback`
   - Copy Client ID and Client Secret

**Environment Variables:**

```bash
GOOGLE_CLIENT_ID=123456789-xxxxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxx
SESSION_SECRET=exactly16chars!!  # Generate: openssl rand -base64 12 | head -c 16
```

**CSRF Protection:**

The OAuth flow uses a cryptographically random state parameter stored in the session. This prevents CSRF attacks by validating that the callback state matches what was sent in the initial request.

**Security Checklist:**

- [ ] SESSION_SECRET is set (not the default from .env.example)
- [ ] HTTPS is enforced (required for secure cookies)
- [ ] OAuth redirect URI matches exactly (no trailing slashes)
- [ ] Google OAuth consent screen is verified (for production)

---

## Accessibility

Accessibility is a requirement, not a nice-to-have:
- Use semantic HTML elements (button, nav, main, etc.)
- Include aria-live for dynamic content updates
- Ensure keyboard navigation works
- Provide alt text for images

---

## Component Styling Conventions

1. **Exploration phase** - Use inline styles for rapid iteration
2. **Before commit** - Extract all styles to `resources/public/styles.css`
3. Use BEM-like naming: `.component-name`, `.component-name-element`, `.component-name--modifier`
4. Use CSS custom properties: `var(--bg-primary)`, `var(--accent-cyan)`

---

## Code Style

- Follow standard Clojure conventions
- Use `cljfmt` formatting (applied automatically via hooks)
- Prefer pure functions where possible
- Use conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`
