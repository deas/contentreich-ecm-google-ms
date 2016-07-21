(defproject de.contentreich/contentreich-ecm-google-ms "0.1.0"
            :description "Microserivices for Alfresco"
            :url "https://github.com/deas/contentreich-ecm-google-ms"
            :license {:name "Apache License 2.0"
                      :url  "http://www.apache.org/license/LICENSE-2.0.html"}
            :scm {:name "git"
                  :url  "https://github.com/deas/contentreich-ecm-google-ms"}
            :source-paths ["src/main/clojure"]
            :java-source-paths ["src/main/java"]
            :dependencies [[org.clojure/clojure "1.8.0"]
                           [com.google.apis/google-api-services-vision "v1-rev2-1.21.0"]
                           [com.google.api-client/google-api-client "1.21.0"]
                           [com.google.guava/guava "19.0"]
                           [com.google.apis/google-api-services-translate "v2-rev44-1.21.0"]
                           [de.otto/tesla-microservice "0.3.33"]
                           [de.otto/tesla-jetty "0.1.2"]
                           [ring/ring-json "0.4.0"]
                           [ring-cors "0.1.8"]
                           [hiccup "1.0.5"]
                           [org.slf4j/slf4j-api "1.7.16"]
                           [ch.qos.logback/logback-core "1.1.5"]
                           [ch.qos.logback/logback-classic "1.1.5"]]
            :profiles {:dev {:main de.contentreich.ecm.testcore
                             :source-paths ["src/main/clojure" "test"]
                             ;; :resource-paths ["src/main/resources"]
                             :dependencies [[ring-mock "0.1.5"]
                                            [me.lomin/component-restart "0.1.0"]
                                            [hickory "0.6.0"]]}}

            :main de.contentreich.ecm.google-system
  )
