# ascolais

A [deps-new](https://github.com/seancorfield/deps-new) template for scaffolding full-stack Clojure web applications powered by the **sandestin effect ecosystem**.

## Usage

Generate a new project:

```bash
# Using a published tag
clojure -Sdeps '{:deps {io.github.brianium/ascolais {:git/tag "v0.2.0" :git/sha "54ca1c5"}}}' \
  -Tnew create :template brianium/ascolais :name myorg/myapp

# From local checkout
clojure -Sdeps '{:deps {io.github.brianium/ascolais {:local/root "."}}}' \
  -Tnew create :template brianium/ascolais :name myorg/myapp
```

This generates a complete, ready-to-run application at `myorg-myapp/`.

## What You Get

Generated projects include:

- **Integrant system** with PostgreSQL, HikariCP connection pooling, and ragtime migrations
- **Effect-driven architecture** using sandestin for schema-validated, discoverable effects
- **Datastar frontend** with SSE-based reactivity via twk
- **Connection management** with sfere for broadcasting to connected clients
- **Declarative routing** with kaiin generating routes from effect metadata
- **Component sandbox** with tsain for REPL-driven UI development
- **Claude Code integration** with comprehensive CLAUDE.md, skills, and formatting hooks

### Project Structure

```
myorg-myapp/
├── deps.edn                    # Dependencies
├── docker-compose.yml          # PostgreSQL for development
├── CLAUDE.md                   # Comprehensive ecosystem docs
├── tsain.edn                   # Component sandbox config
│
├── src/clj/myorg/myapp/
│   ├── core.clj                # Application entry point
│   ├── config.clj              # Integrant system configuration
│   ├── routes.clj              # Ring route handlers
│   ├── fx/                     # Effect registries (one per domain)
│   └── views/                  # Hiccup view functions
│       ├── layout.clj          # Page layout with Datastar setup
│       └── components.clj      # Reusable UI components
│
├── dev/src/clj/
│   ├── user.clj                # REPL initialization
│   ├── dev.clj                 # Dev namespace (start/stop/reload)
│   └── dev/config.clj          # Dev-specific Integrant config
│
├── resources/
│   ├── migrations/             # SQL migration files
│   └── public/styles.css       # Application styles
│
├── dev/resources/
│   └── components.edn          # Tsain component library
│
└── .claude/
    ├── settings.json           # Paren repair hooks
    └── skills/                 # clojure-eval, component-iterate
```

## Technology Stack

The template brings together a cohesive set of libraries for building server-driven reactive applications:

| Library | Purpose |
|---------|---------|
| [sandestin](https://github.com/brianium/sandestin) | Effect dispatch with schema-driven discoverability |
| [twk](https://github.com/brianium/twk) | Datastar SSE integration for reactive frontends |
| [sfere](https://github.com/brianium/sfere) | Connection management and pattern-based broadcasting |
| [kaiin](https://github.com/brianium/kaiin) | Declarative HTTP routing from registry metadata |
| [manse](https://github.com/brianium/manse) | Database effects with next.jdbc |
| [tsain](https://github.com/brianium/tsain) | REPL-driven component development sandbox |

### Supporting Libraries

- **http-kit** - HTTP server
- **reitit** - HTTP routing
- **integrant** - Component lifecycle management
- **malli** - Schema validation
- **chassis** - Hiccup DSL with component aliases
- **portal** - Data inspection (dev)
- **clj-reload** - Namespace reloading (dev)
- **ragtime** - Database migrations (dev)

## Architecture

Generated projects follow an effect-driven architecture where all business logic flows through sandestin dispatch:

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                            │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    twk/with-datastar middleware                 │
│  - Parses Datastar signals from headers                         │
│  - Dispatches effects via sandestin                             │
│  - Returns SSE responses                                        │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      sandestin dispatch                         │
│  - Interpolates placeholders from request context               │
│  - Expands actions → effect vectors                             │
│  - Executes effects with interceptors                           │
└──────────────────────────────┬──────────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
    ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
    │  twk effects │   │ sfere effects│   │  app effects │
    │ patch-elements│   │ broadcast    │   │  manse/db    │
    │ patch-signals │   │              │   │  custom...   │
    └──────────────┘   └──────────────┘   └──────────────┘
```

### Key Concepts

**Effects** are side-effecting operations with schemas and descriptions:

```clojure
{::s/effects
 {:app/save-user
  {::s/description "Save user to database"
   ::s/schema [:tuple [:= :app/save-user] :map]
   ::s/handler (fn [ctx system user]
                 (db/save! (:db system) user))}}}
```

**Actions** are pure functions returning effect vectors:

```clojure
{::s/actions
 {:app/update-profile
  {::s/description "Update profile and notify"
   ::s/handler (fn [state changes]
                 [[:app/save-user changes]
                  [::twk/patch-elements [:div#status "Saved!"]]])}}}
```

**Kaiin metadata** on actions generates HTTP routes automatically:

```clojure
{::kaiin/path "/api/users/:id"
 ::kaiin/method :post
 ::kaiin/signals [:map [:name :string]]
 ::kaiin/dispatch [:app/update-profile [::kaiin/signal :name]]}
```

## Development Workflow

After generating a project:

```bash
cd myorg-myapp

# Start PostgreSQL
docker compose up -d

# Start REPL
clj -M:dev
```

In the REPL:

```clojure
(dev)           ; Load dev namespace
(start)         ; Start server at localhost:3000
(reload)        ; Reload changed namespaces
(restart)       ; Full stop + reload + start

;; Discovery API
(describe (dispatch))              ; List all effects
(sample (dispatch) ::twk/patch-elements)  ; Generate examples
(grep (dispatch) "user")           ; Search registry

;; Database
(migrate!)      ; Apply pending migrations
(rollback!)     ; Undo last migration
```

### Component Development

The tsain sandbox at `localhost:3000/sandbox` provides a browser-based preview for iterating on UI components:

```clojure
;; Preview hiccup in browser
(dispatch [[::tsain/preview [:h1 "Hello World"]]])

;; Commit to component library
(dispatch [[::tsain/commit :my-card {:description "Card component"}]])
```

CSS hot-reloads automatically when editing `resources/public/styles.css`.

## Claude Code Integration

Generated projects are optimized for Claude-assisted development:

- **CLAUDE.md** - Comprehensive documentation (~800 lines) covering the entire effect ecosystem, patterns, and conventions
- **Paren repair hooks** - Automatic Clojure formatting on file edits
- **clojure-eval skill** - REPL evaluation via nREPL
- **component-iterate skill** - Workflow for developing UI components

## Background

This template emerged from the [tsain](https://github.com/brianium/tsain) project's development, which needed a way to scaffold new applications using the sandestin ecosystem. The name "ascolais" comes from Jack Vance's *The Dying Earth* series, following the naming convention of sibling libraries.

## License

MIT
