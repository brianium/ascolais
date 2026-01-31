(ns dev.config
  "Development system configuration.
   Extends app config with dev-only components."
  (:require [{{top/ns}}.config :as app]
            [{{top/ns}}.routes :as routes]
            [ascolais.sfere :as sfere]
            [ascolais.tsain :as tsain]
            [ascolais.tsain.routes :as tsain.routes]
            [ascolais.twk :as twk]
            [integrant.core :as ig]
            [nextjournal.beholder :as beholder]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dev Component Initializers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tsain-registry
  "Tsain sandbox registry."
  [_opts]
  (tsain/registry))

(defn- wrap-dispatch
  "Middleware that adds dispatch to the request."
  [dispatch]
  (fn [handler]
    (fn [request]
      (handler (assoc request :dispatch dispatch)))))

(defn tsain-routes
  "Tsain sandbox routes.
   Requires dispatch and tsain-registry to compute routes.
   Adds dispatch middleware and filters root redirect."
  [{:keys [dispatch tsain-registry]}]
  (let [state (::tsain/state tsain-registry)
        config (::tsain/config tsain-registry)
        dispatch-mw (wrap-dispatch dispatch)]
    (->> (tsain.routes/routes dispatch state config)
         (remove #(= "/" (first %)))
         (mapv (fn [[path data]]
                 [path (update data :middleware (fnil conj []) dispatch-mw)])))))

(def ^:private reload-css-script
  "document.querySelectorAll('link[rel=stylesheet]').forEach(l => l.href = l.href.split('?')[0] + '?' + Date.now())")

(defn file-watcher
  "CSS hot-reload file watcher."
  [{:keys [dispatch paths]}]
  (beholder/watch
    (fn [_event]
      (dispatch [[::sfere/broadcast {:pattern [:* :*]}
                  [::twk/execute-script reload-css-script]]]))
    (first paths)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Integrant Halt Methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod ig/halt-key! ::file-watcher [_ watcher]
  (beholder/stop watcher))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dev Configuration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn config
  "Build dev system configuration.
   Extends app config with dev-only components."
  []
  (merge
    (app/config)

    ;; Add dev-only components
    {::tsain-registry {}

     ;; Override dispatch to include tsain
     ::app/dispatch {:datasource (ig/ref ::app/datasource)
                     :store (ig/ref ::app/store)
                     :registries [(ig/ref :{{top/ns}}.fx.example/registry)
                                  (ig/ref ::tsain-registry)]}

     ;; Tsain sandbox routes (with dispatch middleware)
     ::tsain-routes {:dispatch (ig/ref ::app/dispatch)
                     :tsain-registry (ig/ref ::tsain-registry)}

     ;; Extend production router with tsain routes
     ::app/router {:dispatch (ig/ref ::app/dispatch)
                   :routes (routes/routes)
                   :extra-routes (ig/ref ::tsain-routes)}

     ;; File watcher for CSS hot-reload
     ::file-watcher {:dispatch (ig/ref ::app/dispatch)
                     :paths ["resources/public"]}}))
