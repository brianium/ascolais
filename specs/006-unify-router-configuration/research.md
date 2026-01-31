# Unify Router Configuration - Research

## Problem Statement

Currently both config.clj and dev/config.clj define their own `router` functions:

**Production (config.clj):**
```clojure
(defn router
  [{:keys [dispatch routes]}]
  (rr/router
    (into routes (kaiin/routes dispatch))
    {:data {:middleware [[wrap-params]
                         [wrap-keyword-params]
                         [(twk/with-datastar ds-hk/->sse-response dispatch)]]}}))
```

**Dev (dev/config.clj):**
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
2. **Missing production feature**: Production should also serve static resources
3. **Implicit relationship**: Nothing shows that dev extends production
4. **Maintenance burden**: Changes require updates in two places

## Requirements

### Functional Requirements

1. Production router accepts optional `:extra-routes` parameter
2. Dev config passes tsain routes as extra routes
3. Both prod and dev serve static files via `wrap-resource`

### Non-Functional Requirements

- Cleaner code with less duplication
- Obvious relationship between dev and prod config

## Target Design

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

## Technical Notes

1. **Middleware order** is important:
   - `wrap-params` / `wrap-keyword-params` - parse request params first
   - `wrap-resource` - serve static files (short-circuits if file exists)
   - `twk/with-datastar` - handle SSE/Datastar requests last

2. **Route order**: `extra-routes` are prepended before `kaiin/routes`. This allows dev routes (like `/sandbox`) to take precedence

3. **Removed indirection**: The dev config previously used `::router` → `::app/router` indirection. Now it directly overrides `::app/router`

4. **Reduced requires**: dev/config.clj no longer needs reitit, ring middleware, twk, or datastar adapter requires
