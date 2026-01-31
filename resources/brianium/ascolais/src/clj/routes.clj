(ns {{top/ns}}.routes
  "Application routes."
  (:require [{{top/ns}}.views.layout :as layout]
            [{{top/ns}}.auth :as auth]
            [{{top/ns}}.fx.example :as example]
            [ascolais.sfere :as sfere]
            [ascolais.twk :as twk]
            [dev.onionpancakes.chassis.core :as c]
            [ring.util.response :as response]))

(defn sse-home
  "Establish persistent SSE connection for home page."
  [{:keys [signals]}]
  (let [session-id (:sessionId signals)]
    {::sfere/key [:page "home" session-id]
     ::twk/fx [[::twk/patch-signals {:connected true}]]}))

;; ==========================================================================
;; Auth Route Handlers
;; ==========================================================================

(defn auth-google
  "Initiate Google OAuth flow."
  [request]
  (let [config (-> request :reitit.core/match :data :auth-config)]
    (auth/auth-start (:client-id config) request)))

(defn auth-callback
  "Handle OAuth callback from Google.
   Creates or finds user, sets session.

   Customize on-success to implement your user management logic:
   - Find or create user in database
   - Link anonymous tokens if applicable
   - Set session with user-id"
  [request]
  (let [config (-> request :reitit.core/match :data :auth-config)]
    (auth/auth-callback
     (:client-id config)
     (:client-secret config)
     request
     ;; on-success - receives Google user info
     ;; Customize this for your app's user management
     (fn [google-user]
       ;; TODO: Find or create user in database
       ;; TODO: Set session with user-id
       ;; For now, just store Google user info in session
       (-> (response/redirect "/")
           (assoc :session {:google-user google-user})))
     ;; on-error
     (fn [{:keys [type error]}]
       ;; Log error for monitoring
       (binding [*out* *err*]
         (prn {:event :oauth-error
               :type type
               :error error
               :timestamp (java.time.Instant/now)}))
       ;; Show error page
       {:status 200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body (c/html (layout/auth-error-page type error))}))))

(defn auth-logout
  "Clear session and redirect home."
  [_request]
  (auth/auth-logout nil))

(defn greet
  "Greet handler - dispatches the greet action."
  [{:keys [signals]}]
  {::twk/fx [[::example/greet (:name signals)]]})

(defn routes
  "Application routes."
  []
  [["/" {:get {:handler (fn [_] {:body (layout/home-page)})}}]

   ["/about" {:get {:handler (fn [_] {:body (layout/about-page)})}}]

   ["/sse/home" {:get {:handler sse-home}}]

   ["/api/greet" {:post {:handler greet}}]

   ;; Auth routes
   ["/auth/google" {:get {:handler auth-google}}]
   ["/auth/callback" {:get {:handler auth-callback}}]
   ["/auth/logout" {:get {:handler auth-logout}}]])
