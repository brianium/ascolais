(ns dev.config
  "Development system configuration.
   Extends app config with dev-only components."
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

(defn dev-router
  "Development router with tsain routes."
  [{:keys [dispatch routes]}]
  (rr/router
    (into routes (concat (tsain/routes) (kaiin/routes dispatch)))
    {:data {:middleware [[wrap-params]
                         [wrap-keyword-params]
                         [wrap-resource "public"]
                         [(twk/with-datastar ds-hk/->sse-response dispatch)]]}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Integrant Methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod ig/init-key ::tsain-registry [_ opts]
  (tsain-registry opts))

(defmethod ig/init-key ::file-watcher [_ opts]
  (file-watcher opts))

(defmethod ig/halt-key! ::file-watcher [_ watcher]
  (beholder/stop watcher))

(defmethod ig/init-key ::router [_ opts]
  (dev-router opts))

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

     ;; Use dev router with tsain routes
     ::router {:dispatch (ig/ref ::app/dispatch)
               :routes (routes/routes)}

     ::app/router (ig/ref ::router)

     ;; File watcher for CSS hot-reload
     ::file-watcher {:dispatch (ig/ref ::app/dispatch)
                     :paths ["resources/public"]}}))
