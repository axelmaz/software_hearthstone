(ns firestone.client.kth.mapper
  (:require [firestone.construct :refer [create-card
                                         create-game
                                         create-minion
                                         enough-mana?
                                         get-attack
                                         get-attackable-entities-id
                                         get-card-from-hand
                                         get-hand
                                         get-health
                                         get-minion
                                         get-minions
                                         get-player
                                         get-players
                                         get-power
                                         get-total-health
                                         is-effect?]]
            [firestone.core :refer [can-attack?
                                    sleepy?
                                    valid-power?]]
            [firestone.definitions :refer [get-definition]]
            [clojure.spec.alpha :as s]
            [ysera.test :refer [is is-not is=]]
            [firestone.client.kth.spec]))


(defn power->client-power
  {:test (fn []
           (let [state (create-game)
                 player (get-player state "p1")
                 power (get-power state "p1")]
             (is (s/valid? :firestone.client.kth.spec/hero-power (power->client-power state player power)))))}
  [state player power]
  {:can-use            (valid-power? state (:id player))
   :owner-id           (:id player)
   :entity-type        :hero-power
   :has-used-your-turn (>= (:used-this-turn power) 1)
   :name               (:name power)
   :mana-cost          (get-in power [:mana-cost] (:mana-cost (get-definition (:name power))))
   :original-mana-cost (:mana-cost (get-definition (:name power)))
   :valid-target-ids   (let [function-valid-target (:valid-target (get-definition (:name power)))]
                         (if (some? function-valid-target)
                           (function-valid-target state (:id player))
                           []))
   :description        (:description (get-definition (:name power)))})

(defn get-valid-targets-from-card
  {:test (fn []
           (let [state (create-game [{:hand           [(create-card "Bananas" :id "b")]
                                      :board-entities [(create-minion "Nightblade" :id "n2")]}])]
             (is= (get-valid-targets-from-card state (get-card-from-hand state "p1" "b")) ["n2"]))
           (let [state (create-game [{:hand [(create-card "Bananas" :id "b")]}])]
             (is= (get-valid-targets-from-card state (get-card-from-hand state "p1" "b")) []))
           (let [state (create-game [{:hand [(create-card "Earthen Ring Farseer" :id "e")]}])]
             (is= (get-valid-targets-from-card state (get-card-from-hand state "p1" "e")) ["h1" "h2"]))
           )}
  [state card]
  (let [function-valid-target (:valid-target (get-definition (:name card)))]
    (if (some? function-valid-target)
      (function-valid-target state card)
      [])))

(defn card-in-hand->client-card-in-hand
  {:test (fn []
           (let [state (create-game [{:hand           [(create-card "Nightblade" :id "n")]
                                      :board-entities [(create-minion "Nightblade" :id "n2")]}])
                 card (get-card-from-hand state "p1" "n")]
             (is (s/valid? :firestone.client.kth.spec/card-in-hand (card-in-hand->client-card-in-hand state card)))))}
  [state card]
  {:attack             (or (:attack card) 0)
   :description        (or (:description (get-definition (:name card))) " ")
   :entity-type        :card
   :health             (or (:health card) 0)
   :id                 (:id card)
   :name               (:name card)
   :mana-cost          (let [amount-of-mana-wraiths-on-board (reduce (fn [counter player]
                                                                       (+ counter (count
                                                                                    (filter (fn [x]
                                                                                              (= (:name x) "Mana Wraith"))
                                                                                            (get-in state [:players player :board-entities])))))
                                                                     0
                                                                     ["p1" "p2"])
                             mana (get-in card [:mana-cost] (:mana-cost (get-definition (:name card))))]
                         (if (= (:name card) "Mountain Giant")
                           (if (< (- (+ mana amount-of-mana-wraiths-on-board) (count (get-hand state (:owner-id card)))) 0)
                             0
                             (- (+ mana amount-of-mana-wraiths-on-board) (count (get-hand state (:owner-id card)))))
                           (+ mana amount-of-mana-wraiths-on-board)))
   :original-attack    (or (:attack (get-definition (:name card))) 0)
   :original-health    (or (:health (get-definition (:name card))) 0)
   :original-mana-cost (:mana-cost (get-definition (:name card)))
   :owner-id           (:owner-id card)
   :playable           (if (= (:type card) :minion)
                         (enough-mana? state (:owner-id card) card)
                         (if (contains? (get-definition (:name card)) :valid-target)
                           (and (enough-mana? state (:owner-id card) card) (not (empty? (get-valid-targets-from-card state card))))
                           (enough-mana? state (:owner-id card) card)))
   :valid-target-ids   (get-valid-targets-from-card state card)
   :type               (get-in card [:type])})


(defn minion->client-minion
  {:test (fn []
           (let [state (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                 minion (get-minion state "n")]
             (is (s/valid? :firestone.client.kth.spec/minion (minion->client-minion state minion)))))}
  [state minion]
  {:attack           (get-attack state minion)
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
   :states           (let [states (get-in minion [:states] [])]
                       (if (is-effect? minion :silenced)
                         [:silenced]
                         states))
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
   :hero-power       (power->client-power state player (get-power state (:id player)))
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
  {:board-entities (board-entities->client-board-entities state (get-in player [:board-entities] []))
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
  [{:id             "the-game-id"
    :action-index   0
    :player-in-turn (:player-id-in-turn state)
    :players        (->> (get-players state)
                         (map (fn [p] (player->client-player state p))))}])