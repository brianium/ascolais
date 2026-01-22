# 005: Move Chassis Aliases to Production Namespace

**Status:** Draft

## Summary

Move chassis alias definitions from the dev-only `sandbox.ui` namespace to a production namespace `{{top/ns}}.views.components`. This allows components developed in the sandbox to be used directly in production without copying code between dev and src trees.

## Background

Currently chassis aliases are defined in `dev/src/clj/sandbox/ui.clj`:

```
dev/src/clj/sandbox/
  ui.clj               # Chassis aliases (dev-only)
  views.clj            # Requires ui.clj to ensure registration
```

This creates friction:
1. Components developed in the sandbox can't be used in production without manually copying the alias definitions
2. The `sandbox.ui` namespace suggests these are sandbox-specific, not production-ready
3. Developers must maintain two copies of aliases if they want both sandbox iteration and production use

## Target Structure

```
src/clj/{{top/file}}/views/
  layout.clj           # Base layout (requires components)
  components.clj       # Chassis alias definitions (NEW)
```

The entire `dev/src/clj/sandbox/` directory is removed. The `sandbox/views.clj` file existed only to ensure `sandbox/ui.clj` was loaded, but since `layout.clj` now requires `components.clj`, aliases are registered through the normal application startup chain (routes → layout → components).

## Affected Files

### `resources/brianium/ascolais/src/clj/views/components.clj` (NEW)

Create new file with chassis alias definitions:

```clojure
(ns {{top/ns}}.views.components
  "Chassis alias definitions for reusable UI components.

   Define component structure here, use namespaced attributes for config props:
   - Namespaced attrs (e.g., :card/title) are elided from HTML output
   - Non-namespaced attrs pass through to the element

   Example:
     (defmethod c/resolve-alias ::card
       [_ attrs _]
       (let [{:card/keys [title description]} attrs]
         [:div.card attrs
          [:h3.card-title title]
          [:p.card-description description]]))"
  (:require [dev.onionpancakes.chassis.core :as c]))

;; Example component alias
(defmethod c/resolve-alias ::example-card
  [_ attrs _]
  (let [{:example-card/keys [title description]} attrs]
    [:div.example-card attrs
     [:h3.example-card-title title]
     [:p.example-card-description description]]))
```

### `resources/brianium/ascolais/src/clj/views/layout.clj`

Add require for components namespace to ensure aliases are registered:

```clojure
;; Before
(ns {{top/ns}}.views.layout
  "Base layout and page shells."
  (:require [dev.onionpancakes.chassis.core :as c]))

;; After
(ns {{top/ns}}.views.layout
  "Base layout and page shells."
  (:require [dev.onionpancakes.chassis.core :as c]
            [{{top/ns}}.views.components]))
```

### `resources/brianium/ascolais/dev/src/clj/sandbox/` (DELETE)

Remove the entire `sandbox/` directory, including:
- `sandbox/ui.clj` - contents moved to `views/components.clj`
- `sandbox/views.clj` - no longer needed; aliases are registered via layout → components chain

### `resources/brianium/ascolais/build/tsain.edn`

Update ui-namespace to point to production namespace:

```clojure
;; Before
{:ui-namespace sandbox.ui
 :components-file "dev/resources/components.edn"
 :stylesheet "resources/public/styles.css"
 :port 3000}

;; After
{:ui-namespace {{top/ns}}.views.components
 :components-file "dev/resources/components.edn"
 :stylesheet "resources/public/styles.css"
 :port 3000}
```

Note: This spec assumes 004 is implemented first (stylesheet path change).

### `resources/brianium/ascolais/dev/resources/components.edn`

Update the example component to use the new namespace:

```clojure
;; Before
{:components
 {:example-card
  {:description "Example card component"
   :hiccup [:sandbox.ui/example-card
            {:example-card/title "Example Title"
             :example-card/description "An example card component."}]}}}

;; After
{:components
 {:example-card
  {:description "Example card component"
   :hiccup [:{{top/ns}}.views.components/example-card
            {:example-card/title "Example Title"
             :example-card/description "An example card component."}]}}}
```

### `resources/brianium/ascolais/build/CLAUDE.md`

Update Project Structure section (around line 173-185):

```markdown
;; Before
dev/src/clj/
  user.clj               # REPL initialization
  dev.clj                # Dev namespace
  dev/config.clj         # Dev integrant config
  sandbox/
    ui.clj               # Chassis alias definitions
    views.clj            # Sandbox view re-exports

;; After
dev/src/clj/
  user.clj               # REPL initialization
  dev.clj                # Dev namespace
  dev/config.clj         # Dev integrant config
```

Update "Chassis Alias Conventions" section (around line 545-560):

```markdown
;; Before
Component structure lives in `dev/src/clj/sandbox/ui.clj` as chassis aliases.

;; After
Component structure lives in `src/clj/{{top/file}}/views/components.clj` as chassis aliases.
```

Update example namespace reference:

```markdown
;; Before
;; 1. Define structure in sandbox/ui.clj

;; After
;; 1. Define structure in views/components.clj
```

### `resources/brianium/ascolais/build/README.md`

Update Project Structure section (around line 63-70):

```markdown
;; Before
dev/src/clj/
  user.clj             # REPL initialization
  dev.clj              # Dev namespace (start/stop/dispatch)
  dev/config.clj       # Dev system config (extends app)
  sandbox/
    ui.clj             # Chassis alias definitions
    views.clj          # Sandbox view re-exports

;; After
dev/src/clj/
  user.clj             # REPL initialization
  dev.clj              # Dev namespace (start/stop/dispatch)
  dev/config.clj       # Dev system config (extends app)

src/clj/{{top/file}}/
  ...
  views/
    layout.clj         # Base layout
    components.clj     # Chassis alias definitions
```

### `resources/brianium/ascolais/.claude/skills/component-iterate/SKILL.md`

Update configuration example (around line 16):

```markdown
;; Before
{:ui-namespace sandbox.ui          ;; Where chassis aliases live

;; After
{:ui-namespace {{top/ns}}.views.components  ;; Where chassis aliases live
```

Update "Step 1: Define the Chassis Alias" section (around line 61):

```markdown
;; Before
Before iterating on visuals, define the component structure in the UI namespace (from `:ui-namespace`).

;; After
Before iterating on visuals, define the component structure in the components namespace (from `:ui-namespace`). This is a production namespace, so aliases you define here can be used directly in your application views.
```

Update code example namespace (around line 71):

```markdown
;; Before
;; In the UI namespace

;; After
;; In views/components.clj
```

Update "Step 2: Preview with Alias Invocation" section (around line 92):

```markdown
;; Before
[:sandbox.ui/my-component

;; After
[:{{top/ns}}.views.components/my-component
```

Update all other `sandbox.ui/` references throughout the file to `{{top/ns}}.views.components/`.

## Implementation Notes

1. **Alias registration**: Chassis aliases are registered globally via `defmethod`. The namespace just needs to be loaded; it doesn't matter where it lives on the classpath.

2. **Load order**: `views/layout.clj` requiring `views/components` ensures aliases are registered before any page rendering. Since `routes.clj` requires `layout`, the chain is: routes → layout → components. This happens during normal application startup, so the sandbox (provided by tsain library) automatically has access to all registered aliases.

3. **Template interpolation**: The `{{top/ns}}` placeholder will be replaced with the actual namespace (e.g., `myapp.core`) when a project is generated.

4. **Dependency on 004**: This spec assumes spec 004 (move public to resources) is implemented first, as it references the updated stylesheet path in tsain.edn.

## Verification

After changes:

1. Generate a new project from the template
2. Start the REPL with `clj -M:dev`
3. Run `(dev)` then `(start)`
4. Open `localhost:3000/sandbox` in a browser
5. Preview a component using the new namespace:
   ```clojure
   (dispatch [[::tsain/preview
     [:myapp.views.components/example-card
      {:example-card/title "Test"}]]])
   ```
6. Verify the component renders correctly
7. Add a new alias to `views/components.clj`, run `(reload)`, and verify it's available in the sandbox
8. Use the same alias in a production view (e.g., add to home page) and verify it renders
