(ns de.contentreich.ecm.testcore
  (:require [de.otto.tesla.system :as sys]
            [me.lomin.component-restart :as restart]
            [de.contentreich.ecm.google-system :as system])
  (:gen-class))

(defn -main [& args]
  (println "Starting the system. With restart watcher.")
  (let [started (sys/start (system/vision-system {:property-file-preferred true}))]
    (restart/watch (var -main) started)))
