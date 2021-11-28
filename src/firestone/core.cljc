(ns firestone.core
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [ysera.collections :refer [seq-contains?]]
            [clojure.string :refer [includes?]]
            [firestone.definitions :refer [get-definition]]
            [firestone.construct :refer [add-minion-to-board
                                         create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         draw-card
                                         get-hand
                                         get-heroes
                                         get-mana
                                         get-minion
                                         get-minions
                                         get-opposing-player-id
                                         get-player-id-in-turn
                                         get-players
                                         is-divine-shield?
                                         remove-divine-shield
                                         set-divine-shield
                                         update-minion]]))


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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :health 45 :damage-taken 20)]}])
                    (get-health "n"))
                25)
           )}
  ([character]
   {:pre [(map? character) (contains? character :damage-taken)]}
   (let [definition (get-definition character)]
     (- (get-total-health character) (:damage-taken character))))
  ([state id]
   (get-health (get-character state id))))

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


(defn sleepy?
  "Checks if the minion with given id is sleepy."
  {:test (fn []
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}]
                                :minion-ids-summoned-this-turn ["n"])
                   (sleepy? "n")))
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                       (sleepy? "n"))))}
  [state id]
  (seq-contains? (:minion-ids-summoned-this-turn state) id))


(defn valid-attack?
  "Checks if the attack is valid"
  {:test (fn []
           ; Should be able to attack an enemy minion
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                 {:minions [(create-minion "Defender" :id "d")]}])
                   (valid-attack? "p1" "n" "d")))
           ; Should be able to attack an enemy hero
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                   (valid-attack? "p1" "n" "h2")))
           ; Should not be able to attack your own minions
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")
                                                (create-minion "Defender" :id "d")]}])
                       (valid-attack? "p1" "n" "d")))
           ; Should not be able to attack if it is not your turn
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                     {:minions [(create-minion "Defender" :id "d")]}]
                                    :player-id-in-turn "p2")
                       (valid-attack? "p1" "n" "d")))
           ; Should not be able to attack if you are sleepy
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                     {:minions [(create-minion "Defender" :id "d")]}]
                                    :minion-ids-summoned-this-turn ["n"])
                       (valid-attack? "p1" "n" "d")))
           ; Should not be able to attack if you already attacked this turn
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :attacks-performed-this-turn 1)]}
                                     {:minions [(create-minion "Defender" :id "d")]}])
                       (valid-attack? "p1" "n" "d")))
           ; Ragnaros the Firelord shouldn't be able to attack
           (is-not (-> (create-game [{:minions [(create-minion "Ragnaros the Firelord" :id "n")]}
                                 {:minions [(create-minion "Defender" :id "d")]}])
                   (valid-attack? "p1" "n" "d"))))}
  [state player-id attacker-id target-id]
  (let [attacker (get-minion state attacker-id)
        target (get-character state target-id)]
    (and attacker
         target
         (= (:player-id-in-turn state) player-id)
         (< (:attacks-performed-this-turn attacker) 1)
         (not (sleepy? state attacker-id))
         (not= (:owner-id attacker) (:owner-id target))
         (not (:effect-cant-attack (get-definition attacker))))))



(defn get-hero-id-from-player-id
  "return the id of the heroe of the player with the given id"
  {:test (fn []
           (is= (-> (create-game)
                    (get-hero-id-from-player-id "p1"))
                "h1"))}
  [state player-id]
  (get-in state [:players player-id :hero :id]))

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

(defn effect-minion-damaged
  "the minion minion-id took damages, so we apply the effects of the other minion of the field"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-card "Armorsmith" :id "a")
                                             (create-card "Nightblade" :id "n")]}])
                    (effect-minion-damaged "a")
                    (get-armor "h1"))
                1)
           (is= (-> (create-game [{:minions [(create-card "Armorsmith" :id "a")
                                             (create-card "Nightblade" :id "n")
                                             (create-card "Armorsmith" :id "a")]}])
                    (effect-minion-damaged "a")
                    (get-armor "h1"))
                2)
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "a")
                                             (create-card "Nightblade" :id "n")
                                             (create-card "Nightblade" :id "a")]}])
                    (effect-minion-damaged "a")
                    (get-armor "h1"))
                0))}
  [state minion-id]
  (let [owner-id (:owner-id (get-minion state minion-id))
        minions-of-the-player (get-minions state owner-id)
        function-damaged-minion (fn [a minion]
                                  (let [function-result (:effect-when-friendly-minion-takes-damage (get-definition (:name minion)))]
                                    (if (some? function-result)
                                      (function-result a minion)
                                      a)))]
    (reduce function-damaged-minion state minions-of-the-player)))

(defn summon-minions-on-board-spell
  "Deals 1 damage to a random enemy whenever a friendly card is summoned"
  {:test (fn []
           (is= (as-> (create-game [{:minions [(create-card "Knife Juggler" :id "k")]
                                   :hand [(create-card "Defender" :id "d")]}]) $
                    (get-in (summon-minions-on-board-spell $) [:players "p2" :hero :damage-taken]))
                1))}
  [state]
  (let [minions (get-minions state (get-player-id-in-turn state))
        function-summoned-minion-spell (fn [a minion]
                                  (let [function-result (:summon-friendly-minion-do-attack-spell (get-definition (:name minion)))]
                                    (if (some? function-result)
                                      (function-result a)
                                      a)))]
    (reduce function-summoned-minion-spell state minions)))

(defn update-armor
  "Update (increase or decrease) the armor of the hero of the given player-id."
  {:test (fn []
           ; hero without armor
           (is= (-> (create-game)
                    (update-armor "p1" 1)
                    (get-armor "h1"))
                1)
           ; hero with armor
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 2 :id "h1")}])
                    (update-armor "p1" 1)
                    (get-armor "h1"))
                3)
           ; loosing armor
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 2 :id "h1")}])
                    (update-armor "p1" -1)
                    (get-armor "h1"))
                1))}
  [state player-id value]
  (if (= 0 (get-armor state (get-hero-id-from-player-id state player-id)))
    (update-in state [:players player-id :hero] assoc :armor value)
    (update-in state [:players player-id :hero :armor] + value)))

(defn update-attack
  "Update (increase or decrease) the attack of the minion of the given minion-id."
  {:test (fn []
           ; If the attack is not yet defined in the minion
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 1)]}])
                    (update-attack "n" +2)
                    (get-attack "n"))
                6)
           ;If it is already defined
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 1 :attack 7)]}])
                    (update-attack "n" +2)
                    (get-attack "n"))
                9)
           ;Decrease
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :attack 7)]}])
                    (update-attack "n" -2)
                    (get-attack "n"))
                5))}
  [state minion-id value]
  (let [old-attack (get-attack state minion-id)
        update-function (fn [v] (+ v value))
        new-attack (update-function old-attack)]
    (-> state
        (update-minion minion-id :attack new-attack))))

(defn update-total-health
  "Update (increase or decrease) the total-health of the minion of the given minion-id."
  {:test (fn []
           ; If the total-health is not yet defined in the minion
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 1)]}])
                    (update-total-health "n" +2)
                    (get-total-health "n"))
                6)
           ;If it is already defined
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 1 :health 7)]}])
                    (update-total-health "n" +2)
                    (get-total-health "n"))
                9)
           ;Decrease
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 1 :health 7)]}])
                    (update-total-health "n" -2)
                    (get-total-health "n"))
                5)
           )}
  [state minion-id value]
  (let [old-health (get-total-health state minion-id)
        update-function (fn [v] (+ v value))
        new-health (update-function old-health)]
    (-> state
        (update-minion minion-id :health new-health))))

(defn deal-damages-to-minion
  "Deal the value of damage to the corresponding minion"
  {:test (fn []
           ;A minion with the given id should take damage so its life decreases
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages-to-minion "n1" 1)
                    (get-health "n1"))
                3)
           ;If the minion has a divine-shield it does not loose any life point
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (set-divine-shield "n1")
                    (deal-damages-to-minion "n1" 1)
                    (get-health "n1"))
                4)
           ;If the minion has a divine-shield it should loose it
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (set-divine-shield "n1")
                    (deal-damages-to-minion "n1" 1)
                    (is-divine-shield? "n1"))
                false)
           ;If the id doesn't correspond we return nil (uselful for deal-damages function
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages-to-minion "doesn't exist" 1))
                nil)
           ;Is the effect of Armorsmith working ?
           (is= (-> (create-game [{:minions [(create-card "Armorsmith" :id "a")
                                             (create-card "Nightblade" :id "n")]}])
                    (deal-damages-to-minion "n" 1)
                    (get-armor "h1"))
                1)
           )}
  [state minion-id value-damages]
  (let [is-minion-id? (reduce (fn [a v]
                                (if (= v minion-id)
                                  true
                                  a))
                              false
                              (map :id (get-minions state)))]
    (if-not is-minion-id?                                   ; test if minion-id is the id of a minion
      nil
      (if (is-divine-shield? state minion-id)
        (remove-divine-shield state minion-id)
        (-> state
            (effect-minion-damaged minion-id)
            (update-minion minion-id :damage-taken value-damages))))))

(defn deal-damages-to-heroe-by-player-id
  "Deal the value of damage to the corresponding heroe given thanks to the player id"
  {:test (fn []
           ; Without armor
           (is= (-> (create-game)
                    (deal-damages-to-heroe-by-player-id "p1" 10)
                    (get-health "h1"))
                20)
           ; With armor but damages are less important than armor
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 10 :id "h1")}])
                    (deal-damages-to-heroe-by-player-id "p1" 2)
                    (get-armor "h1"))
                8)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 10 :id "h1")}])
                    (deal-damages-to-heroe-by-player-id "p1" 2)
                    (get-health "h1"))
                30)
           ; With armor and damages are more important than armor
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 10 :id "h1")}])
                    (deal-damages-to-heroe-by-player-id "p1" 18)
                    (get-armor "h1"))
                0)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 10 :id "h1")}])
                    (deal-damages-to-heroe-by-player-id "p1" 18)
                    (get-health "h1"))
                22)
           ; Error = return nil
           (is= (-> (create-game)
                    (deal-damages-to-heroe-by-player-id "doesn't exist" 10))
                nil)
           )}
  [state player-id value-damages]
  (let [is-player-id? (reduce (fn [a v]
                                (if (= v player-id)
                                  true
                                  a))
                              false
                              (map :id (get-players state)))]
    (if-not is-player-id?                                   ; test if player-id is the id of a player
      nil
      (let [armor (get-armor state (get-hero-id-from-player-id state player-id))
            armor-damages (- armor value-damages)]
        (if (>= armor-damages 0)
          (update-armor state player-id (- value-damages))
          (-> state
              (update-armor player-id (- armor))
              (update-in [:players player-id :hero :damage-taken] - armor-damages)))))))

(defn deal-damages-to-heroe-by-heroe-id
  "Deal the value of damage to the corresponding heroe given thanks to the heroe id"
  {:test (fn []
           ; Without armor
           (is= (-> (create-game)
                    (deal-damages-to-heroe-by-heroe-id "h1" 10)
                    (get-health "h1"))
                20)
           ; With armor but damages are less important than armor
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 10 :id "h1")}])
                    (deal-damages-to-heroe-by-heroe-id "h1" 2)
                    (get-armor "h1"))
                8)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 10 :id "h1")}])
                    (deal-damages-to-heroe-by-heroe-id "h1" 2)
                    (get-health "h1"))
                30)
           ; With armor and damages are more important than armor
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 10 :id "h1")}])
                    (deal-damages-to-heroe-by-heroe-id "h1" 18)
                    (get-armor "h1"))
                0)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 10 :id "h1")}])
                    (deal-damages-to-heroe-by-heroe-id "h1" 18)
                    (get-health "h1"))
                22)
           )}
  [state heroe-id value-damages]
  (deal-damages-to-heroe-by-player-id state (get-player-id-from-heroe-id state heroe-id) value-damages))

(defn deal-damages
  "Deal the value of damage to the corresponding character"
  {:test (fn []
           (is= (-> (create-game)
                    (deal-damages "h1" 10)
                    (get-health "h1"))
                20)
           (is= (-> (create-game)
                    (deal-damages "p1" 10)
                    (get-health "h1"))
                20)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages "n1" 1)
                    (get-health "n1"))
                3)
           )}
  [state id value-damages]
  (or (deal-damages-to-minion state id value-damages)
      (deal-damages-to-heroe-by-heroe-id state id value-damages)
      (deal-damages-to-heroe-by-player-id state id value-damages)
      ))


(defn end-turn-effect
  "Apply the end-turn-effect of all the minions on the board which have one"
  {:test (fn []
           ; The end-turn effect of Ragnaros the Firelord is to deal 8 damages to a random enemy.
           (is= (-> (create-game [{:minions [(create-card "Ragnaros the Firelord")]}])
                    (end-turn-effect)
                    (get-health "h2"))
                22)
           (is= (-> (create-game [{:minions [(create-card "Ragnaros the Firelord")]}
                                  {:minions [(create-card "Nightblade" :id "n1")
                                             (create-card "Nightblade" :id "n2" :health 12)]}])
                    (end-turn-effect)
                    (get-health "n2"))
                4))}
  [state]
  (let [minions (get-minions state (get-player-id-in-turn state))
        function-of-the-effect (fn [a minion]
                                         (let [function-result (:effect-end-turn (get-definition (:name minion)))]
                                           (if (some? function-result)
                                             (function-result a minion)
                                             a)))]
    (reduce function-of-the-effect state minions)))

      (deal-damages-to-heroe-by-player-id state id value-damages)))

(defn damage-random-enemy
  "Add random random seed in the future"
  ;{:test (fn []
  ;         (is= (as-> (create-game [{:minions [(create-minion "Defender" :id "d" :damage-taken 0) (create-minion "Argent Protector" :id "a" :damage-taken 0)]
  ;                                   :hero (create-hero "Jaina Proudmoore" :id "h1")}]) $
  ;                    (get-in (damage-random-enemy $ "p1") [:players "p1" :minions]))
  ;              1))}
  [state enemy-id]
  (let [random-id (rand-int (+ (count (get-minions state enemy-id))1)) count (count (get-minions state enemy-id))]
    (if (= random-id count)
      (deal-damages state (get-in state [:players enemy-id :hero :id]) 1)
      (deal-damages state (get-in state [:players enemy-id :minions random-id :id]) 1))))

(defn copy-spell-of-opposite-player
  "Copies the spell card of the opposite player when used"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Lorewalker Cho" :id "lo")]}])
                    (copy-spell-of-opposite-player "Shield Slam")
                    (get-hand "p2").
                    (first):name)
                "Shield Slam"))}
  [state card]
  (if (=((get-definition card):type) :spell)
  (let [minions (get-minions state (get-player-id-in-turn state))
        function-summoned-minion-spell (fn [a minion]
                                         (let [function-result (:copy-spell-card-to-opposite-player (get-definition (:name minion)))]
                                           (if (some? function-result)
                                             (function-result a card)
                                             a)))]
    (reduce function-summoned-minion-spell state minions))
  state))
