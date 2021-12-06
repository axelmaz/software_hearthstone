(ns firestone.client.kth.mapper
  (:require [firestone.construct :refer [create-game
                                         get-player
                                         get-players
                                         create-minion
                                         create-card]]
            [clojure.spec.alpha :as s]
            [ysera.test :refer [is is-not]]
            [firestone.client.kth.spec]))


(defn hero->client-hero
  {:test (fn []
           (let [state (create-game)
                 player (get-player state "p1")
                 ; Change to your abstraction to get the hero
                 hero (:hero player)]
             (is (s/valid? :firestone.client.kth.spec/hero (hero->client-hero state player hero)))))}
  [state player hero]
  {:armor            10
   :attack           0
   :can-attack       false
   :entity-type      :hero
   :health           14
   :id               (:id hero)
   :mana             (get-in state [:players (:id player) :mana])
   :max-health       30
   :max-mana         10
   :name             (:name hero)
   :owner-id         (:id player)
   :states           []
   :valid-attack-ids []})




(defn player->client-player
  [state player]
  {:board-entities (get-in player [:board-entities] [])
   :active-secrets []
   :deck-size      (count (get-in player [:deck]))
   :hand           (get-in player [:hand] [])
   :id             (:id player)
   :hero           (hero->client-hero state
                                      player
                                      ;; Change using your functions to get the hero
                                      (:hero player))})

(defn state->client-state
  [state]
  [{:id             "the-game-id"
    :action-index   0
    :player-in-turn (:player-id-in-turn state)
    :players        (->> (get-players state)
                         ;; ->> will add the players at the end of map and will be used as arguments for the anon.func
                         (map (fn [p] (player->client-player state p))))}])