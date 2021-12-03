(ns firestone.construct
  (:require [ysera.error :refer [error]]
            [ysera.random :refer [random-nth]]
            [ysera.test :refer [is is-not is= error?]]
            [firestone.definitions :refer [get-definition]]))


(defn create-hero
  "Creates a hero from its definition by the given hero name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-hero "Jaina Proudmoore")
                {:name         "Jaina Proudmoore"
                 :entity-type  :hero
                 :damage-taken 0})
           (is= (create-hero "Jaina Proudmoore" :damage-taken 10)
                {:name         "Jaina Proudmoore"
                 :entity-type  :hero
                 :damage-taken 10}))}
  ; Variadic functions [https://clojure.org/guides/learn/functions#_variadic_functions]
  [name & kvs]
  (let [hero {:name         name
              :entity-type  :hero
              :damage-taken 0}]
    (if (empty? kvs)
      hero
      (apply assoc hero kvs))))


(defn create-card
  "Creates a card from its definition by the given card name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-card "Nightblade" :id "n")
                {:id          "n"
                 :entity-type :card
                 :name        "Nightblade"}))}
  [name & kvs]
  (let [card {:name        name
              :entity-type :card}]
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
                 :divine-shield nil})
           (is= (create-minion "Argent Squire"
                               :id "n"
                               :attacks-performed-this-turn 1)
                {:attacks-performed-this-turn 1
                 :damage-taken                0
                 :entity-type                 :minion
                 :name                        "Argent Squire"
                 :id                          "n"
                 :divine-shield true}))}
  [name & kvs]
  (let [definition (get-definition name)                    ; Will be used later
        minion (assoc {:damage-taken                0
                       :entity-type                 :minion
                       :name                        name
                       :attacks-performed-this-turn 0} :divine-shield (:divine-shield definition))]
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
                 :players                       {"p1" {:id       "p1"
                                                       :mana     10
                                                       :max-mana 10
                                                       :deck     []
                                                       :hand     []
                                                       :minions  []
                                                       :hero     {:name         "Jaina Proudmoore"
                                                                  :id           "r"
                                                                  :damage-taken 0
                                                                  :entity-type  :hero}}
                                                 "p2" {:id       "p2"
                                                       :mana     10
                                                       :max-mana 10
                                                       :deck     []
                                                       :hand     []
                                                       :minions  []
                                                       :hero     {:name         "Thrall"
                                                                  :id           "h2"
                                                                  :damage-taken 0
                                                                  :entity-type  :hero}}}
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
                                                         {:id       (str "p" (inc index))
                                                          :mana     10
                                                          :max-mana 10
                                                          :deck     []
                                                          :hand     []
                                                          :minions  []
                                                          :hero     (if (contains? hero :id)
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
                      (assoc-in $ [:players "p1" :minions] [(create-minion "Nightblade")])
                      (get-minions $ "p1")
                      (map :name $))
                ["Nightblade"]))}
  ([state player-id]
   (:minions (get-player state player-id)))
  ([state]
   (->> (:players state)
        (vals)
        (map :minions)
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
                                   :added-to-board-time-id time-id)]
    (update-in state
               [:players player-id :minions]
               (fn [minions]
                 (conj (->> minions
                            (mapv (fn [m]
                                    (if (< (:position m) position)
                                      m
                                      (update m :position inc)))))
                       ready-minion)))))


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

           (is= (create-game [{:minions [(create-minion "Nightblade")]}])
                (create-game [{:minions ["Nightblade"]}]))

           ; This test is showing the state structure - otherwise avoid large assertions
           (is= (create-game [{:minions ["Nightblade"]
                               :deck    ["Novice Engineer"]
                               :hand    ["Snake"]
                               :mana    6}
                              {:hero "Thrall"}]
                             :player-id-in-turn "p2")
                {:player-id-in-turn             "p2"
                 :players                       {"p1" {:id       "p1"
                                                       :mana     6
                                                       :max-mana 10
                                                       :deck     [{:entity-type :card
                                                                   :id          "c3"
                                                                   :name        "Novice Engineer"
                                                                   :owner-id    "p1"}]
                                                       :hand     [{:entity-type :card
                                                                   :id          "c4"
                                                                   :name        "Snake"
                                                                   :owner-id    "p1"}]
                                                       :minions  [{:divine-shield nil
                                                                   :damage-taken                0
                                                                   :attacks-performed-this-turn 0
                                                                   :added-to-board-time-id      2
                                                                   :entity-type                 :minion
                                                                   :name                        "Nightblade"
                                                                   :id                          "m1"
                                                                   :position                    0
                                                                   :owner-id                    "p1"}]
                                                       :hero     {:name         "Jaina Proudmoore"
                                                                  :id           "h1"
                                                                  :entity-type  :hero
                                                                  :damage-taken 0}}
                                                 "p2" {:id       "p2"
                                                       :mana     10
                                                       :max-mana 10
                                                       :deck     []
                                                       :hand     []
                                                       :minions  []
                                                       :hero     {:name         "Thrall"
                                                                  :id           "h2"
                                                                  :entity-type  :hero
                                                                  :damage-taken 0}}}
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
                                         minions   :minions
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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
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




(defn get-random-character
  "Returns a random character (of the given player or not)."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-random-character "p1")
                    (:name))
                "Jaina Proudmoore")
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")
                                             (create-minion "Nightblade" :id "n2")]}])
                    (get-random-character "p1")
                    (:name))
                "Nightblade")
           (is= (-> (create-game [{:minions [(create-minion "Nightblade")
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
  "Returns the total-health of the character with the given id."
  {:test (fn []
           ; If no particular attack value we look in the definition
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-total-health "n"))
                4)
           ;else we take the one of the particular minion
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :health 45)]}])
                    (get-total-health "n"))
                45)
           )}
  ([character]
   (let [definition (get-definition character)]
     (or (:health character) (:health definition))))

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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :health 45)]}])
                    (get-health "n"))
                45)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-health "n"))
                4)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :health 45 :damage-taken 20)]}])
                    (get-health "n"))
                25)
           )}
  ([state id]
   (get-health (get-character state id)))
  ([character]
   {:pre [(map? character) (contains? character :damage-taken)]}
   (- (get-total-health character) (:damage-taken character))))

(defn get-attack
  "Returns the attack of the character with the given id."
  {:test (fn []
           ; If no particular attack value we look in the definition
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-attack "n"))
                4)
           ;else we take the one of the particular minion
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :attack 45)]}])
                    (get-attack "n"))
                45)
           )}
  ([character]
   (let [definition (get-definition character)]
     (or (:attack character) (:attack definition))))
  ([state id]
   (let [character (get-character state id)]
     (get-attack character))))

(defn replace-minion
  "Replaces a minion with the same id as the given new-minion."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "m")]}])
                    (replace-minion (create-minion "Snake" :id "m"))
                    (get-minion "m")
                    (:name))
                "Snake"))}
  [state new-minion]
  (let [owner-id (or (:owner-id new-minion)
                     (:owner-id (get-minion state (:id new-minion))))]
    (update-in state
               [:players owner-id :minions]
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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (update-minion "n" :damage-taken inc)
                    (get-minion "n")
                    (:damage-taken))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (update-minions ["n"] :damage-taken inc)
                    (get-minion "n")
                    (:damage-taken))
                1)
           (is= (as-> (create-game [{:minions [(create-minion "Nightblade" :id "n")
                                               (create-minion "Nightblade" :id "n2")]}]) $
                      (update-minions $ (map :id (get-minions $ "p1")) :damage-taken 2)
                      (get-minion $ "n")
                      (:damage-taken $))
                2))}
  [state ids key function-or-value]
  (reduce (fn [s id] (update-minion s id key function-or-value)) state ids))


(defn remove-minion
  "Removes a minion with the given id from the state."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (remove-minion "n")
                    (get-minions))
                []))}
  [state id]
  (let [owner-id (:owner-id (get-minion state id))]
    (update-in state
               [:players owner-id :minions]
               (fn [minions]
                 (remove (fn [m] (= (:id m) id)) minions)))))


(defn remove-minions
  "Removes the minions with the given ids from the state."
  {:test (fn []
           (is= (as-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")
                                               (create-minion "Nightblade" :id "n2")]}
                                    {:minions [(create-minion "Nightblade" :id "n3")
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
           (is= (as-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")
                                               (create-minion "Nightblade" :id "n2")]}
                                    {:minions [(create-minion "Nightblade" :id "n3")
                                               (create-minion "Nightblade" :id "n4")]}]) $
                      (remove-all-minions $)
                      (get-minions $)
                      (count $))
                0)
           (is= (as-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")
                                               (create-minion "Nightblade" :id "n2")]}
                                    {:minions [(create-minion "Nightblade" :id "n3")
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

           )}
  [state player-id card]
  (>= (get-mana state player-id) ((get-definition (card :name)) :mana-cost)))

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

(defn draw-card
  {:test (fn []
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n1")]}])
                    (draw-card "p1")
                    (get-card-from-hand "p1" "n1")
                    (:name))
                "Nightblade")
           (is= (-> (create-game)
                    (draw-card "p1")
                    (get-in [:players "p1" :hero :damage-taken]))
                1))}
  [state player-id]
  (if (empty? (get-deck state player-id))
    (update-in state [:players player-id :hero :damage-taken] inc)
    (let [card (nth (get-deck state player-id) 0)]
      (-> state
          (remove-card-from-deck player-id (:id card))
          (add-card-to-hand player-id card)))))

(defn draw-cards
  {:test (fn []
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n1")]}])
                    (draw-cards "p1" 1)
                    (get-card-from-hand "p1" "n1")
                    (:name))
                "Nightblade")
           (is= (-> (create-game)
                    (draw-cards "p1" 5)
                    (get-in [:players "p1" :hero :damage-taken]))
                5))}
  [state player-id number-of-cards]
  (if (<= number-of-cards 0)
    state
    (draw-cards (draw-card state player-id) player-id (- number-of-cards 1))))

(defn set-divine-shield
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n1")]}])
                    (set-divine-shield "n1")
                    (get-minion "n1")
                    (:divine-shield))
                true))}
  [state minion-id]
  (update-minion state minion-id :divine-shield true))

(defn remove-divine-shield
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n1")]}])
                    (set-divine-shield "n1")
                    (remove-divine-shield "n1")
                    (get-minion "n1")
                    (:divine-shield))
                false))}
  [state minion-id]
  (update-minion state minion-id :divine-shield false))

(defn is-divine-shield?
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n1")]}])
                    (set-divine-shield "n1")
                    (is-divine-shield? "n1"))
                true)
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n1")]}])
                    (is-divine-shield? "n1"))
                false)
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n1")]}])
                    (set-divine-shield "n1")
                    (remove-divine-shield "n1")
                    (is-divine-shield? "n1"))
                false))}
  [state minion-id]
  (boolean (:divine-shield (get-minion state minion-id))))


(defn draw-for-each-damaged
  "Make the player draw for each minion damaged he has"
  {:test (fn []
           ;No damaged minion = no draw
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n1")]}])
                    (draw-for-each-damaged "p1")
                    (get-card-from-hand "p1" "n1"))
                nil)
           (is= (-> (create-game [{:deck    [(create-card "Nightblade" :id "n1")]
                                   :minions [(create-minion "Argent Protector")]}])
                    (draw-for-each-damaged "p1")
                    (get-card-from-hand "p1" "n1")
                    (count))
                0)
           ;if one minion is damaged then draw a card.
           (is= (-> (create-game [{:deck    [(create-card "Nightblade" :id "n1")
                                             (create-card "Nightblade" :id "n2")]
                                   :minions [(create-minion "Argent Protector" :damage-taken 1)]}])
                    (draw-for-each-damaged "p1")
                    (get-hand "p1")
                    (count))
                1)
           ;if two minions are damaged then draw two card.
           (is= (-> (create-game [{:deck    [(create-card "Nightblade" :id "n1")
                                             (create-card "Nightblade" :id "n2")]
                                   :minions [(create-minion "Argent Protector" :damage-taken 1 :id "a1")
                                             (create-minion "Argent Protector" :damage-taken 1 :id "a2")]}])
                    (draw-for-each-damaged "p1")
                    (get-hand "p1")
                    (count))
                2))}
  [state player-id]
  (let [minions-player-list (get-minions state player-id)
        function-how-many-damaged (fn [number minion]
                                    (let [damaged? (> (:damage-taken minion) 0)]
                                      (if-not damaged?
                                        number
                                        (inc number))))
        number-damaged (reduce function-how-many-damaged 0 minions-player-list)]
    (draw-cards state player-id number-damaged)))


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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           (is= (-> (create-game [{:hand [(create-card "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           (error? (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
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
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                   (friendly? "n1" "h1")))
           (is (-> (create-game [{:hand [(create-card "Nightblade" :id "n1")]}])
                   (friendly? "n1" "h1")))
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                       (friendly? "n1" "h2")))
           (error? (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                       (friendly? "n1" "bad-id"))))}
  [state id1 id2]
  (let [owner1 (get-owner-id state id1)
        owner2 (get-owner-id state id2)]
    (= owner1 owner2)))