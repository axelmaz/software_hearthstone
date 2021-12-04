(ns firestone.client.kth.instrumentation
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as spec-test]
            [firestone.client.kth.edn-api]
            [firestone.client.kth.spec]))

(s/fdef firestone.client.kth.edn-api/response
        :args (s/coll-of :firestone.client.kth.spec/game-states))

(spec-test/instrument 'firestone.client.kth.edn-api/response)