# {{top/ns}}/{{main}}

## Project Overview

This is a Clojure web application powered by the sandestin effect dispatch ecosystem with Datastar for reactive frontend components.

### Key Libraries

| Library | Purpose | Provides |
|---------|---------|----------|
| **sandestin** | Effect dispatch with schema-driven discoverability | `s/create-dispatch`, discovery API |
| **twk** | Datastar SSE integration | `twk/registry`, `twk/with-datastar` middleware |
| **sfere** | Connection management and broadcasting | `sfere/registry`, `sfere/store` |
| **kaiin** | Declarative HTTP routing from registry metadata | `kaiin/routes` |
| **manse** | Database effects with next.jdbc | `manse/registry`, execute effects |
| **tsain** | Component development sandbox | `tsain/registry`, preview effects |

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

## Technology Stack

- **Clojure 1.12** with deps.edn
- **Integrant** for system lifecycle
- **clj-reload** for namespace reloading
- **Portal** for data inspection (tap>)
- **PostgreSQL** with HikariCP connection pool
- **Cognitect test-runner** for tests

## Development Setup

### Prerequisites

```bash
# Start PostgreSQL
docker-compose up -d

# Verify database is running
docker-compose ps
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
- `(start)` - Start the system at localhost:3000
- `(stop)` - Stop the system
- `(reload)` - Reload changed namespaces via clj-reload
- `(restart)` - Stop, reload, and start
- `(dispatch effects)` - Dispatch sandestin effects
- `(dispatch)` - Get the raw dispatch function (for discovery)
- `(describe (dispatch))` - List all registered effects/actions
- `(sample (dispatch) key)` - Generate sample invocations
- `(grep (dispatch) pattern)` - Search registry

### Portal

Portal opens automatically when the dev namespace loads. Any `(tap> data)` calls will appear in the Portal UI.

### Component Preview (Tsain Sandbox)

The sandbox provides a browser-based preview area for rapidly iterating on hiccup components. Open `localhost:3000/sandbox` in a browser, then use the tsain registry effects via dispatch:

```clojure
(require '[ascolais.tsain :as tsain])

;; Replace preview with new content
(dispatch [[::tsain/preview [:h1 "Hello World"]]])

;; Build up content by appending
(dispatch [[::tsain/preview-clear]])
(dispatch [[::tsain/preview-append [:div.card [:h3 "Card 1"] [:p "First card"]]]])

;; Commit a component to the library
(dispatch [[::tsain/commit :my-card {:description "Card component"}]])

;; Show a specific component
(dispatch [[::tsain/show-components :my-card]])

;; Patch Datastar signals for testing interactivity
(dispatch [[::tsain/patch-signals {:count 42}]])
```

---

## Database Migrations

Migrations use ragtime with SQL files in `resources/migrations/`.

### Creating a Migration

1. Create numbered SQL files:
   - `resources/migrations/001-create-users.up.sql`
   - `resources/migrations/001-create-users.down.sql`

2. Run from REPL:
   ```clojure
   (migrate!)    ;; Apply pending migrations
   (rollback!)   ;; Undo last migration
   ```

### Resetting Database

```clojure
;; Roll back all, then migrate
(dotimes [_ 10] (rollback!))
(migrate!)
```

### Docker Commands

```bash
docker-compose up -d      # Start PostgreSQL
docker-compose down       # Stop PostgreSQL
docker-compose down -v    # Reset database (delete volume)
```

---

## Project Structure

```
src/clj/{{top/file}}/
  {{main/file}}.clj      # Application entry point
  config.clj             # Integrant system configuration
  routes.clj             # Ring route handlers
  fx/                    # Effect registries

dev/src/clj/
  user.clj               # REPL initialization
  dev.clj                # Dev namespace
  dev/config.clj         # Dev integrant config
  sandbox/
    ui.clj               # Chassis alias definitions
    views.clj            # Sandbox view re-exports

resources/
  migrations/            # SQL migration files

dev/resources/
  components.edn         # Tsain component library
  public/
  styles.css             # Component CSS (hot-reloadable)

test/src/clj/            # Test files
```

---

## REPL Evaluation

Use the clojure-eval skill to evaluate code via nREPL.

### Starting an nREPL Server

```bash
clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.0"}}}' -M:dev -m nrepl.cmdline --port 7888
```

### Connecting and Evaluating

```bash
clj-nrepl-eval --discover-ports          # Find running REPLs
clj-nrepl-eval -p 7888 "(+ 1 2 3)"       # Evaluate expression
```

**Important:** All REPL evaluation should take place in the `dev` namespace:

```bash
clj-nrepl-eval -p 7888 "(dev)"
clj-nrepl-eval -p 7888 "(reload)"
```

---

## Sandestin Effect System

### Registry Authoring

A registry is a map with namespaced keys under `ascolais.sandestin`:

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

Effects are side-effecting operations:

```clojure
{::s/effects
 {:app/save-user
  {::s/description "Save user to database"
   ::s/schema [:tuple [:= :app/save-user] :map]
   ::s/system-keys [:db]
   ::s/handler (fn [{:keys [dispatch dispatch-data]} system user]
                 (db/save! (:db system) user)
                 {:saved true})}}}
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
(require '[ascolais.sandestin :as s]
         '[ascolais.twk :as twk]
         '[ascolais.sfere :as sfere]
         '[ascolais.manse :as manse])

(def dispatch
  (s/create-dispatch
    [(twk/registry)
     (sfere/registry store)
     (manse/registry {:datasource datasource})
     app-registry]))
```

### Invoking a Dispatch

```clojure
;; 1-arity: effects only
(dispatch [[:app/log "hello"]])

;; 2-arity: system + effects
(dispatch {:db connection}
          [[:app/save-user {:name "Alice"}]])

;; 3-arity: system + dispatch-data + effects
(dispatch {:db connection}
          {:current-user {:id 1}}
          [[:app/greet [:app/current-user]]])
```

---

## REPL Discovery API

**Critical:** Use these functions to explore available effects, actions, and their schemas.

### describe - List and inspect registered items

```clojure
(s/describe dispatch)                    ;; List all items
(s/describe dispatch :effects)           ;; List effects only
(s/describe dispatch ::twk/patch-elements)  ;; Inspect specific effect
```

### sample - Generate example invocations

```clojure
(s/sample dispatch ::twk/patch-elements)     ;; One sample
(s/sample dispatch ::twk/patch-signals 3)    ;; Multiple samples
```

### grep - Search by pattern

```clojure
(s/grep dispatch "message")              ;; String search
(s/grep dispatch #"broadcast|connection")  ;; Regex search
```

---

## Manse Database Effects

### Available Effects

| Effect | Purpose |
|--------|---------|
| `::manse/execute` | Execute query, return all rows |
| `::manse/execute-one` | Execute query, return first row |
| `::manse/execute-one!` | Execute query, throw if no row |

### Usage

```clojure
;; In an effect handler
(fn [{:keys [dispatch]} system query-params]
  (dispatch
    [[::manse/execute-one
      ["SELECT * FROM users WHERE id = ?" user-id]]]))

;; From action (returns effects)
(fn [state user-id]
  [[::manse/execute-one
    ["SELECT * FROM users WHERE id = ?" user-id]]])
```

---

## Datastar Frontend Framework

Datastar is a lightweight frontend framework combining backend-driven reactivity with frontend interactivity.

### Core Concepts

1. **Backend drives state** - Server pushes state via SSE
2. **Signals** - Reactive variables prefixed with `$`
3. **Attributes** - `data-*` attributes declare reactive behavior
4. **Actions** - `@get()`, `@post()` send requests that return SSE

### Signals

```html
<!-- Two-way binding -->
<input data-bind:username />

<!-- Direct initialization -->
<div data-signals:count="0"></div>
<div data-signals="{count: 0, user: {name: 'Alice'}}"></div>

<!-- Derived signals -->
<div data-computed:doubled="$count * 2"></div>
```

### Attribute Plugins

| Attribute | Purpose | Example |
|-----------|---------|---------|
| `data-text` | Set text content | `<span data-text="$count"></span>` |
| `data-show` | Toggle visibility | `<div data-show="$isVisible"></div>` |
| `data-class` | Toggle CSS classes | `<div data-class:active="$isActive"></div>` |
| `data-attr` | Set HTML attributes | `<button data-attr:disabled="$loading"></button>` |
| `data-bind` | Two-way form binding | `<input data-bind:email />` |
| `data-on` | Event handlers | `<button data-on:click="$count++"></button>` |

### Event Handling

```html
<button data-on:click="$count++">Increment</button>
<button data-on:click="@post('/api/save')">Save</button>
<input data-on:input__debounce.300ms="@get('/search')" />
```

Use `evt` to access DOM event:
```html
<select data-on:change="@post('/api/update?value=' + evt.target.value)">
```

---

## TWK (Datastar) Patterns

### Hiccup in TWK

Return hiccup data structures directly - TWK renders them automatically:

```clojure
;; CORRECT - return hiccup directly
{:body [:h1 "Hello"]}

;; Datastar SSE response
{:ascolais.twk/fx
 [[:ascolais.twk/patch-elements [:div "content"]]
  [:ascolais.twk/patch-signals {:count 1}]]}
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

[:ascolais.twk/patch-elements [:div "content"]
 {twk/selector "#target" twk/patch-mode twk/pm-append}]
```

---

## Sfere (Connection Management)

### Creating a Store

```clojure
;; Development (in-memory)
(def store (sfere/store {:type :atom}))

;; Production with TTL
(def store (sfere/store {:type :caffeine :duration-ms 30000}))
```

### Broadcast Effect

```clojure
;; Broadcast to pattern (uses :* wildcards)
[[:ascolais.sfere/broadcast {:pattern [:* [:room "lobby" :*]]}
  [:ascolais.twk/patch-elements [:div "announcement"]]]]

;; Pattern matching
[:* [:room "lobby" :*]]    ;; All users in "lobby"
[:* [:room :* :*]]         ;; All users in any room
[:* :*]                    ;; All connections
```

---

## Kaiin (Declarative Routing)

### Registry with Kaiin Metadata

```clojure
{::s/actions
 {:room/send-message
  {::s/description "Send a message to a room"
   ::s/schema [:tuple [:= :room/send-message] :string :string :string]
   ::s/handler (fn [_state room-id username message]
                 [[:ascolais.twk/patch-elements
                   [:div.message [:strong username] ": " message]
                   {twk/selector "#messages" twk/patch-mode twk/pm-append}]])

   ;; Kaiin metadata
   ::kaiin/path "/room/:room-id/message"
   ::kaiin/method :post
   ::kaiin/signals [:map [:username :string] [:message :string]]
   ::kaiin/dispatch [:room/send-message
                     [::kaiin/path-param :room-id]
                     [::kaiin/signal :username]
                     [::kaiin/signal :message]]
   ::kaiin/target [:* [:room [::kaiin/path-param :room-id] :*]]}}}
```

### Generating Routes

```clojure
(require '[ascolais.kaiin :as kaiin])

;; Generate routes from dispatch
(kaiin/routes dispatch)

;; Combine with custom routes
(def router
  (rr/router
    (into custom-routes (kaiin/routes dispatch))
    {:data {:middleware [(twk/with-datastar adapter dispatch)]}}))
```

---

## Effect Organization

### Pattern

Each domain gets its own namespace in `fx/` exporting a `registry` function:

```clojure
(ns {{top/ns}}.fx.users
  (:require [ascolais.sandestin :as s]
            [ascolais.manse :as manse]
            [ascolais.twk :as twk]
            [ascolais.kaiin :as kaiin]))

(defn registry
  "User management effects."
  [{:keys [datasource]}]
  {::s/effects
   {::create-user
    {::s/description "Create a new user in the database."
     ::s/schema [:tuple [:= ::create-user] [:map [:email :string] [:name :string]]]
     ::s/handler
     (fn [{:keys [dispatch]} _system user-data]
       (dispatch
         [[::manse/execute-one
           ["INSERT INTO users (email, name) VALUES (?, ?) RETURNING *"
            (:email user-data) (:name user-data)]]]))}}})
```

### Conventions

- **One registry per domain** - `fx/users.clj`, `fx/orders.clj`, etc.
- **Registry function takes deps** - `(defn registry [{:keys [datasource]}])`
- **Namespaced effect keys** - `::users/create-user` not `:create-user`
- **Descriptions always** - Every effect/action has `::s/description`

---

## Adding Dependencies

When adding new dependencies in a REPL-connected environment:

1. **Add to the running REPL first**:
   ```clojure
   (clojure.repl.deps/add-lib 'metosin/malli {:mvn/version "0.20.0"})
   ```

2. **Confirm the dependency works** in the REPL.

3. **Add to deps.edn** once confirmed working.

---

## Component Styling Conventions

### Development Workflow

1. **Exploration phase** - Use inline styles for rapid iteration
2. **Before commit** - Extract all styles to `dev/resources/public/styles.css`
3. **Commit** - Component hiccup should use CSS classes, not inline styles

### Naming Convention (BEM-like)

```css
.component-name { }
.component-name-element { }
.component-name--modifier { }
```

### Theme Support

Use CSS custom properties:

```css
.my-component {
  background: var(--bg-primary);
  color: var(--accent-cyan);
}
```

---

## Chassis Alias Conventions

Component structure lives in `dev/src/clj/sandbox/ui.clj` as chassis aliases.

### Alias-First Development

```clojure
;; 1. Define structure in sandbox/ui.clj
(defmethod c/resolve-alias ::my-card
  [_ attrs _]
  (let [{:my-card/keys [title subtitle]} attrs]
    [:div.my-card attrs
     [:h3.my-card-title title]
     [:p.my-card-subtitle subtitle]]))

;; 2. Use in dev/resources/components.edn with lean config
[:sandbox.ui/my-card
 {:my-card/title "Hello World"
  :my-card/subtitle "A description"}]
```

### Namespaced Attributes

Chassis elides namespaced attributes from HTML. Use for config props:

```clojure
[:sandbox.ui/game-card
 {:game-card/title "Title"     ;; Config (elided)
  :data-on:click "..."         ;; HTML attr (kept)
  :class "highlighted"}]       ;; HTML attr (kept)
```

---

## Code Style

- Follow standard Clojure conventions
- Use `cljfmt` formatting (applied automatically via hooks)
- Prefer pure functions where possible
- Use `tap>` for debugging output (appears in Portal)

### Namespaced Keywords

```clojure
;; Single colon - explicit namespace
:my.app.config/timeout

;; Double colon - auto-resolved to current namespace
::key  ; becomes :my.current.ns/key

;; With alias
(require '[my.app.db :as db])
::db/query  ; becomes :my.app.db/query
```

## Git Commits

Use conventional commits format:

```
<type>: <description>

[optional body]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
