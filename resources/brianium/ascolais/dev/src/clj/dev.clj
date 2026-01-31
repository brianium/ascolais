(ns dev
  (:require [clj-reload.core :as reload]
            [portal.api :as p]
            [ascolais.sandestin :as s]
            [ascolais.tsain :as tsain]
            [dev.config :as config]
            [integrant.core :as ig]
            [ragtime.jdbc :as ragtime-jdbc]
            [ragtime.repl :as ragtime]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Portal Setup (reload-safe)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce portal (p/open))
(defonce _setup-tap (add-tap #'p/submit))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; System State
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce ^:dynamic *system* nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; System Lifecycle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start
  "Start the development system."
  ([] (start (config/config)))
  ([config]
   (alter-var-root #'*system* (constantly (ig/init config)))
   (tap> {:event :system/started :keys (keys *system*)})
   :started))

(defn stop
  "Stop the development system."
  []
  (when *system*
    (ig/halt! *system*)
    (alter-var-root #'*system* (constantly nil))
    (tap> {:event :system/stopped})
    :stopped))

(defn reload
  "Reload changed namespaces."
  []
  (reload/reload))

(defn restart
  "Full restart: stop, reload, and start."
  []
  (stop)
  (reload)
  (start))

;; clj-reload hooks
(defn before-ns-unload []
  (stop))

(defn after-ns-reload []
  (start))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dispatch Access
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatch
  "Get the sandestin dispatch function from the running system.
   Use this for both dispatching effects and discovery:

   Dispatch effects:
     (dispatch [[::tsain/preview [:h1 \"Hello\"]]])

   Discovery:
     (s/describe (dispatch))
     (s/sample (dispatch) ::tsain/preview)"
  ([]
   (:{{top/ns}}.config/dispatch *system*))
  ([effects]
   (when-let [d (:{{top/ns}}.config/dispatch *system*)]
     (d effects)))
  ([system effects]
   (when-let [d (:{{top/ns}}.config/dispatch *system*)]
     (d system effects)))
  ([system dispatch-data effects]
   (when-let [d (:{{top/ns}}.config/dispatch *system*)]
     (d system dispatch-data effects))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sandestin Discovery Aliases
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def describe
  "List and inspect registered effects/actions.

  Usage:
    (describe (dispatch))              ;; List all items
    (describe (dispatch) :effects)     ;; List effects only
    (describe (dispatch) ::tsain/preview)  ;; Inspect specific effect"
  s/describe)

(def sample
  "Generate example invocations.

  Usage:
    (sample (dispatch) ::tsain/preview)     ;; One sample
    (sample (dispatch) ::tsain/preview 3)   ;; Multiple samples"
  s/sample)

(def grep
  "Search registry by pattern.

  Usage:
    (grep (dispatch) \"component\")
    (grep (dispatch) #\"preview|commit\")"
  s/grep)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Database Migrations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn migration-config
  "Get ragtime migration configuration."
  []
  (let [ds (:{{top/ns}}.config/datasource *system*)]
    {:datastore (ragtime-jdbc/sql-database {:datasource ds})
     :migrations (ragtime-jdbc/load-resources "migrations")}))

(defn migrate!
  "Apply pending database migrations."
  []
  (ragtime/migrate (migration-config)))

(defn rollback!
  "Roll back last database migration."
  []
  (ragtime/rollback (migration-config)))
