(ns metabase.common.config
  (:require [clojure.string :as str]
            [environ.core :as environ])
  (:import clojure.lang.Keyword))

(def ^Boolean is-windows?
  "Are we running on a Windows machine?"
  (str/includes? (str/lower-case (System/getProperty "os.name")) "win"))

(def ^:private app-defaults
  "Global application defaults"
  {:mb-run-mode            "prod"
   ;; DB Settings
   :mb-db-type             "h2"
   :mb-db-file             "metabase.db"
   :mb-db-automigrate      "true"
   :mb-db-logging          "true"
   ;; Jetty Settings. Full list of options is available here:
   ;; https://github.com/ring-clojure/ring/blob/master/ring-jetty-adapter/src/ring/adapter/jetty.clj
   :mb-jetty-port          "3000"
   :mb-jetty-join          "true"
   ;; other application settings
   :mb-password-complexity "normal"
   :mb-version-info-url    "http://static.metabase.com/version-info.json"
   ;; session length in minutes (14 days)
   :max-session-age        "20160"
   ;; since PowerShell and cmd.exe don't support ANSI color escape codes or emoji,
   :mb-colorize-logs       (str (not is-windows?))
   ;; disable them by default when running on Windows. Otherwise they're enabled
   :mb-emoji-in-logs       (str (not is-windows?))
   :mb-qp-cache-backend    "db"})


(defn config-str
  "Retrieve value for a single configuration key.  Accepts either a keyword or a string.

   We resolve properties from these places:

   1.  environment variables (ex: MB_DB_TYPE -> :mb-db-type)
   2.  jvm options (ex: -Dmb.db.type -> :mb-db-type)
   3.  hard coded `app-defaults`"
  [k]
  (let [k (keyword k)]
    (or (k environ/env) (k app-defaults))))


;; These are convenience functions for accessing config values that ensures a specific return type
;; TODO - These names are bad. They should be something like  `int`, `boolean`, and `keyword`, respectively.
;;
;; See
;; https://github.com/metabase/metabase/wiki/Metabase-Clojure-Style-Guide#dont-repeat-namespace-alias-in-function-names
;; for discussion
(defn ^Integer config-int
  "Fetch a configuration key and parse it as an integer."
  [k]
  (some-> k config-str Integer/parseInt))

(defn ^Boolean config-bool
  "Fetch a configuration key and parse it as a boolean."
  [k]
  (some-> k config-str Boolean/parseBoolean))

(defn ^Keyword config-kw
  "Fetch a configuration key and parse it as a keyword."
  [k]
  (some-> k config-str keyword))

(def ^Boolean is-dev?
  "Are we running in `dev` mode (i.e. in a REPL or via `lein ring server`)?"
  (= :dev  (config-kw :mb-run-mode)))

(def ^Boolean is-prod?
  "Are we running in `prod` mode (i.e. from a JAR)?"
  (= :prod (config-kw :mb-run-mode)))

(def ^Boolean is-test? "Are we running in `test` mode (i.e. via `lein test`)?"
  (= :test (config-kw :mb-run-mode)))
