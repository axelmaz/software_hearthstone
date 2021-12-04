(ns firestone.client.kth.mapper
  (:require [firestone.construct :refer [create-game
                                         get-player
                                         get-players]]
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
   :mana             8
   :max-health       30
   :max-mana         10
   :name             (:name hero)
   :owner-id         (:id player)
   :states           []
   :valid-attack-ids []})


(defn player->client-player
  {:test (fn []
           (let [state (create-game)
                 player (get-player state "p1")]
             (is (s/valid? :firestone.client.kth.spec/player (player->client-player state player)))))}
  [state player]
  {:board-entities []
   :active-secrets []
   :deck-size      0
   :hand           []
   :id             (:id player)
   :hero           (hero->client-hero state
                                      player
                                      ;; Change using your functions to get the hero
                                      (:hero player))})


(defn state->client-state
  {:test (fn []
           (is (->> (create-game)
                    (state->client-state)
                    (s/valid? :firestone.client.kth.spec/game-states))))}
  [state]
  [{:id             "the-game-id"
    :action-index   0
    :player-in-turn (:player-id-in-turn state)
    :players        (->> (get-players state)
                         (map (fn [p] (player->client-player state p))))}])
