(ns firestone.client.kth.edn-api
  (:require [firestone.construct :refer [create-game]]
            [firestone.client.kth.mapper :refer [state->client-state]]
            [firestone.core-api :refer [end-turn
                                        play-minion-card]]))

(defonce state-atom (atom nil))

(defn response
  "This is function aimed to be instrumented to satisfy spec."
  [client-state]
  client-state)

(defn create-game!
  []
  (let [state (reset! state-atom (create-game))]
    (time (response (state->client-state state)))))

(defn play-minion-card!
  [player-id card-id position]
  (time (response (state->client-state (swap! state-atom play-minion-card player-id card-id position)))))
