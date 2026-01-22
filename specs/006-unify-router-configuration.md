# 006: Unify Router Configuration

**Status:** Draft

## Summary

Refactor the router configuration so that the dev router extends the production router through parameters rather than duplicating the implementation. This eliminates the separate `router` function in dev/config.clj and makes the relationship explicit: dev is production + extra routes.

## Background

Currently both config.clj and dev/config.clj define their own `router` functions:

**Production (config.clj:46-53):**
```clojure
(defn router
  [{:keys [dispatch routes]}]
  (rr/router
    (into routes (kaiin/routes dispatch))
    {:data {:middleware [[wrap-params]
                         [wrap-keyword-params]
                         [(twk/with-datastar ds-hk/->sse-response dispatch)]]}}))
```

**Dev (dev/config.clj:34-42):**
```clojure
(defn router
  [{:keys [dispatch routes]}]
  (rr/router
    (into routes (concat (tsain/routes) (kaiin/routes dispatch)))
    {:data {:middleware [[wrap-params]
                         [wrap-keyword-params]
                         [wrap-resource "public"]
                         [(twk/with-datastar ds-hk/->sse-response dispatch)]]}}))
```

Issues:
1. **Duplication**: The middleware stack is copied between files
2. **Missing production feature**: Production should also serve static resources via `wrap-resource`
3. **Implicit relationship**: Nothing in the code shows that dev extends production
4. **Maintenance burden**: Changes to the middleware stack require updates in two places

## Target Design

Production router accepts an optional `:extra-routes` parameter. Dev config simply passes tsain routes as extra routes - no separate router function needed.

```clojure
;; Production: extensible router with static file serving
(defn router
  [{:keys [dispatch routes extra-routes]}]
  ...)

;; Dev: just adds extra routes
::app/router {:dispatch ...
              :routes ...
              :extra-routes (tsain/routes)}
```

## Affected Files

### `resources/brianium/ascolais/src/clj/config.clj`

Add `wrap-resource` require and update router function:

```clojure
;; Before (requires section)
(:require [{{top/ns}}.routes :as routes]
          ...
          [ring.middleware.params :refer [wrap-params]]
          [ring.middleware.keyword-params :refer [wrap-keyword-params]]
          ...)

;; After (requires section)
(:require [{{top/ns}}.routes :as routes]
          ...
          [ring.middleware.params :refer [wrap-params]]
          [ring.middleware.keyword-params :refer [wrap-keyword-params]]
          [ring.middleware.resource :refer [wrap-resource]]
          ...)
```

```clojure
;; Before
(defn router
  "Create reitit router with middleware."
  [{:keys [dispatch routes]}]
  (rr/router
    (into routes (kaiin/routes dispatch))
    {:data {:middleware [[wrap-params]
                         [wrap-keyword-params]
                         [(twk/with-datastar ds-hk/->sse-response dispatch)]]}}))

;; After
(defn router
  "Create reitit router with middleware.

   Options:
   - :dispatch - sandestin dispatch fn (required)
   - :routes - base application routes (required)
   - :extra-routes - additional routes prepended before kaiin routes (optional)"
  [{:keys [dispatch routes extra-routes]}]
  (rr/router
    (into routes (concat extra-routes (kaiin/routes dispatch)))
    {:data {:middleware [[wrap-params]
                         [wrap-keyword-params]
                         [wrap-resource "public"]
                         [(twk/with-datastar ds-hk/->sse-response dispatch)]]}}))
```

### `resources/brianium/ascolais/dev/src/clj/dev/config.clj`

Remove the router function and simplify requires. Update config to use production router directly.

```clojure
;; Before (requires section)
(ns dev.config
  (:require [{{top/ns}}.config :as app]
            [{{top/ns}}.routes :as routes]
            [ascolais.tsain :as tsain]
            [ascolais.kaiin :as kaiin]
            [integrant.core :as ig]
            [reitit.ring :as rr]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ascolais.twk :as twk]
            [starfederation.datastar.clojure.adapter.http-kit :as ds-hk]
            [nextjournal.beholder :as beholder]))

;; After (requires section - much shorter)
(ns dev.config
  (:require [{{top/ns}}.config :as app]
            [{{top/ns}}.routes :as routes]
            [ascolais.tsain :as tsain]
            [integrant.core :as ig]
            [nextjournal.beholder :as beholder]))
```

```clojure
;; Before (component initializers section)
(defn tsain-registry
  "Tsain sandbox registry."
  [_opts]
  (tsain/registry))

(defn file-watcher
  "CSS hot-reload file watcher."
  [{:keys [dispatch paths]}]
  (beholder/watch
    (fn [_event]
      (dispatch [[::tsain/reload-css]]))
    (first paths)))

(defn router
  "Development router with tsain routes."
  [{:keys [dispatch routes]}]
  (rr/router
    (into routes (concat (tsain/routes) (kaiin/routes dispatch)))
    {:data {:middleware [[wrap-params]
                         [wrap-keyword-params]
                         [wrap-resource "public"]
                         [(twk/with-datastar ds-hk/->sse-response dispatch)]]}}))

;; After (router function removed)
(defn tsain-registry
  "Tsain sandbox registry."
  [_opts]
  (tsain/registry))

(defn file-watcher
  "CSS hot-reload file watcher."
  [{:keys [dispatch paths]}]
  (beholder/watch
    (fn [_event]
      (dispatch [[::tsain/reload-css]]))
    (first paths)))
```

```clojure
;; Before (config map)
(def config
  (merge
    app/config

    {::tsain-registry {}

     ::app/dispatch {:datasource (ig/ref ::app/datasource)
                     :store (ig/ref ::app/store)
                     :registries [(ig/ref :{{top/ns}}.fx.example/registry)
                                  (ig/ref ::tsain-registry)]}

     ::router {:dispatch (ig/ref ::app/dispatch)
               :routes (routes/routes)}

     ::app/router (ig/ref ::router)

     ::file-watcher {:dispatch (ig/ref ::app/dispatch)
                     :paths ["resources/public"]}}))

;; After (config map - no ::router indirection)
(def config
  (merge
    app/config

    {::tsain-registry {}

     ::app/dispatch {:datasource (ig/ref ::app/datasource)
                     :store (ig/ref ::app/store)
                     :registries [(ig/ref :{{top/ns}}.fx.example/registry)
                                  (ig/ref ::tsain-registry)]}

     ::app/router {:dispatch (ig/ref ::app/dispatch)
                   :routes (routes/routes)
                   :extra-routes (tsain/routes)}

     ::file-watcher {:dispatch (ig/ref ::app/dispatch)
                     :paths ["resources/public"]}}))
```

### `resources/brianium/ascolais/build/CLAUDE.md`

Update the architecture documentation to reflect the unified router pattern. In the "Development Setup" or architecture section, add a note about router extension:

```markdown
### Router Extension

The production router accepts an `:extra-routes` parameter, allowing dev to extend
it without duplication:

```clojure
;; Production
::router {:dispatch (ig/ref ::dispatch)
          :routes (routes/routes)}

;; Dev adds tsain routes
::app/router {:dispatch (ig/ref ::app/dispatch)
              :routes (routes/routes)
              :extra-routes (tsain/routes)}
```
```

## Implementation Notes

1. **Static file serving in production**: Adding `wrap-resource "public"` to production enables serving CSS, JS, images, and other static assets. This is standard for web applications.

2. **Middleware order**: The order is important:
   - `wrap-params` / `wrap-keyword-params` - parse request params first
   - `wrap-resource` - serve static files (short-circuits if file exists)
   - `twk/with-datastar` - handle SSE/Datastar requests last

3. **Route order**: `extra-routes` are prepended before `kaiin/routes`. This allows dev routes (like `/sandbox`) to take precedence if there were any conflicts.

4. **Removed indirection**: The dev config previously used `::router` → `::app/router` indirection. Now it directly overrides `::app/router`, which is cleaner.

5. **Reduced requires**: dev/config.clj no longer needs reitit, ring middleware, twk, or datastar adapter requires since it doesn't define its own router.

## Verification

After changes:

1. Generate a new project from the template
2. **Test production mode**:
   - Start without dev alias: `clj -M -m {{top/ns}}.core`
   - Verify static files are served (e.g., `/styles.css` returns CSS)
   - Verify routes work normally
3. **Test dev mode**:
   - Start with dev: `clj -M:dev`, then `(dev)` and `(start)`
   - Verify `/sandbox` route works (tsain routes loaded)
   - Verify static files still work
   - Verify hot-reload still works
4. Confirm dev/config.clj has no router-related requires (reitit, ring.middleware.*, twk, ds-hk)
