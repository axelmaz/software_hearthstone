(ns firestone.client.kth.edn-api
  (:require [firestone.construct :refer [create-game
                                         create-minion
                                         create-card
                                         create-minion
                                         get-player-id-in-turn]]
            [firestone.client.kth.mapper :refer [state->client-state]]
            [firestone.core-api :refer [end-turn
                                        play-minion-card
                                        attack-minion
                                        play-spell-card]]
            [firestone.core :refer [update-armor]]))

(defonce state-atom (atom nil))

(defn response
  "This is function aimed to be instrumented to satisfy spec."
  [client-state]
  client-state)

(defn create-game!
  []
  (let [state (reset! state-atom (create-game [{:mana 10
                                                :board-entities [(create-minion "Nightblade" :id "n1" :valid-attack-ids ["s2"] :sleepy false :can-attack true)]
                                                :hand [(create-card "Nightblade")
                                                       (create-card "Defender")
                                                       (create-card "Argent Squire")
                                                       (create-card "King Mukla")
                                                       (create-card "Bananas" :event :after-spell-card-played)]
                                                :deck [(create-card "Knife Juggler")]
                                                :hero "Jaina Proudmoore"}
                                               {:mana 10
                                                :board-entities [(create-minion "Snake" :id "s2" :valid-attack-ids ["n1"] :sleepy false :can-attack true)]
                                                :hand [(create-card "Nightblade")
                                                       (create-card "Defender")
                                                       (create-card "Argent Squire")
                                                       (create-card "King Mukla")]
                                                :deck [(create-card "Knife Juggler")]
                                                :hero "Garrosh Hellscream"}]))]
    (time (response (state->client-state state)))))

(defn play-minion-card!
  [player-id card-id position]
  (time (response (state->client-state (swap! state-atom play-minion-card player-id card-id position)))))

(defn end-turn!
  [player-id]
  (time (response (state->client-state (swap! state-atom end-turn player-id)))))

(defn attack!
  [player-id attacker-id defender-id]
  (time (response (state->client-state (swap! state-atom attack-minion player-id attacker-id defender-id)))))

(defn use-spell!
  [player-id card-id target-id]
  (time (response (state->client-state (swap! state-atom play-spell-card player-id card-id target-id)))))
