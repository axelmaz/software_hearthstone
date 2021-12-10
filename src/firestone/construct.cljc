(ns firestone.construct
  (:require [ysera.error :refer [error]]
            [ysera.random :refer [random-nth
                                  shuffle-with-seed]]
            [ysera.test :refer [is is-not is= error?]]
            [firestone.definitions :refer [get-definition]]))


(defn create-power
  "Creates a power from its definition by the given power name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-power "Fireblast")
                {:name           "Fireblast"
                 :type           :hero-power
                 :mana-cost      2
                 :used-this-turn 0}))}
  [name & kvs]
  (let [mana-cost (:mana-cost (get-definition name))
        power {:name           name
               :type           :hero-power
               :mana-cost      mana-cost
               :used-this-turn 0}]
    (if (empty? kvs)
      power
      (apply assoc power kvs))))

(defn create-hero
  "Creates a hero from its definition by the given hero name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-hero "Jaina Proudmoore")
                {:name         "Jaina Proudmoore"
                 :entity-type  :hero
                 :damage-taken 0
                 :power        {:name "Fireblast" :type :hero-power :mana-cost 2 :used-this-turn 0}})
           (is= (create-hero "Jaina Proudmoore" :damage-taken 10)
                {:name         "Jaina Proudmoore"
                 :entity-type  :hero
                 :damage-taken 10
                 :power        {:name "Fireblast" :type :hero-power :mana-cost 2 :used-this-turn 0}}))}
  ; Variadic functions [https://clojure.org/guides/learn/functions#_variadic_functions]
  [name & kvs]
  (let [power-name (:hero-power (get-definition name))
        hero {:name         name
              :entity-type  :hero
              :damage-taken 0
              :power        (create-power power-name)}]
    (if (empty? kvs)
      hero
      (apply assoc hero kvs))))


(defn create-card
  "Creates a card from its definition by the given card name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-card "Nightblade" :id "n")
                {:id                 "n"
                 :entity-type        :card
                 :name               "Nightblade"
                 :attack             4
                 :health             4
                 :mana-cost          5
                 :original-mana-cost 5
                 :type               :minion
                 :description        "Battlecry: Deal 3 damage to the enemy hero."
                 :playable           true}))}
  [name & kvs]
  (let [def (get-definition name)
        card {:name               name
              :entity-type        :card
              :type               (get def :type)
              :mana-cost          (get def :mana-cost)
              :original-mana-cost (get def :mana-cost)
              :attack             (get def :attack)
              :health             (get def :health)
              :description        (get def :description)
              :playable           true}]
    (if (empty? kvs)
      card
      (apply assoc card kvs))))



(defn create-minion
  "Creates a minion from its definition by the given minion name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-minion "Nightblade"
                               :id "n"
                               :attacks-performed-this-turn 1)
                {:attacks-performed-this-turn 1
                 :damage-taken                0
                 :entity-type                 :minion
                 :name                        "Nightblade"
                 :id                          "n"
                 :attack                      4
                 :health                      4
                 :states                      []})

           (is= (create-minion "Sunwalker"
                               :id "n"
                               :attacks-performed-this-turn 1)
                {:attacks-performed-this-turn 1
                 :damage-taken                0
                 :entity-type                 :minion
                 :name                        "Sunwalker"
                 :id                          "n"
                 :attack                      4
                 :health                      5
                 :states                      [:taunt :divine-shield]}))}
  [name & kvs]
  (let [definition (get-definition name)                    ; Will be used later
        attack (definition :attack)
        health (definition :health)
        minion (assoc {:damage-taken                0
                       :entity-type                 :minion
                       :name                        name
                       :attacks-performed-this-turn 0
                       :attack                      attack
                       :health                      health}
                 :states
                 (or (:states definition) []))]
    (if (empty? kvs)
      minion
      (apply assoc minion kvs))))


(defn create-empty-state
  "Creates an empty state with the given heroes."
  {:test (fn []
           ; Jaina Proudmoore will be the default hero
           (is= (create-empty-state [(create-hero "Jaina Proudmoore")
                                     (create-hero "Jaina Proudmoore")])
                (create-empty-state))

           (is= (create-empty-state [(create-hero "Jaina Proudmoore" :id "r")
                                     (create-hero "Thrall")])
                {:player-id-in-turn             "p1"
                 :players                       {"p1" {:id             "p1"
                                                       :mana           10
                                                       :max-mana       10
                                                       :deck           []
                                                       :hand           []
                                                       :board-entities []
                                                       :hero           {:name         "Jaina Proudmoore"
                                                                        :id           "r"
                                                                        :damage-taken 0
                                                                        :entity-type  :hero
                                                                        :power        {:name "Fireblast" :type :hero-power :mana-cost 2 :used-this-turn 0}}}
                                                 "p2" {:id             "p2"
                                                       :mana           10
                                                       :max-mana       10
                                                       :deck           []
                                                       :hand           []
                                                       :board-entities []
                                                       :hero           {:name         "Thrall"
                                                                        :id           "h2"
                                                                        :damage-taken 0
                                                                        :entity-type  :hero
                                                                        :power        {:name "Totemic Call" :type :hero-power :mana-cost 2 :used-this-turn 0}}}}
                 :counter                       1
                 :minion-ids-summoned-this-turn []}))}
  ; Multiple arity of a function [https://clojure.org/guides/learn/functions#_multi_arity_functions]
  ([]
   (create-empty-state []))
  ([heroes]
   ; Creates Jaina Proudmoore heroes if heroes are missing.
   (let [heroes (->> (concat heroes [(create-hero "Jaina Proudmoore")
                                     (create-hero "Jaina Proudmoore")])
                     (take 2))]
     {:player-id-in-turn             "p1"
      :players                       (->> heroes
                                          (map-indexed (fn [index hero]
                                                         {:id             (str "p" (inc index))
                                                          :mana           10
                                                          :max-mana       10
                                                          :deck           []
                                                          :hand           []
                                                          :board-entities []
                                                          :hero           (if (contains? hero :id)
                                                                            hero
                                                                            (assoc hero :id (str "h" (inc index))))}))
                                          (reduce (fn [a v]
                                                    (assoc a (:id v) v))
                                                  {}))
      :counter                       1
      :minion-ids-summoned-this-turn []})))


(defn get-player
  "Returns the player with the given id."
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-player "p1")
                    (:id))
                "p1"))}
  [state player-id]
  (get-in state [:players player-id]))


(defn get-player-id-in-turn
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-player-id-in-turn))
                "p1"))}
  [state]
  (:player-id-in-turn state))



(defn get-deck
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-deck "p1"))
                []))}
  [state player-id]
  (get-in state [:players player-id :deck]))


(defn get-hand
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-hand "p1"))
                []))}
  [state player-id]
  (get-in state [:players player-id :hand]))

(defn get-minions
  "Returns the minions on the board for the given player-id or for both players."
  {:test (fn []
           ; Getting minions is also tested in add-minion-to-board.
           (is= (-> (create-empty-state)
                    (get-minions "p1"))
                [])
           (is= (-> (create-empty-state)
                    (get-minions))
                [])
           (is= (as-> (create-empty-state) $
                      (assoc-in $ [:players "p1" :board-entities] [(create-minion "Nightblade")])
                      (get-minions $ "p1")
                      (map :name $))
                ["Nightblade"]))}
  ([state player-id]
   (:board-entities (get-player state player-id)))
  ([state]
   (->> (:players state)
        (vals)
        (map :board-entities)
        (apply concat))))


(defn- generate-id
  "Generates an id and returns a tuple with the new state and the generated id."
  {:test (fn []
           (is= (generate-id {:counter 6})
                [{:counter 7} 6]))}
  [state]
  {:pre [(contains? state :counter)]}
  [(update state :counter inc) (:counter state)])


(defn- generate-time-id
  "Generates a number and returns a tuple with the new state and the generated number."
  {:test (fn []
           (is= (generate-time-id {:counter 6})
                [{:counter 7} 6]))}
  [state]
  {:pre [(contains? state :counter)]}
  [(update state :counter inc) (:counter state)])


(defn add-minion-to-board
  "Adds a minion with a given position to a player's minions and updates the other minions' positions."
  {:test (fn []
           ; Adding a minion to an empty board
           (is= (as-> (create-empty-state) $
                      (add-minion-to-board $ "p1" (create-minion "Nightblade" :id "n") 0)
                      (get-minions $ "p1")
                      (map (fn [m] {:id (:id m) :name (:name m)}) $))
                [{:id "n" :name "Nightblade"}])
           ; Adding a minion and update positions
           (let [minions (-> (create-empty-state)
                             (add-minion-to-board "p1" (create-minion "Nightblade" :id "n1") 0)
                             (add-minion-to-board "p1" (create-minion "Nightblade" :id "n2") 0)
                             (add-minion-to-board "p1" (create-minion "Nightblade" :id "n3") 1)
                             (get-minions "p1"))]
             (is= (map :id minions) ["n1" "n2" "n3"])
             (is= (map :position minions) [2 0 1]))
           ; Generating an id for the new minion
           (let [state (-> (create-empty-state)
                           (add-minion-to-board "p1" (create-minion "Nightblade") 0))]
             (is= (-> (get-minions state "p1")
                      (first)
                      (:name))
                  "Nightblade")
             (is= (:counter state) 3)))}
  [state player-id minion position]
  {:pre [(map? state) (string? player-id) (map? minion) (number? position)]}
  (let [[state id] (if (contains? minion :id)
                     [state (:id minion)]
                     (let [[state value] (generate-id state)]
                       [state (str "m" value)]))
        [state time-id] (generate-time-id state)
        ready-minion (assoc minion :position position
                                   :owner-id player-id
                                   :id id
                                   :added-to-board-time-id time-id
                                   :attack (get minion :attack)
                                   :health (get minion :health)
                                   :entity-type :minion
                                   :attacks-performed-this-turn (get minion :attacks-performed-this-turn))]
    (-> state
        (update-in
          [:players player-id :board-entities]
          (fn [minions]
            (conj (->> minions
                       (mapv (fn [m]
                               (if (< (:position m) position)
                                 m
                                 (update m :position inc)))))
                  ready-minion))))))


(defn add-minions-to-board
  {:test (fn []
           (is= (as-> (create-empty-state) $
                      (add-minions-to-board $ "p1" [(create-minion "Nightblade")
                                                    "Snake"
                                                    (create-minion "Novice Engineer")])
                      (get-minions $ "p1")
                      (map :name $))
                ["Nightblade" "Snake" "Novice Engineer"]))}
  [state player-id minions]
  (->> minions
       (reduce-kv (fn [state index minion]
                    (add-minion-to-board state
                                         player-id
                                         (if (string? minion)
                                           (create-minion minion)
                                           minion)
                                         index))
                  state)))


(defn- add-card-to
  "Adds a card to either the hand or the deck."
  {:test (fn []
           ; Adding cards to deck
           (is= (as-> (create-empty-state) $
                      (add-card-to $ "p1" "Nightblade" :deck)
                      (add-card-to $ "p1" "Novice Engineer" :deck)
                      (get-deck $ "p1")
                      (map :name $))
                ["Nightblade" "Novice Engineer"])
           ; Adding cards to hand
           (is= (as-> (create-empty-state) $
                      (add-card-to $ "p1" "Nightblade" :hand)
                      (add-card-to $ "p1" "Novice Engineer" :hand)
                      (get-hand $ "p1")
                      (map :name $))
                ["Nightblade" "Novice Engineer"]))}
  [state player-id card-or-name place]
  (let [card (if (string? card-or-name)
               (create-card card-or-name)
               card-or-name)
        [state id] (if (contains? card :id)
                     [state (:id card)]
                     (let [[state value] (generate-id state)]
                       [state (str "c" value)]))
        ready-card (assoc card :owner-id player-id
                               :id id)]
    (update-in state [:players player-id place] conj ready-card)))


(defn add-card-to-deck
  {:test (fn []
           (is= (as-> (create-empty-state) $
                      (add-card-to-deck $ "p1" "Nightblade")
                      (get-deck $ "p1")
                      (map :name $))
                ["Nightblade"]))}
  [state player-id card]
  (add-card-to state player-id card :deck))


(defn add-card-to-hand
  {:test (fn []
           (is= (as-> (create-empty-state) $
                      (add-card-to-hand $ "p1" "Nightblade")
                      (get-hand $ "p1")
                      (map :name $))
                ["Nightblade"]))}
  [state player-id card]
  (add-card-to state player-id card :hand))

(defn add-specific-cards-to-hand
  "add one card to the hand of a player a certain number of time"
  {:test (fn []
           (is= (-> (create-empty-state)
                    (add-specific-cards-to-hand "p1" "Nightblade" 5)
                    (get-hand "p1")
                    (count))
                5))}
  [state player-id card number-of-cards]
  (if (<= number-of-cards 0)
    state
    (add-specific-cards-to-hand (add-card-to-hand state player-id card) player-id card (- number-of-cards 1))))


(defn add-cards-to-deck
  {:test (fn []
           (is= (as-> (create-empty-state) $
                      (add-cards-to-deck $ "p1" ["Nightblade" "Novice Engineer"])
                      (get-deck $ "p1")
                      (map :name $))
                ["Nightblade" "Novice Engineer"]))}
  [state player-id cards]
  (reduce (fn [state card]
            (add-card-to-deck state player-id card))
          state
          cards))


(defn add-cards-to-hand
  {:test (fn []
           (is= (as-> (create-empty-state) $
                      (add-cards-to-hand $ "p1" ["Nightblade" "Novice Engineer"])
                      (get-hand $ "p1")
                      (map :name $))
                ["Nightblade" "Novice Engineer"]))}
  [state player-id cards]
  (reduce (fn [state card]
            (add-card-to-hand state player-id card))
          state
          cards))


(defn create-game
  "Creates a game with the given deck, hand, minions (placed on the board), and heroes."
  {:test (fn []
           (is= (create-game) (create-empty-state))

           (is= (create-game [{:hero (create-hero "Thrall")}])
                (create-game [{:hero "Thrall"}]))

           (is= (create-game [{:board-entities [(create-minion "Nightblade")]}])
                (create-game [{:board-entities ["Nightblade"]}]))

           ; This test is showing the state structure - otherwise avoid large assertions
           (is= (create-game [{:board-entities ["Nightblade"]
                               :deck           ["Novice Engineer"]
                               :hand           ["Snake"]
                               :mana           6}
                              {:hero "Thrall"}]
                             :player-id-in-turn "p2")
                {:player-id-in-turn             "p2"
                 :players                       {"p1" {:id             "p1"
                                                       :mana           6
                                                       :max-mana       10
                                                       :deck           [{:description        "Battlecry: Draw a card.",
                                                                         :entity-type        :card,
                                                                         :name               "Novice Engineer",
                                                                         :type               :minion,
                                                                         :mana-cost          2,
                                                                         :original-mana-cost 2,
                                                                         :id                 "c3",
                                                                         :health             1,
                                                                         :owner-id           "p1",
                                                                         :attack             1
                                                                         :playable           true}]
                                                       :hand           [{:description        nil,
                                                                         :entity-type        :card,
                                                                         :name               "Snake",
                                                                         :type               :minion,
                                                                         :mana-cost          1,
                                                                         :original-mana-cost 1,
                                                                         :id                 "c4",
                                                                         :health             1,
                                                                         :owner-id           "p1",
                                                                         :attack             1
                                                                         :playable           true}]
                                                       :board-entities [{:states                      []
                                                                         :damage-taken                0
                                                                         :attacks-performed-this-turn 0
                                                                         :added-to-board-time-id      2
                                                                         :entity-type                 :minion
                                                                         :name                        "Nightblade"
                                                                         :id                          "m1"
                                                                         :position                    0
                                                                         :attack                      4
                                                                         :health                      4
                                                                         :owner-id                    "p1"}]
                                                       :hero           {:name         "Jaina Proudmoore"
                                                                        :id           "h1"
                                                                        :entity-type  :hero
                                                                        :damage-taken 0
                                                                        :power        {:name "Fireblast" :type :hero-power :mana-cost 2 :used-this-turn 0}}}
                                                 "p2" {:id             "p2"
                                                       :mana           10
                                                       :max-mana       10
                                                       :deck           []
                                                       :hand           []
                                                       :board-entities []
                                                       :hero           {:name         "Thrall"
                                                                        :id           "h2"
                                                                        :entity-type  :hero
                                                                        :damage-taken 0
                                                                        :power        {:name "Totemic Call" :type :hero-power :mana-cost 2 :used-this-turn 0}}}}
                 :counter                       5
                 :minion-ids-summoned-this-turn []}))}
  ([data & kvs]
   (let [players-data (map-indexed (fn [index player-data]
                                     (assoc player-data :player-id (str "p" (inc index))))
                                   data)
         state (as-> (create-empty-state (map (fn [player-data]
                                                (cond (nil? (:hero player-data))
                                                      (create-hero "Jaina Proudmoore")

                                                      (string? (:hero player-data))
                                                      (create-hero (:hero player-data))

                                                      :else
                                                      (:hero player-data)))
                                              data)) $
                     (reduce (fn [state {player-id :player-id
                                         minions   :board-entities
                                         deck      :deck
                                         hand      :hand
                                         mana      :mana}]
                               (-> (if mana
                                     (assoc-in state [:players player-id :mana] mana)
                                     state)
                                   (add-minions-to-board player-id minions)
                                   (add-cards-to-deck player-id deck)
                                   (add-cards-to-hand player-id hand)))
                             $
                             players-data))]
     (if (empty? kvs)
       state
       (apply assoc state kvs))))
  ([]
   (create-game [])))


(defn get-minion
  "Returns the minion with the given id."
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (get-minion "n")
                    (:name))
                "Nightblade"))}
  [state id]
  (->> (get-minions state)
       (filter (fn [m] (= (:id m) id)))
       (first)))

(defn get-players
  {:test (fn []
           (is= (->> (create-game)
                     (get-players)
                     (map :id))
                ["p1" "p2"]))}
  [state]
  (->> (:players state)
       (vals)))


(defn get-heroes
  {:test (fn []
           (is= (->> (create-game [{:hero "Thrall"}])
                     (get-heroes)
                     (map :name))
                ["Thrall" "Jaina Proudmoore"]))}
  [state]
  (->> (get-players state)
       (map :hero)))

(defn get-character
  "Returns the character with the given id from the state."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-character "h1")
                    (:name))
                "Jaina Proudmoore")
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (get-character "n")
                    (:name))
                "Nightblade"))}
  [state id]
  (or (some (fn [m] (when (= (:id m) id) m))
            (get-minions state))
      (some (fn [h] (when (= (:id h) id) h))
            (get-heroes state))))

(defn get-hero-id-from-player-id
  "return the id of the heroe of the player with the given id"
  {:test (fn []
           (is= (-> (create-game)
                    (get-hero-id-from-player-id "p1"))
                "h1"))}
  [state player-id]
  (get-in state [:players player-id :hero :id]))




(defn get-opposing-player-id
  "give the id of the opposing player, opposing the given id, or player not in turn if no id given."
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-opposing-player-id))
                "p2")
           (is= (-> (create-empty-state)
                    (get-opposing-player-id "p1"))
                "p2"))}
  ([state]
   (get-opposing-player-id state (get-player-id-in-turn state)))
  ([state id]
   (let [players (map :id (get-players state))
         p1-id (first players)
         p2-id (first (rest players))]
     (if (= p1-id id)
       p2-id
       p1-id))))


(defn get-random-minion
  "Returns a random character (of the given player or not)."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-random-minion "p1"))
                nil)
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")
                                                    (create-minion "Defender" :id "n2")]}])
                    (get-random-minion "p1")
                    (:name))
                "Defender"))}
  ([state player-targeted-id]
   (let [minion-list (get-minions state player-targeted-id)
         random-character ((random-nth 1 minion-list) 1)]
     random-character))
  ([state]
   (let [minion-list (get-minions state)
         random-character ((random-nth 1 minion-list) 1)]
     random-character)))

(defn get-random-character
  "Returns a random character (of the given player or not)."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-random-character "p1")
                    (:name))
                "Jaina Proudmoore")
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")
                                                    (create-minion "Nightblade" :id "n2")]}])
                    (get-random-character "p1")
                    (:name))
                "Nightblade")
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade")
                                                    (create-minion "Nightblade")
                                                    (create-minion "Nightblade")
                                                    (create-minion "Nightblade")
                                                    (create-minion "Nightblade")
                                                    (create-minion "Nightblade")
                                                    (create-minion "Nightblade")
                                                    (create-minion "Nightblade")]}])
                    (get-random-character)
                    (:name)
                    )
                "Nightblade"))}
  ([state player-targeted-id]
   (let [minion-list (get-minions state player-targeted-id)
         minion-and-hero-list (conj minion-list (get-character state (get-hero-id-from-player-id state player-targeted-id)))
         random-character ((random-nth 1 minion-and-hero-list) 1)]
     random-character))
  ([state]
   (let [minion-list (get-minions state)
         function-append (fn [a, v] (conj a v))
         minion-and-hero-list (reduce function-append minion-list (get-heroes state))
         random-character ((random-nth 12 (vec minion-and-hero-list)) 1)]
     random-character
     )))


(defn get-player-id-from-heroe-id
  "return the id of the player corresponding to the heroe with the given id"
  [state heroe-id]
  {:test (fn []
           (is= (-> (create-game)
                    (get-player-id-from-heroe-id "h1"))
                "p1"))}
  (let [which-player? (fn [player-id] (= heroe-id (get-hero-id-from-player-id state player-id)))
        players (get-players state)]
    (first (filter which-player? (map :id players)))))

(defn get-armor
  "Returns the armor of the character."
  {:test (fn []
           ; hero without armor
           (is= (-> (create-hero "Jaina Proudmoore")
                    (get-armor))
                0)
           ; hero with armor
           (is= (-> (create-hero "Jaina Proudmoore" :armor 1)
                    (get-armor))
                1)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-armor "h1"))
                0)
           )}
  ([character]
   (let [armor (:armor character)]
     (if (some? armor)
       armor
       0)))
  ([state id]
   (get-armor (get-character state id))))

(defn get-total-health
  "Returns the total-health of the character."
  {:test (fn []
           ; If no particular attack value we look in the definition
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (get-total-health "n"))
                4)
           ;else we take the one of the particular minion
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n" :health 45)]}])
                    (get-total-health "n"))
                45)
           )}
  ([character]
   (or (:health character) (let [definition (get-definition (:name character))]
                             (:health definition))))

  ([state id]
   (let [character (get-character state id)]
     (get-total-health character))))

(defn get-health
  "Returns the health of the character."
  {:test (fn []
           ; Uninjured minion
           (is= (-> (create-minion "Nightblade")
                    (get-health))
                4)
           ; Injured minion
           (is= (-> (create-minion "Nightblade" :damage-taken 1)
                    (get-health))
                3)
           ; Minion in a state
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (get-health "n"))
                4)
           ; Uninjured hero
           (is= (-> (create-hero "Jaina Proudmoore")
                    (get-health))
                30)
           ; Injured hero
           (is= (-> (create-hero "Jaina Proudmoore" :damage-taken 2)
                    (get-health))
                28)
           ; Hero in a state
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-health "h1"))
                30)
           ;If a minion had a bonus on its health, should be taken in consideration
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n" :health 45)]}])
                    (get-health "n"))
                45)
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (get-health "n"))
                4)
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n" :health 45 :damage-taken 20)]}])
                    (get-health "n"))
                25)
           )}
  ([state id]
   (get-health (get-character state id)))
  ([character]
   {:pre [(map? character)]}
   (- (get-total-health character) (or (:damage-taken character) 0))))

(defn get-attack
  "Returns the attack of the character with the given id."
  {:test (fn []
           ; If no particular attack value we look in the definition
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (get-attack "n"))
                4)
           ;else we take the one of the particular minion
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n" :attack 45)]}])
                    (get-attack "n"))
                45))}
  ([character]
   (let [definition (get-definition (:name character))]
     (or (:attack character) (:attack definition))))
  ([state id]
   (let [character (get-character state id)]
     (get-attack character))))

(defn replace-minion
  "Replaces a minion with the same id as the given new-minion."
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "m")]}])
                    (replace-minion (create-minion "Snake" :id "m"))
                    (get-minion "m")
                    (:name))
                "Snake"))}
  [state new-minion]
  (let [owner-id (or (:owner-id new-minion)
                     (:owner-id (get-minion state (:id new-minion))))]
    (update-in state
               [:players owner-id :board-entities]
               (fn [minions]
                 (map (fn [m]
                        (if (= (:id m) (:id new-minion))
                          new-minion
                          m))
                      minions)))))


(defn update-minion
  "Updates the value of the given key for the minion with the given id. If function-or-value is a value it will be the
   new value, else if it is a function it will be applied on the existing value to produce the new value."
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (update-minion "n" :damage-taken inc)
                    (get-minion "n")
                    (:damage-taken))
                1)
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (update-minion "n" :damage-taken 2)
                    (get-minion "n")
                    (:damage-taken))
                2))}
  [state id key function-or-value]
  (let [minion (get-minion state id)]
    (replace-minion state (if (fn? function-or-value)
                            (update minion key function-or-value)
                            (assoc minion key function-or-value)))))


(defn update-minions
  "Updates the value of the given key for the minions with the given ids. If function-or-value is a value it will be the
   new value, else if it is a function it will be applied on the existing value to produce the new value."
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (update-minions ["n"] :damage-taken inc)
                    (get-minion "n")
                    (:damage-taken))
                1)
           (is= (as-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")
                                                      (create-minion "Nightblade" :id "n2")]}]) $
                      (update-minions $ (map :id (get-minions $ "p1")) :damage-taken 2)
                      (get-minion $ "n")
                      (:damage-taken $))
                2))}
  [state ids key function-or-value]
  (reduce (fn [s id] (update-minion s id key function-or-value)) state ids))




(defn deathrattle
  "Play the deathrattle of a minion that just die, if it has one"
  {:test (fn []
           ; Malorne deathrattle
           (is= (as-> (create-game [{:board-entities [(create-minion "Malorne" :id "n")]
                                     :deck           [(create-card "Nightblade" :id "n1")
                                                      (create-card "Nightblade" :id "n2")
                                                      (create-card "Nightblade" :id "n3")
                                                      (create-card "Nightblade" :id "n4")]}]) $
                      (deathrattle $ (get-minion $ "n"))
                      (get-deck $ "p1")
                      (map :name $))
                ["Nightblade" "Nightblade" "Malorne" "Nightblade" "Nightblade"]))}
  ([state minion]
   (deathrattle state minion {}))
  ([state minion target-id]
   (let [deathrattle-function ((get-definition minion) :deathrattle)]
     (if deathrattle-function
       (deathrattle-function state {:minion-play-effect minion :target-id target-id})
       state))))

(defn remove-minion
  "Removes a minion with the given id from the state."
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (remove-minion "n")
                    (get-minions))
                [])
           ;The deathrattle should be played when a minion is removed
           ; Loot Hoarder
           (is= (-> (create-game [{:board-entities [(create-minion "Loot Hoarder" :id "n")]
                                   :deck           [(create-card "Nightblade")]}])
                    (remove-minion "n")
                    (get-hand "p1")
                    (count))
                1)
           ; Malorne
           (is= (-> (create-game [{:board-entities [(create-minion "Malorne" :id "n")]
                                   :deck           [(create-card "Nightblade")]}])
                    (remove-minion "n")
                    (get-deck "p1")
                    (count))
                2))}
  [state id]
  (let [minion (get-minion state id)
        owner-id (:owner-id minion)
        position (:position minion)]
    (-> state
        (deathrattle (get-minion state id))
        (update-in                                          ;remove the minion
          [:players owner-id :board-entities]
          (fn [minions]
            (remove (fn [m] (= (:id m) id)) minions)))
        (update-in                                          ;replace the others to the good position
          [:players owner-id :board-entities]
          (fn [minions]
            (->> minions
                 (mapv (fn [m]
                         (if (< (:position m) position)
                           m
                           (update m :position dec)))))
            )))))


(defn remove-minions
  "Removes the minions with the given ids from the state."
  {:test (fn []
           (is= (as-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")
                                                      (create-minion "Nightblade" :id "n2")]}
                                    {:board-entities [(create-minion "Nightblade" :id "n3")
                                                      (create-minion "Nightblade" :id "n4")]}]) $
                      (remove-minions $ "n1" "n4")
                      (get-minions $)
                      (map :id $))
                ["n2" "n3"]))}
  [state & ids]
  (reduce remove-minion state ids)
  )

(defn remove-all-minions
  "Removes the all the minions of the board.
  If a player id is given, remove only the minions of this player"
  {:test (fn []
           (is= (as-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")
                                                      (create-minion "Nightblade" :id "n2")]}
                                    {:board-entities [(create-minion "Nightblade" :id "n3")
                                                      (create-minion "Nightblade" :id "n4")]}]) $
                      (remove-all-minions $)
                      (get-minions $)
                      (count $))
                0)
           (is= (as-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")
                                                      (create-minion "Nightblade" :id "n2")]}
                                    {:board-entities [(create-minion "Nightblade" :id "n3")
                                                      (create-minion "Nightblade" :id "n4")]}]) $
                      (remove-all-minions $ "p1")
                      (get-minions $)
                      (count $))
                2))}
  ([state]
   (reduce remove-minion state (map :id (get-minions state))))
  ([state player-id]
   (reduce remove-minion state (map :id (get-minions state player-id)))))


(defn remove-card-from-hand
  {:test (fn []
           (is= (as-> (create-game [{:hand [(create-card "Nightblade" :id "n1")
                                            (create-card "Defender" :id "d")
                                            (create-card "Nightblade" :id "n2")]}]) $
                      (remove-card-from-hand $ "p1" "d")
                      (get-hand $ "p1")
                      (map :name $))
                ["Nightblade" "Nightblade"]))}
  [state player-id card-id]
  (update-in state [:players player-id :hand]
             (fn [hand]
               (->> hand
                    (remove (fn [{id :id}] (= id card-id)))))))


(defn remove-all-cards-from-hand
  "Removes all the card from the hand of the given player"
  {:test (fn []
           (is= (as-> (create-game [{:hand [(create-card "Nightblade" :id "n1")
                                            (create-card "Nightblade" :id "n2")]}
                                    {:hand [(create-card "Nightblade" :id "n3")
                                            (create-card "Nightblade" :id "n4")]}]) $
                      (remove-all-cards-from-hand $ "p1")
                      (get-hand $ "p1")
                      (count $))
                0))}
  [state player-id]
  (reduce (fn [s card-id] (remove-card-from-hand s player-id card-id)) state (map :id (get-hand state player-id))))

(defn remove-card-from-deck
  {:test (fn []
           (is= (as-> (create-game [{:deck [(create-card "Nightblade" :id "n1")
                                            (create-card "Defender" :id "d")
                                            (create-card "Nightblade" :id "n2")]}]) $
                      (remove-card-from-deck $ "p1" "d")
                      (get-deck $ "p1")
                      (map :name $))
                ["Nightblade" "Nightblade"]))}
  [state player-id card-id]
  (update-in state [:players player-id :deck]
             (fn [deck]
               (->> deck
                    (into [] (remove (fn [{id :id}] (= id card-id))))))))

(defn get-card-from-hand
  {:test (fn []
           (is= (-> (create-game [{:hand [(create-card "Defender" :id "d")]}])
                    (get-card-from-hand "p1" "d")
                    (:name))
                "Defender"))}
  [state player-id card-id]
  (->> (get-hand state player-id)
       (some (fn [{id :id :as card}] (when (= id card-id) card)))))

(defn get-mana
  [state player-id]
  (get-in state [:players player-id :mana]))

(defn enough-mana?
  {:test (fn []
           (is (-> (create-game [{:hand [(create-card "Nightblade" :id "d")]
                                  :mana 9}])
                   (enough-mana? "p1" (create-card "Nightblade" :id "d"))))
           (is-not (-> (create-game [{:hand [(create-card "Nightblade" :id "d")]
                                      :mana 2}])
                       (enough-mana? "p1" (create-card "Nightblade" :id "d"))))
           (is-not (-> (create-game [{:mana 1}])
                       (enough-mana? "p1" (create-power "Fireblast" :id "f"))))
           (is (-> (create-game [{:mana 2}])
                   (enough-mana? "p1" (create-power "Fireblast"))))

           )}
  [state player-id entity]
  (>= (get-mana state player-id) (or (:mana-cost entity) ((get-definition (:name entity)) :mana-cost))))

(defn decrease-mana
  {:test (fn []
           (is= (-> (create-game [{:mana 9}])
                    (decrease-mana "p1" 2)
                    (get-mana "p1")
                    )
                7)
           )
   }
  [state player-id decrease-number]
  (update-in state [:players player-id :mana]
             (fn [mana] (- mana decrease-number))))

(defn decrease-mana-with-card
  {:test (fn []
           (is= (-> (create-game [{:mana 9}])
                    (decrease-mana-with-card "p1" (create-card "Nightblade" :id "d"))
                    (get-mana "p1")
                    )
                4)
           )
   }
  [state player-id card]
  (-> state (decrease-mana player-id ((get-definition (card :name)) :mana-cost))))

(defn set-effect
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
                    (set-effect "n1" :divine-shield)
                    (get-minion "n1")
                    (:states)
                    (first))
                :divine-shield))}
  [state minion-id effect]
  (update-minion state minion-id :states (fn [eff] (conj eff effect))))

(defn remove-effect
  {:test (fn []
           (empty? (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
                       (set-effect "n1" :divine-shield)
                       (remove-effect "n1" :divine-shield)
                       (get-minion "n1")
                       (:states))))}
  [state minion-id effect]
  (update-minion state minion-id :states (fn [list_eff] (remove (fn [eff] (= eff effect)) list_eff))))

(defn is-effect?
  "True if the minion has the corresponding effect"
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
                    (set-effect "n1" :divine-shield)
                    (is-effect? "n1" :divine-shield))
                true)
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
                    (is-effect? "n1" :divine-shield))
                false)
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
                    (set-effect "n1" :divine-shield)
                    (remove-effect "n1" :divine-shield)
                    (is-effect? "n1" :divine-shield))
                false))}
  ([state minion-id effect]
   (boolean (some #{effect} (:states (get-minion state minion-id)))))
  ([minion effect]
   (boolean (some #{effect} (:states minion)))))

(defn get-owner-id
  "give the id of the owner of the character"
  {:test (fn []
           ; If the id is a player, should give this player
           (is= (-> (create-game)
                    (get-owner-id "p1"))
                "p1")
           (is= (-> (create-game)
                    (get-owner-id "h1"))
                "p1")
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           (is= (-> (create-game [{:hand [(create-card "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           (error? (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                       (get-owner-id "bad-id"))))}

  [state id]
  (let [players-id (map :id (get-players state))
        p1 (first players-id)
        p2 (first (rest players-id))
        heroes-id (map :id (get-heroes state))
        minion (get-minion state id)
        hand-p1-id (map :id (get-hand state p1))
        hand-p2-id (map :id (get-hand state p2))
        deck-p1-id (map :id (get-deck state p1))
        deck-p2-id (map :id (get-deck state p2))]
    (if (some #{id} players-id)
      id
      (if (some #{id} heroes-id)
        (get-player-id-from-heroe-id state id)
        (if (some? minion)
          (:owner-id minion)
          (if (or (some #{id} hand-p1-id) (some #{id} deck-p1-id))
            p1
            (if (or (some #{id} hand-p2-id) (some #{id} deck-p2-id))
              p2
              (error "id : " id " not found")
              )))))))

(defn friendly?
  "True if the two characters are friendly "
  {:test (fn []
           (is (-> (create-game)
                   (friendly? "p1" "h1")))
           (is (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                   (friendly? "n1" "h1")))
           (is (-> (create-game [{:hand [(create-card "Nightblade" :id "n1")]}])
                   (friendly? "n1" "h1")))
           (is-not (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                       (friendly? "n1" "h2")))
           (error? (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                       (friendly? "n1" "bad-id"))))}
  [state id1 id2]
  (let [owner1 (get-owner-id state id1)
        owner2 (get-owner-id state id2)]
    (= owner1 owner2)))


(defn get-taunt-minions-id
  "Return a sequence of id corresponding to the minions of the player that have taunt. "
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-minion "Sunwalker" :id "n1")
                                                    (create-minion "Sunwalker" :id "n2")]}])
                    (get-taunt-minions-id "p1"))
                ["n1" "n2"])
           (is= (-> (create-game [{:board-entities [(create-minion "Sunwalker" :id "n1")
                                                    (create-minion "Sunwalker" :id "n2")]}])
                    (get-taunt-minions-id "p1"))
                ["n1" "n2"]))}
  [state player-id]
  (filter (fn [minion-id] (is-effect? state minion-id :taunt)) (map :id (get-minions state player-id))))

(defn set-deck
  "Return a state with the given deck for the given player"
  {:test (fn []
           (is= (-> (create-game)
                    (set-deck "p1" [(create-card "Sunwalker" :id "n1")
                                    (create-card "Sunwalker" :id "n2")
                                    (create-card "Sunwalker" :id "n3")
                                    (create-card "Sunwalker" :id "n4")])
                    (get-deck "p1")
                    (count))
                4))}
  [state player-id cards]
  (assoc-in state [:players player-id :deck] cards))


(defn shuffle-deck
  "Return a state with a shuffle deck for the given player"
  {:test (fn []
           (is= (as-> (create-game [{:deck [(create-card "Sunwalker" :id "n1")
                                            (create-card "Sunwalker" :id "n2")
                                            (create-card "Sunwalker" :id "n3")
                                            (create-card "Sunwalker" :id "n4")
                                            (create-card "Sunwalker" :id "n5")
                                            (create-card "Sunwalker" :id "n6")]}]) $
                      (shuffle-deck $ "p1")
                      (get-deck $ "p1")
                      (map :id $))
                ["n1" "n5" "n4" "n6" "n3" "n2"])
           )}
  [state player-id]
  (let [old-deck (get-deck state player-id)
        shuffled-deck ((shuffle-with-seed 12 old-deck) 1)]
    (set-deck state player-id shuffled-deck)))

(defn get-attackable-entities-id
  "Return a sequence of id corresponding to the minions and hero that the player could attack. "
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-minion "Sunwalker" :id "n1")
                                                    (create-minion "Sunwalker" :id "n2")
                                                    (create-minion "Nightblade" :id "n")]}])
                    (get-attackable-entities-id "p2"))
                ["n1" "n2"])
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (get-attackable-entities-id "p2"))
                ["h1" "n"]))}
  [state player-id]
  (let [other-player-id (get-opposing-player-id state player-id)
        taunt-minions-id (get-taunt-minions-id state other-player-id)]
    (if-not (empty? taunt-minions-id)
      taunt-minions-id
      (vec (conj (map :id (get-minions state other-player-id)) (get-hero-id-from-player-id state other-player-id))))))

(defn card-to-minion
  "Return the minion corresponding to the card."
  [card]
  (let [name (:name card)
        mana-cost (:mana-cost card)
        health (:health card)
        attack (:attack card)
        id (:id card)]
    (create-minion name :mana-cost mana-cost :health health :attack attack :id id)))

(defn get-power
  "Return the power of the player of the given id"
  {:test (fn []
           (is= (-> (create-game)
                    (get-power "p2")
                    (:name))
                "Fireblast"))}
  [state player-id]
  (get-in state [:players player-id :hero :power]))

(defn swap-minion-of-player
  "Swap the given minion from one player to another"
  {:test (fn []
           (is (empty? (-> (create-game [{:board-entities [(create-minion "Defender" :id "d")]}])
                           (swap-minion-of-player "d")
                           (get-minions "p1")
                           )))
           (is= (-> (create-game [{:board-entities [(create-minion "Defender" :id "d")]}])
                    (swap-minion-of-player "d")
                    (get-minions "p2")
                    (count))
                1))}
  [state minion-id]
  (let [old-minion (get-minion state minion-id)
        old-owner-id (:owner-id old-minion)
        new-owner-id (get-opposing-player-id state old-owner-id)
        new-minion (assoc old-minion :owner-id new-owner-id)]
    (-> state
        (update-in [:players old-owner-id :board-entities] (fn [board-entitities]
                                                             (remove (fn [entity]
                                                                       (= (:id entity) minion-id)) board-entitities)))
        (update-in [:players new-owner-id :board-entities] (fn [board-entitities]
                                                             (conj board-entitities new-minion))))))

(defn listener-effect
  "Apply the effect of the listener which correspond to the event of all the minions on the board which have one"
  {:test (fn []
           ; The end-turn effect of Ragnaros the Firelord is to deal 8 damages to a random enemy.
           (is= (-> (create-game [{:board-entities [(create-card "Ragnaros the Firelord")]}])
                    (listener-effect :states-end-turn)
                    (get-health "h2"))
                22)
           (is= (-> (create-game [{:board-entities [(create-card "Ragnaros the Firelord")]}
                                  {:board-entities [(create-card "Nightblade" :id "n1")
                                                    (create-card "Nightblade" :id "n2" :health 12)]}])
                    (listener-effect :states-end-turn)
                    (get-health "n2"))
                4)
           ; The damaged minion effect of Armorsmith is to give 1 armor to the hero every-time a friendly-minion take damage
           (is= (-> (create-game [{:board-entities [(create-card "Armorsmith" :id "a")
                                                    (create-card "Nightblade" :id "n")]}])
                    (listener-effect :states-minion-takes-damage {:minion-takes-damage (create-minion "Nightblade" :id "n" :owner-id "p1")})
                    (get-armor "h1"))
                1)
           (is= (-> (create-game [{:board-entities [(create-card "Armorsmith" :id "a")
                                                    (create-card "Nightblade" :id "n")
                                                    (create-card "Armorsmith" :id "a")]}])
                    (listener-effect :states-minion-takes-damage {:minion-takes-damage (create-minion "Nightblade" :id "n" :owner-id "p1")})
                    (get-armor "h1"))
                2)
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "a")
                                                    (create-card "Nightblade" :id "n")
                                                    (create-card "Nightblade" :id "m")]}])
                    (listener-effect :states-minion-takes-damage {:minion-takes-damage (create-minion "Nightblade" :id "n" :owner-id "p1")})
                    (get-armor "h1"))
                0)
           ; :states-cast-spell effect test
           (is= (-> (create-game [{:board-entities [(create-card "Lorewalker Cho" :id "a")]}])
                    (listener-effect :states-cast-spell {:card-spell-casted (create-card "Battle Rage" :id "b" :owner-id "p1")})
                    (get-hand "p2")
                    (first)
                    (:name))
                "Battle Rage")
           ; test Doomsayer : should remove all minions if it is its turn
           (is= (-> (create-game [{:board-entities [(create-minion "Defender")
                                                    (create-minion "Doomsayer")]}
                                  {:board-entities [(create-minion "Defender")]}])
                    (listener-effect :states-start-turn)
                    (get-minions)
                    (count))
                0)
           ; test Doomsayer : should not remove all minions if it is not its turn
           (is= (-> (create-game [{:board-entities [(create-minion "Defender")]}
                                  {:board-entities [(create-minion "Defender")
                                                    (create-minion "Doomsayer")]}])
                    (listener-effect :states-start-turn)
                    (get-minions)
                    (count))
                3))}
  ([state event other-args]
   (let [minions (get-minions state)
         function-of-the-effect (fn [a minion]
                                  (let [function-result (event (get-definition (:name minion)))]
                                    (if (some? function-result)
                                      (function-result a (assoc other-args :minion-play-effect minion))
                                      a)))]
     (reduce function-of-the-effect state minions)))
  ([state event]
   (listener-effect state event {})))

(defn listener-effect-in-hand
  "Apply the effect of the listener which correspond to the event of all the card in hand which have one"
  {:test (fn []
           ; test Blubber Baron : should get +1/+1 for each battlecry summoned
           (is= (-> (create-game [{:hand [(create-card "Blubber Baron" :id "b1")]}])
                    (listener-effect-in-hand :states-summon-minion-in-hand {:card-minion-summoned (create-card "Nightblade" :owner-id "p1")})
                    (get-card-from-hand "p1" "b1")
                    (get-attack))
                2)
           (is= (-> (create-game [{:hand [(create-card "Blubber Baron" :id "b1")]}])
                    (listener-effect-in-hand :states-summon-minion-in-hand {:card-minion-summoned (create-card "Nightblade" :owner-id "p1")})
                    (get-card-from-hand "p1" "b1")
                    (get-health))
                2))}
  ([state event other-args]
   (let [cards (concat (get-hand state (get-player-id-in-turn state)) (get-hand state (get-opposing-player-id state)))
         function-of-the-effect (fn [a card]
                                  (let [function-result (event (get-definition (:name card)))]
                                    (if (some? function-result)
                                      (function-result a (assoc other-args :card-play-effect card))
                                      a)))]
     (reduce function-of-the-effect state cards)))
  ([state event]
   (listener-effect-in-hand state event {})))

(defn replace-card-in-hand
  "Replaces a card with the same id as the given new-card."
  {:test (fn []
           (is= (-> (create-game [{:hand [(create-card "Nightblade" :id "m")]}])
                    (replace-card-in-hand "p1" (create-card "Snake" :id "m"))
                    (get-card-from-hand "p1" "m")
                    (:name))
                "Snake"))}
  [state owner-id new-card]
  (update-in state
             [:players owner-id :hand]
             (fn [cards]
               (map (fn [m]
                      (if (= (:id m) (:id new-card))
                        new-card
                        m))
                    cards))))

(defn update-card
  "Updates the value of the given key for the minion with the given id. If function-or-value is a value it will be the
   new value, else if it is a function it will be applied on the existing value to produce the new value."
  {:test (fn []
           (is= (-> (create-game [{:hand [(create-card "Nightblade" :id "n")]}])
                    (update-card "p1" "n" :attack inc)
                    (get-card-from-hand "p1" "n")
                    (:attack))
                5))}
  [state owner-id id key function-or-value]
  (let [card (get-card-from-hand state owner-id id)]
    (replace-card-in-hand state owner-id (if (fn? function-or-value)
                                           (update card key function-or-value)
                                           (assoc card key function-or-value)))))