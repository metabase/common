(ns metabase.common.i18n
  (:require [cheshire.generate :as json-gen]
            [clojure.tools.logging :as log]
            [metabase.common.pretty :refer [PrettyPrintable]]
            [puppetlabs.i18n.core :as i18n :refer [available-locales]])
  (:import java.util.Locale))

(defn available-locales-with-names
  "Returns all locale abbreviations and their full names"
  []
  (map (fn [locale] [locale (.getDisplayName (Locale/forLanguageTag locale))]) (available-locales)))

(defn set-locale
  "This sets the local for the instance"
  [locale]
  (Locale/setDefault (Locale/forLanguageTag locale)))

(defn- translate
  "A failsafe version of `i18n/translate`. Will attempt to translate `msg` but if for some reason we're not able
  to (such as a typo in the translated version of the string), log the failure but return the original (untranslated)
  string. This is a workaround for translations that, due to a typo, will fail to parse using Java's message
  formatter."
  [ns-str msg args]
  (try
    (apply i18n/translate ns-str (i18n/user-locale) msg args)
    (catch IllegalArgumentException e
      ;; Not translating this string to prevent an unfortunate stack overflow. If this string happened to be the one
      ;; that had the typo, we'd just recur endlessly without logging an error.
      (log/errorf e "Unable to translate string '%s'" msg)
      msg)))

(defrecord UserLocalizedString [ns-str msg args]
  java.lang.Object
  (toString [_]
    (translate ns-str msg args))
  PrettyPrintable
  (pretty [this]
    (str this)))

(defrecord SystemLocalizedString [ns-str msg args]
  java.lang.Object
  (toString [_]
    (translate ns-str msg args))
  PrettyPrintable
  (pretty [this]
    (str this)))

(defn- localized-to-json
  "Write a UserLocalizedString or SystemLocalizedString to the `json-generator`. This is intended for
  `json-gen/add-encoder`. Ideallys we'd implement those protocols directly as it's faster, but currently that doesn't
  work with Cheshire"
  [localized-string json-generator]
  (json-gen/write-string json-generator (str localized-string)))

(json-gen/add-encoder UserLocalizedString localized-to-json)
(json-gen/add-encoder SystemLocalizedString localized-to-json)

(defmacro tru
  "Similar to `puppetlabs.i18n.core/tru` but creates a `UserLocalizedString` instance so that conversion to the
  correct locale can be delayed until it is needed. The user locale comes from the browser, so conversion to the
  localized string needs to be 'late bound' and only occur when the user's locale is in scope. Calling `str` on the
  results of this invocation will lookup the translated version of the string."
  [msg & args]
  `(UserLocalizedString. ~(namespace-munge *ns*) ~msg ~(vec args)))

(defmacro trs
  "Similar to `puppetlabs.i18n.core/trs` but creates a `SystemLocalizedString` instance so that conversion to the
  correct locale can be delayed until it is needed. This is needed as the system locale from the JVM can be
  overridden/changed by a setting. Calling `str` on the results of this invocation will lookup the translated version
  of the string."
  [msg & args]
  `(SystemLocalizedString. ~(namespace-munge *ns*) ~msg ~(vec args)))
