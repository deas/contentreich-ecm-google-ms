(ns de.contentreich.ecm.google-system
  (:import (es.keensoft.alfresco.google GoogleVisionWorker
                                        SafeSearchConfig SafeSearchConfig$Likelihood))
  (:require [de.otto.tesla.system :as system]
            [de.contentreich.ecm.google-vision :as vision]
            [de.contentreich.ecm.page :as page]
            [clojure.tools.logging :as log]
            [de.otto.tesla.serving-with-jetty :as jetty]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))

(defn create-vision-service [self]
  (let [cfg (get-in self [:config :config])]
    (doto (GoogleVisionWorker.)
      (.setApplicationName (:vision-app-name cfg))
      (.setCredentialsJsonPath (:vision-credentials-json cfg))
        (.setMaxResults (. Integer parseInt  (:vision-max-results cfg)))
      (.setTranslateApiKey (:vision-translate-api-key cfg))
      (.setSafeSearchConfig
        (doto (SafeSearchConfig.)
          (.setAdultLikelihoodLevel (SafeSearchConfig$Likelihood/valueOf SafeSearchConfig$Likelihood (:vision-safe-search-adult cfg)))
          (.setViolenceLikelihoodLevel (SafeSearchConfig$Likelihood/valueOf SafeSearchConfig$Likelihood (:vision-safe-search-violence cfg)))
          ))
      (.setTranslateLanguage (:vision-translate-language cfg)))))


(defn view [params input]
  (let [vs (create-vision-service params)
        is (slurp-bytes input)]
    (log/info "Calling google service")
    {:body (into {} (.execute vs is))}))

(defn vision-system [runtime-config]
  (-> (system/base-system (merge {:name "vision-service"} runtime-config))
      (assoc
        :calculator (component/using (vision/new-vision view) [:metering :app-status :config])
        :example-page (component/using (page/new-page) [:handler :calculator :app-status])
        )
      (jetty/add-server)))

(defn -main
  "starts up the production system."
  [& args]
  (system/start (vision-system {:property-file-preferred true})))
