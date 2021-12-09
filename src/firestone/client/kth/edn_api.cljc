(ns firestone.client.kth.edn-api
  (:require [firestone.construct :refer [create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         get-player-id-in-turn]]
            [firestone.client.kth.mapper :refer [state->client-state]]
            [firestone.core-api :refer [end-turn
                                        play-minion-card
                                        attack-minion
                                        play-spell-card
                                        use-hero-power]]
            [firestone.core :refer [update-armor]]))

(defonce state-atom (atom nil))

(defn response
  "This is function aimed to be instrumented to satisfy spec."
  [client-state]
  client-state)

(defn create-game!
  []
  (let [state (reset! state-atom
                      (create-game [{:mana           10
                                     :board-entities [(create-minion "Defender" :id "d1")
                                                      (create-minion "Malorne" :id "d2")
                                                      (create-minion "Maexxna" :id "d3")]
                                     :hand           [(create-card "Deathwing")
                                                      (create-card "Loot Hoarder")
                                                      ]
                                     :deck           [(create-card "Knife Juggler")
                                                      (create-card "Nightblade")]
                                     :hero           (create-hero "Jaina Proudmoore" :armor 10 :health 25)}
                                    {:mana           10
                                     :board-entities [(create-minion "Loot Hoarder" :id "d4" :owner-id "p2")
                                                      (create-minion "Maexxna" :id "d5")
                                                      (create-minion "Defender" :id "d6")]
                                     :hand           [(create-card "Nightblade")
                                                      (create-card "Defender")
                                                      (create-card "Argent Squire")
                                                      (create-card "King Mukla")]
                                     :deck           [(create-card "Knife Juggler")]
                                     :hero           (create-hero "Garrosh Hellscream" :health 25)}]))]
    (time (response (state->client-state state)))))

(defn play-minion-card!
  ([player-id card-id position]
   (time (response (state->client-state (swap! state-atom play-minion-card player-id card-id position)))))
  ([player-id card-id position target-id]
   (time (response (state->client-state (swap! state-atom play-minion-card player-id card-id position target-id))))))

(defn end-turn!
  [player-id]
  (time (response (state->client-state (swap! state-atom end-turn player-id)))))

(defn attack!
  [player-id attacker-id defender-id]
  (time (response (state->client-state (swap! state-atom attack-minion player-id attacker-id defender-id)))))

(defn use-spell!
  ([player-id card-id]
   (time (response (state->client-state (swap! state-atom play-spell-card player-id card-id)))))
  ([player-id card-id target-id]
   (time (response (state->client-state (swap! state-atom play-spell-card player-id card-id target-id))))))

(defn use-power!
  ([player-id]
   (time (response (state->client-state (swap! state-atom use-hero-power player-id)))))
  ([player-id target-id]
   (time (response (state->client-state (swap! state-atom use-hero-power player-id target-id))))))
