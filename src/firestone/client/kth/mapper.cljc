(ns firestone.client.kth.mapper
  (:require [firestone.construct :refer [create-game
                                         enough-mana?
                                         get-attackable-entities-id
                                         get-card-from-hand
                                         get-health
                                         get-minion
                                         get-minions
                                         get-player
                                         get-players
                                         get-total-health
                                         create-minion
                                         create-card]]
            [firestone.core :refer [can-attack?
                                    sleepy?]]
            [firestone.definitions :refer [get-definition]]
            [clojure.spec.alpha :as s]
            [ysera.test :refer [is is-not]]
            [firestone.client.kth.spec]))

(defn card-in-hand->client-card-in-hand
  {:test (fn []
           (let [state (create-game [{:hand [(create-card "Nightblade" :id "n")]
                                      :board-entities [(create-minion "Nightblade" :id "n2")] }])
                 card (get-card-from-hand state "p1" "n")]
           (is (s/valid? :firestone.client.kth.spec/card-in-hand (card-in-hand->client-card-in-hand state card))))) }
[state card]
{:attack             (or (:attack card) 0)
 :description        (or (:description (get-definition (:name card))) " ")
 :entity-type        :card
 :health             (or (:health card) 0)
 :id                 (:id card)
 :name               (:name card)
 :mana-cost          (get-in card [:mana-cost] (:mana-cost (get-definition (:name card))))
 :original-attack    (or (:attack (get-definition (:name card))) 0)
 :original-health    (or (:health (get-definition (:name card))) 0)
 :original-mana-cost (:mana-cost (get-definition (:name card)))
 :owner-id           (:owner-id card)
 :playable           (enough-mana? state (:owner-id card) card)
 :valid-target-ids   (let [function-valid-target (:valid-target (get-definition (:name card)))]
                       (if (some? function-valid-target)
                         (function-valid-target state card)
                         []))
 :type (get-in card [:type])})


(defn minion->client-minion
  {:test (fn []
           (let [state (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                 minion (get-minion state "n")]
             (is (s/valid? :firestone.client.kth.spec/minion (minion->client-minion state minion)))))}
  [state minion]
  {:attack           (get-in minion [:attack] 0)
   :can-attack       (can-attack? state minion)
   :description      (:description (get-definition (:name minion)))
   :entity-type      :minion
   :health           (get-health minion)
   :id               (:id minion)
   :name             (:name minion)
   :mana-cost        (get-in minion [:mana-cost] (:mana-cost (get-definition (:name minion))))
   :max-health       (get-total-health minion)
   :original-attack  (:attack (get-definition (:name minion)))
   :original-health  (:health (get-definition (:name minion)))
   :owner-id         (:owner-id minion)
   :position         (:position minion)
   :set              (:set (get-definition (:name minion)))
   :sleepy           (sleepy? state (:id minion))
   :states           (get-in minion [:states] [])
   :valid-attack-ids (or (get-attackable-entities-id state (:owner-id minion)) [])})

(defn permanent->client-permanent
  [state permanent]
  ({:entity-type :permanent
    :id          (:id permanent)
    :name        (:name permanent)
    :owner-id    (:owner-id permanent)
    :position    (:position permanent)
    :set         (:set permanent)}))


(defn board-entity->client-board-entity
  [state board-entity]
  (if (= (get-in board-entity [:entity-type]) :minion)
    (minion->client-minion state board-entity)
    (permanent->client-permanent state board-entity)))

(defn board-entities->client-board-entities
  {:test (fn []
           (let [state (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                 minions (get-minions state "p1")]
             (is (s/valid? :firestone.client.kth.spec/board-entities (board-entities->client-board-entities state minions)))))}
  [state board-entities]
  (->> board-entities
       (map (fn [be] (board-entity->client-board-entity state be)))))

(defn hero->client-hero
  {:test (fn []
           (let [state (create-game)
                 player (get-player state "p1")
                 ; Change to your abstraction to get the hero
                 hero (:hero player)]
             (is (s/valid? :firestone.client.kth.spec/hero (hero->client-hero state player hero)))))}
  [state player hero]
  {:armor            (or (:armor hero) 0)
   :attack           (or (:attack hero) 0)
   :can-attack       (or (:can-attack hero) false)
   :entity-type      :hero
   :health           (get-health hero)
   :id               (:id hero)
   :mana             (get-in state [:players (:id player) :mana])
   :max-health       (or (get-total-health hero) 30)
   :max-mana         10
   :name             (:name hero)
   :owner-id         (:id player)
   :states           (or (:states hero) [])
   :valid-attack-ids (or (get-attackable-entities-id state (:id player)) [])})

(defn player->client-player
  {:test (fn []
           (let [state (create-game)
                 player (get-player state "p1")]
             (is (s/valid? :firestone.client.kth.spec/player (player->client-player state player)))))}
  [state player]
  {;:board-entities (get-in player [:board-entities] [])
   :board-entities (board-entities->client-board-entities state (get-in player [:board-entities] []))
   :active-secrets (get-in player [:active-secrets] [])
   :deck-size      (count (get-in player [:deck]))
   :hand           (->> (get-in player [:hand] [])
                        (map (fn [cih] (card-in-hand->client-card-in-hand state cih))))
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
  (print state)
  [{:id             "the-game-id"
    :action-index   0
    :player-in-turn (:player-id-in-turn state)
    :players        (->> (get-players state)
                         (map (fn [p] (player->client-player state p))))}])