(ns {{top/ns}}.config
  "Integrant system configuration."
  (:require [{{top/ns}}.routes :as routes]
            [{{top/ns}}.fx.example :as example]
            [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ascolais.manse :as manse]
            [ascolais.kaiin :as kaiin]
            [integrant.core :as ig]
            [reitit.ring :as rr]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [starfederation.datastar.clojure.adapter.http-kit :as ds-hk]
            [org.httpkit.server :as http-kit])
  (:import [com.zaxxer.hikari HikariDataSource HikariConfig]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component Initializers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn datasource
  "Create HikariCP connection pool."
  [{:keys [jdbc-url username password]}]
  (let [config (doto (HikariConfig.)
                 (.setJdbcUrl jdbc-url)
                 (.setUsername username)
                 (.setPassword password)
                 (.setMaximumPoolSize 10))]
    (HikariDataSource. config)))

(defn store
  "Create sfere connection store."
  [{:keys [type duration-ms]}]
  (sfere/store {:type type :duration-ms duration-ms}))

(defn dispatch
  "Create composed sandestin dispatch function."
  [{:keys [datasource store registries]}]
  (s/create-dispatch
    (into [(twk/registry)
           (sfere/registry store)
           (manse/registry {:datasource datasource})]
          registries)))

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
                         [(twk/with-datastar ds-hk/->sse-response dispatch)]]}}))

(defn handler
  "Create ring handler from router."
  [{:keys [router]}]
  (rr/ring-handler
    router
    (rr/routes
      (rr/create-resource-handler {:path "/"})
      (rr/create-default-handler))))

(defn server
  "Start HTTP-kit server."
  [{:keys [handler port]}]
  (println (str "Server starting on port " port))
  (http-kit/run-server handler {:port port}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Integrant Halt Methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod ig/halt-key! ::datasource [_ ds]
  (.close ^HikariDataSource ds))

(defmethod ig/halt-key! ::server [_ stop-fn]
  (stop-fn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; System Configuration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def config
  {::datasource {:jdbc-url "jdbc:postgresql://localhost:5432/{{main}}_dev"
                 :username "postgres"
                 :password "postgres"}

   ::store {:type :caffeine
            :duration-ms 1800000}

   ::example/registry {:datasource (ig/ref ::datasource)}

   ::dispatch {:datasource (ig/ref ::datasource)
               :store (ig/ref ::store)
               :registries [(ig/ref ::example/registry)]}

   ::router {:dispatch (ig/ref ::dispatch)
             :routes (routes/routes)}

   ::handler {:router (ig/ref ::router)}

   ::server {:handler (ig/ref ::handler)
             :port 3000}})
