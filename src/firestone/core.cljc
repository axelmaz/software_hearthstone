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
                                         get-armor
                                         get-attack
                                         get-character
                                         get-hand
                                         get-health
                                         get-hero-id-from-player-id
                                         get-heroes
                                         get-mana
                                         get-minion
                                         get-minions
                                         get-opposing-player-id
                                         get-player-id-from-heroe-id
                                         get-player-id-in-turn
                                         get-players
                                         get-random-character
                                         get-total-health
                                         is-divine-shield?
                                         remove-divine-shield
                                         set-divine-shield
                                         update-minion]]))


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

(defn listener-effect
  "Apply the effect of the listener which correspond to the event of all the minions on the board which have one"
  {:test (fn []
           ; The end-turn effect of Ragnaros the Firelord is to deal 8 damages to a random enemy.
           (is= (-> (create-game [{:minions [(create-card "Ragnaros the Firelord")]}])
                    (listener-effect :effect-end-turn)
                    (get-health "h2"))
                22)
           (is= (-> (create-game [{:minions [(create-card "Ragnaros the Firelord")]}
                                  {:minions [(create-card "Nightblade" :id "n1")
                                             (create-card "Nightblade" :id "n2" :health 12)]}])
                    (listener-effect :effect-end-turn)
                    (get-health "n2"))
                4)
           ; The damaged minion effect of Armorsmith is to give 1 armor to the hero every-time a friendly-minion take damage
           (is= (-> (create-game [{:minions [(create-card "Armorsmith" :id "a")
                                             (create-card "Nightblade" :id "n")]}])
                    (listener-effect :effect-minion-takes-damage {:minion-takes-damage (create-minion "Nightblade" :id "n" :owner-id "p1")})
                    (get-armor "h1"))
                1)
           (is= (-> (create-game [{:minions [(create-card "Armorsmith" :id "a")
                                             (create-card "Nightblade" :id "n")
                                             (create-card "Armorsmith" :id "a")]}])
                    (listener-effect :effect-minion-takes-damage {:minion-takes-damage (create-minion "Nightblade" :id "n" :owner-id "p1")})
                    (get-armor "h1"))
                2)
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "a")
                                             (create-card "Nightblade" :id "n")
                                             (create-card "Nightblade" :id "m")]}])
                    (listener-effect :effect-minion-takes-damage {:minion-takes-damage (create-minion "Nightblade" :id "n" :owner-id "p1")})
                    (get-armor "h1"))
                0)
           )}
  ([state event other-args]
   (let [minions (get-minions state (get-player-id-in-turn state))
         function-of-the-effect (fn [a minion]
                                  (let [function-result (event (get-definition (:name minion)))]
                                    (if (some? function-result)
                                      (function-result a (assoc other-args :minion-play-effect minion))
                                      a)))]
     (reduce function-of-the-effect state minions)))
  ([state event]
   (listener-effect state event {})))

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
            (listener-effect :effect-minion-takes-damage {:minion-takes-damage (get-minion state minion-id)})
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

(defn damage-random
  "Add random random seed in the future"
  {:test (fn []
           ; With this seed, it is the Nightblade that take a damage.
           (is= (-> (create-game [{:minions [(create-minion "Armorsmith" :id "a")
                                             (create-minion "Nightblade" :id "n")]
                                     :hero   (create-hero "Jaina Proudmoore" :id "h1")}])
                    (damage-random  1 "p1")
                    (get-health "n"))
                3)
           ; With this seed, it is the h2 that take a damage.
           (is= (-> (create-game [{:minions [(create-minion "Armorsmith" :id "a")
                                             (create-minion "Nightblade" :id "n")]
                                   :hero   (create-hero "Jaina Proudmoore" :id "h1")}])
                    (damage-random  1)
                    (get-health "h2"))
                29))}
  ([state value-damages player-targeted-id]
  (let [random-id (:id(get-random-character state player-targeted-id))]
    (deal-damages state random-id value-damages)))
  ([state value-damages]
   (let [random-id (:id(get-random-character state))]
     (deal-damages state random-id value-damages))))

(defn copy-spell-of-opposite-player
  "Copies the spell card of the opposite player when used"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Lorewalker Cho" :id "lo")]}])
                    (copy-spell-of-opposite-player "Shield Slam")
                    (get-hand "p2")
                    (first) :name)
                "Shield Slam"))}
  [state card]
  (if (= ((get-definition card) :type) :spell)
    (let [minions (get-minions state (get-player-id-in-turn state))
          function-summoned-minion-spell (fn [a minion]
                                           (let [function-result (:copy-spell-card-to-opposite-player (get-definition (:name minion)))]
                                             (if (some? function-result)
                                               (function-result a card)
                                               a)))]
      (reduce function-summoned-minion-spell state minions))
    state))

(defn summon-minion
  "Summon the given minion card to the board at the given position (and play the effect if there is one"
  {:test (fn []
           ; Adding a minion to an empty board
           (is= (as-> (create-game) $
                      (summon-minion $ "p1" (create-minion "Nightblade" :id "n") 0)
                      (get-minions $ "p1")
                      (map (fn [m] {:id (:id m) :name (:name m)}) $))
                [{:id "n" :name "Nightblade"}])
           ; play the listener effect corresponding
           (is= (-> (create-game [{:minions [(create-minion "Armorsmith" :id "a")
                                             (create-minion "Nightblade" :id "n")]}])
                    (summon-minion "p1" (create-card "Knife Juggler") 0)
                    (get-health "h2"))
                29)
           )}
  [state player-id card position]
  (-> state
      (add-minion-to-board player-id card position)
      (listener-effect :effect-summon-minion)))
