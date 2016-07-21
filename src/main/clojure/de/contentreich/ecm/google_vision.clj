(ns de.contentreich.ecm.google-vision
  (:import (com.google.api.services.vision.v1 Vision
                                              VisionScopes
                                              model.AnnotateImageRequest)
            (com.google.api.client.googleapis.auth.oauth2.GoogleCredential)
            (com.google.api.client.googleapis.javanet.GoogleNetHttpTransport)
            (com.google.api.client.json JsonFactory
                                        jackson2.JacksonFactory)
            (com.google.api.services.translate.Translate))
  (:require [com.stuartsierra.component :as component]
            [de.otto.tesla.stateful.metering :as metering]
            [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.app-status :as app-status]
            [de.otto.status :as s]
            [metrics.timers :as timers]))


;; status turns warning after 10 calculations. Because license expired.
(defn- status-fun
  [calculations]
  (if (> 500 @calculations)
    (s/status-detail :calculator :ok "less than 500 calculations performed")
    (s/status-detail :calculator :warning "more than 500 calculations perormed. Renew license.")))

(defprotocol PubGoogleVision
  (calculations [self])
  (calculate! [self input]))

(defrecord GoogleVision [fun]
  component/Lifecycle
  (start [self]
    (log/info "-> starting google vision.")
    (let [new-self
          (assoc self
            :timer (metering/timer! (:metering self) "visions")
            :calculations (atom 0)
            :fun fun)]
      (app-status/register-status-fun (:app-status new-self)
                                      (partial status-fun (:calculations new-self)))
      new-self))

  (stop [self]
    (log/info "<- stopping google vision.")
    (reset! (:calculations self) 0)
    self)
  PubGoogleVision
  (calculations [self] @(:calculations self))
  (calculate! [self input]
    (timers/time! (:timer self)
                  (swap! (:calculations self) inc)
                  ((:fun self) self input))))

(defn new-vision [fun] (map->GoogleVision {:fun fun}))

