(ns de.contentreich.ecm.vision-test
  (:require [clojure.test :refer :all]
            [de.contentreich.ecm.google-vision :as vision]
            [de.contentreich.ecm.google-system :as google-system]
            [de.otto.tesla.util.test-utils :as u]))
#_(deftest ^:unit calculations-should-be-counted
  (testing "Should increment the calc counter on calculations."
    (u/with-started [started (example-system/example-system {})]
                    (let [calculator (:calculator started)
                          result1 (calculating/calculate! calculator "foo")
                          result2 (calculating/calculate! calculator "bar")]
                      (is (= result1 "FOO"))
                      (is (= result2 "BAR"))
                      (is (= (calculating/calculations calculator) 2))))))