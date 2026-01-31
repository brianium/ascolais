(ns {{top/ns}}.config
  "Integrant system configuration."
  (:require [{{top/ns}}.routes :as routes]
            [{{top/ns}}.secrets :as secrets]
            [{{top/ns}}.fx.example :as example]
            [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ascolais.manse :as manse]
            [integrant.core :as ig]
            [reitit.ring :as rr]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
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
   - :auth-config - OAuth config map {:client-id ... :client-secret ... :session-secret ...}
   - :extra-routes - additional routes to merge (optional, used by dev for tsain)
   - :production? - if true, sets secure cookie flag (requires HTTPS)"
  [{:keys [dispatch routes auth-config extra-routes production?]}]
  (let [session-store (when-let [secret (:session-secret auth-config)]
                        ;; Support both raw 16-char strings and base64-encoded keys
                        (let [key-bytes (if (= 16 (count secret))
                                          (.getBytes secret "UTF-8")
                                          (.decode (java.util.Base64/getDecoder) secret))]
                          (cookie-store {:key key-bytes})))
        cookie-attrs (cond-> {:http-only true
                              :same-site :lax
                              :max-age 2592000}  ;; 30 days
                       production? (assoc :secure true))]
    (rr/router
      (into routes extra-routes)
      {:data {:dispatch dispatch
              :auth-config auth-config
              :middleware (cond-> [[wrap-params]
                                   [wrap-keyword-params]]
                            ;; Add session middleware if configured
                            session-store
                            (conj [wrap-session {:store session-store
                                                 :cookie-name "{{main}}-session"
                                                 :cookie-attrs cookie-attrs}])
                            ;; Add datastar middleware last
                            true
                            (conj [(twk/with-datastar ds-hk/->sse-response dispatch)]))}})))

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

(defn config
  "Build system configuration.
   Loads secrets fresh on each call for REPL reloadability."
  []
  (let [secrets (secrets/load-secrets)]
    {::datasource {:jdbc-url (get secrets "JDBC_URL" "jdbc:postgresql://localhost:5432/{{main}}_dev")
                   :username (get secrets "DB_USERNAME" "postgres")
                   :password (get secrets "DB_PASSWORD" "postgres")}

     ::store {:type :caffeine
              :duration-ms 1800000}

     ::example/registry {:datasource (ig/ref ::datasource)}

     ::dispatch {:datasource (ig/ref ::datasource)
                 :store (ig/ref ::store)
                 :registries [(ig/ref ::example/registry)]}

     ::router {:dispatch (ig/ref ::dispatch)
               :routes (routes/routes)
               ;; Auth config - session secret must be exactly 16 bytes for AES encryption
               :auth-config {:client-id (get secrets "GOOGLE_CLIENT_ID")
                             :client-secret (get secrets "GOOGLE_CLIENT_SECRET")
                             :session-secret (or (get secrets "SESSION_SECRET")
                                                 "{{main}}-dev-key!")}
               ;; Set secure cookie flag in production (requires HTTPS)
               :production? (= "production" (System/getenv "{{sanitized/env}}_ENV"))}

     ::handler {:router (ig/ref ::router)}

     ::server {:handler (ig/ref ::handler)
               :port 3000}}))
