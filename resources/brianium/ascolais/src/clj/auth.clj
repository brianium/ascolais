(ns {{top/ns}}.auth
  "Google OAuth2 authentication.

   Implements the OAuth2 authorization code flow:
   1. /auth/google - redirects to Google with client_id, scopes, state
   2. Google redirects back with ?code=...&state=...
   3. /auth/callback - validates state, exchanges code for tokens, fetches user info
   4. /auth/logout - clears session"
  (:require [org.httpkit.client :as http]
            [charred.api :as json]
            [ring.util.response :as response])
  (:import [java.security SecureRandom]
           [java.util Base64]))

;; OAuth configuration
(def google-auth-url "https://accounts.google.com/o/oauth2/v2/auth")
(def google-token-url "https://oauth2.googleapis.com/token")
(def google-userinfo-url "https://www.googleapis.com/oauth2/v2/userinfo")
(def scopes "openid email profile")

(defn generate-state
  "Generate a cryptographically random state parameter for CSRF protection.
   Returns a URL-safe base64 string."
  []
  (let [random (SecureRandom.)
        bytes (byte-array 32)]
    (.nextBytes random bytes)
    (.encodeToString (Base64/getUrlEncoder) bytes)))

(defn build-auth-url
  "Build Google OAuth authorization URL.
   redirect-uri should be the full callback URL (e.g., http://localhost:3000/auth/callback)
   state is a CSRF token that will be returned in the callback"
  [client-id redirect-uri state]
  (str google-auth-url
       "?client_id=" (java.net.URLEncoder/encode client-id "UTF-8")
       "&redirect_uri=" (java.net.URLEncoder/encode redirect-uri "UTF-8")
       "&response_type=code"
       "&scope=" (java.net.URLEncoder/encode scopes "UTF-8")
       "&access_type=offline"
       "&prompt=consent"
       "&state=" (java.net.URLEncoder/encode state "UTF-8")))

(defn exchange-code
  "Exchange authorization code for access token.
   Returns {:access_token \"...\" :token_type \"Bearer\" ...} or nil on error."
  [client-id client-secret redirect-uri code]
  (let [response @(http/post google-token-url
                             {:form-params {"client_id" client-id
                                            "client_secret" client-secret
                                            "redirect_uri" redirect-uri
                                            "code" code
                                            "grant_type" "authorization_code"}})]
    (if (= 200 (:status response))
      (json/read-json (:body response) :key-fn keyword)
      ;; Log failed token exchange (don't log secrets)
      (do
        (binding [*out* *err*]
          (prn {:event :oauth-token-exchange-failed
                :status (:status response)
                :timestamp (java.time.Instant/now)}))
        nil))))

(defn fetch-user-info
  "Fetch user profile from Google using access token.
   Returns {:id \"...\" :email \"...\" :name \"...\" :picture \"...\"} or nil on error."
  [access-token]
  (let [response @(http/get google-userinfo-url
                            {:headers {"Authorization" (str "Bearer " access-token)}})]
    (if (= 200 (:status response))
      (json/read-json (:body response) :key-fn keyword)
      ;; Log failed user info fetch
      (do
        (binding [*out* *err*]
          (prn {:event :oauth-userinfo-fetch-failed
                :status (:status response)
                :timestamp (java.time.Instant/now)}))
        nil))))

(defn derive-redirect-uri
  "Derive the OAuth callback URI from the request."
  [request]
  (let [scheme (name (or (some-> request :headers (get "x-forwarded-proto") keyword)
                         (:scheme request)))
        host (or (get-in request [:headers "x-forwarded-host"])
                 (get-in request [:headers "host"]))]
    (str scheme "://" host "/auth/callback")))

;; Route handlers

(defn auth-start
  "Initiate Google OAuth flow. Redirects to Google.
   Generates a state parameter for CSRF protection and stores it in session."
  [client-id request]
  (let [redirect-uri (derive-redirect-uri request)
        state (generate-state)]
    (-> (response/redirect (build-auth-url client-id redirect-uri state))
        (assoc :session (merge (:session request) {:oauth-state state})))))

(defn auth-callback
  "Handle OAuth callback from Google.
   - Validates state parameter (CSRF protection)
   - Exchanges code for tokens
   - Fetches user info
   - Calls on-success with google-user map
   - Returns response (caller handles user creation, session)"
  [client-id client-secret request on-success on-error]
  (let [redirect-uri (derive-redirect-uri request)
        code (get-in request [:query-params "code"])
        error (get-in request [:query-params "error"])
        returned-state (get-in request [:query-params "state"])
        expected-state (get-in request [:session :oauth-state])]
    (cond
      ;; User denied or other OAuth error
      error
      (on-error {:type :denied :error error})

      ;; Validate state parameter (CSRF protection)
      (or (nil? returned-state)
          (nil? expected-state)
          (not= returned-state expected-state))
      (on-error {:type :invalid :error "Invalid state parameter"})

      ;; Missing code
      (nil? code)
      (on-error {:type :invalid :error "Missing authorization code"})

      ;; Exchange code for tokens
      :else
      (if-let [tokens (exchange-code client-id client-secret redirect-uri code)]
        (if-let [user-info (fetch-user-info (:access_token tokens))]
          (on-success user-info)
          (on-error {:type :invalid :error "Failed to fetch user info"}))
        (on-error {:type :invalid :error "Failed to exchange code"})))))

(defn auth-logout
  "Clear session and redirect to home."
  [_request]
  (-> (response/redirect "/")
      (assoc :session nil)))
