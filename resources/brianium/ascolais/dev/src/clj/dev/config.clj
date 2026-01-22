(ns dev.config
  "Development system configuration.
   Extends app config with dev-only components."
  (:require [{{top/ns}}.config :as app]
            [{{top/ns}}.routes :as routes]
            [ascolais.tsain :as tsain]
            [integrant.core :as ig]
            [nextjournal.beholder :as beholder]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dev Component Initializers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Integrant Halt Methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod ig/halt-key! ::file-watcher [_ watcher]
  (beholder/stop watcher))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dev Configuration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def config
  (merge
    app/config

    ;; Add dev-only components
    {::tsain-registry {}

     ;; Override dispatch to include tsain
     ::app/dispatch {:datasource (ig/ref ::app/datasource)
                     :store (ig/ref ::app/store)
                     :registries [(ig/ref :{{top/ns}}.fx.example/registry)
                                  (ig/ref ::tsain-registry)]}

     ;; Extend production router with tsain routes
     ::app/router {:dispatch (ig/ref ::app/dispatch)
                   :routes (routes/routes)
                   :extra-routes (tsain/routes)}

     ;; File watcher for CSS hot-reload
     ::file-watcher {:dispatch (ig/ref ::app/dispatch)
                     :paths ["resources/public"]}}))
