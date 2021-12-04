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
                                         get-deck
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
                                         remove-minion
                                         set-divine-shield
                                         update-minion
                                         update-minions]]))


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
           ; :effect-cast-spell effect test
           (is= (-> (create-game [{:minions [(create-card "Lorewalker Cho" :id "a")]}])
                    (listener-effect :effect-cast-spell {:card-spell-casted (create-card "Battle Rage" :id "b" :owner-id "p1")})
                    (get-hand "p2")
                    (first)
                    (:name))
                "Battle Rage"))}
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

(defn restore-health-minion
  "decrease :damage-taken by the value. :damage-taken could not be negative."
  {:test (fn []
           ; Should restore health of a damaged minion
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 3)]}])
                    (restore-health-minion "n" +2)
                    (get-health "n"))
                3)
           ;Should not restore more than total health
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 1)]}])
                    (restore-health-minion "n" +10)
                    (get-health "n"))
                4))}
  [state minion-id value]
  (let [damages-taken (:damage-taken (get-minion state minion-id))
        value-restore-max (min value damages-taken)
        new-damages-taken (- damages-taken value-restore-max)]
    (update-minion state minion-id :damage-taken new-damages-taken)))

(defn restore-health-hero-by-player-id
  "decrease :damage-taken by the value. :damage-taken could not be negative."
  {:test (fn []
           ;should work for hero as well
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 5)}])
                    (restore-health-hero-by-player-id "p1" +3)
                    (get-health "h1"))
                28)
           ;should return nil if the id is false
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 5)}])
                    (restore-health-hero-by-player-id "no-id" +3))
                nil)
           )}
  [state player-id value]
  (let [is-player-id? (reduce (fn [a v]
                                (if (= v player-id)
                                  true
                                  a))
                              false
                              (map :id (get-players state)))]
    (if-not is-player-id?                                   ; test if player-id is the id of a player
      nil
      (let [damages-taken (:damage-taken (get-character state (get-hero-id-from-player-id state player-id)))
            value-restore-max (min value damages-taken)]
        (update-in state [:players player-id :hero :damage-taken] - value-restore-max))
      )))

(defn restore-health-hero-by-hero-id
  "decrease :damage-taken by the value. :damage-taken could not be negative."
  {:test (fn []
           ;should work for hero as well
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 5)}])
                    (restore-health-hero-by-hero-id "h1" +3)
                    (get-health "h1"))
                28)
           ;should return nil if the id is false
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 5)}])
                    (restore-health-hero-by-hero-id "fake-id" +3))
                nil)
           )}
  [state hero-id value]
  (restore-health-hero-by-player-id state (get-player-id-from-heroe-id state hero-id) value))

(defn restore-health
  "Deal the value of damage to the corresponding character"
  {:test (fn []
           (is= (-> (create-game[{:hero (create-hero "Jaina Proudmoore" :damage-taken 12)}])
                    (restore-health "h1" 10)
                    (get-health "h1"))
                28)
           (is= (-> (create-game[{:hero (create-hero "Jaina Proudmoore" :damage-taken 8)}])
                    (restore-health "p1" 10)
                    (get-health "h1"))
                30)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1" :damage-taken 2)]}])
                    (restore-health "n1" 1)
                    (get-health "n1"))
                3)
           )}
  [state id value]
  (or (restore-health-hero-by-hero-id state id value)
      (restore-health-hero-by-player-id state id value)
      (restore-health-minion state id value)))

(defn kill-if-dead
  "Deal the value of damage to the corresponding character"
  {:test (fn []
           ; Should remove a minion that has no life
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1" :damage-taken 4)]}])
                    (kill-if-dead "n1")
                    (get-minions "p1")
                    (count))
                0)
           ; Should remove a minion that has no life
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (kill-if-dead "n1")
                    (get-minions "p1")
                    (count))
                1)
           )}
  [state id]
  (let [minion-health (get-health state id)]
    (if (<= minion-health 0)
      (remove-minion state id)
      state)))


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
           ; Same with Argent Squire that already have a divine shield
           (is= (-> (create-game [{:minions [(create-minion "Argent Squire" :id "n1")]}])
                    (deal-damages-to-minion "n1" 1)
                    (get-health "n1"))
                1)
           ;If the minion has a divine-shield it should loose it
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (set-divine-shield "n1")
                    (deal-damages-to-minion "n1" 1)
                    (is-divine-shield? "n1"))
                false)
           ; Same with Argent Squire that already have a divine shield
           (is= (-> (create-game [{:minions [(create-minion "Argent Squire" :id "n1")]}])
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
           ; Is a minion without life deleted from the board ?
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages-to-minion "n1" 4)
                    (get-minions "p1")
                    (count))
                0)
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
            (update-minion minion-id :damage-taken value-damages)
            (kill-if-dead minion-id))))))

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
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages "n1" 4)
                    (get-minions "p1")
                    (count))
                0)
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
                                   :hero    (create-hero "Jaina Proudmoore" :id "h1")}])
                    (damage-random 1 "p1")
                    (get-health "n"))
                3)
           ; With this seed, it is the h2 that take a damage.
           (is= (-> (create-game [{:minions [(create-minion "Armorsmith" :id "a")
                                             (create-minion "Nightblade" :id "n")]
                                   :hero    (create-hero "Jaina Proudmoore" :id "h1")}])
                    (damage-random 1)
                    (get-health "h2"))
                29))}
  ([state value-damages player-targeted-id]
   (let [random-id (:id (get-random-character state player-targeted-id))]
     (deal-damages state random-id value-damages)))
  ([state value-damages]
   (let [random-id (:id (get-random-character state))]
     (deal-damages state random-id value-damages))))

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

(defn cast-spell
  "Summon the given minion card to the board at the given position (and play the effect if there is one"
  {:test (fn []
           ; play the listener effect corresponding
           (is= (-> (create-game [{:minions [(create-minion "Lorewalker Cho")]}])
                    (cast-spell (create-card "Battle Rage" :owner-id "p1"))
                    (get-hand "p2")
                    (first)
                    (:name))
                "Battle Rage")
           ; effect of battle rage should be done
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :damage-taken 1)]
                                   :deck    [(create-card "Defender")]}])
                    (cast-spell (create-card "Battle Rage" :owner-id "p1"))
                    (get-hand "p1")
                    (first)
                    (:name))
                "Defender")
           ; effect of Whirlwind should be done
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (cast-spell (create-card "Whirlwind" :owner-id "p1"))
                    (get-health "n"))
                3)
           ; effect of Shield Slam should be done
           (is= (as-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 2)}
                                    {:minions [(create-minion "Nightblade" :id "n")]}]) $
                      (cast-spell $ (create-card "Shield Slam" :owner-id "p1") "n")
                      (get-health $ "n"))
                2)
           ; Test "Blessed champion effect : should double the attack
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n")]
                                   :hand    [(create-card "Blessed Champion" :id "ne")]}])
                    (cast-spell (create-card "Blessed Champion") "n")
                    (get-attack "n"))
                8)
           ; Test "Bananas effect : should give +1/+1
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n")]}])
                    (cast-spell (create-card "Bananas") "n")
                    (get-attack "n"))
                5)
           )}
  ([state card]
   (let [spell-function ((get-definition card) :effect-spell)]
     (if spell-function
       (-> state
           (listener-effect :effect-cast-spell {:card-spell-casted card})
           (spell-function {:spell-played card}))
       (listener-effect state :effect-cast-spell {:card-spell-casted card}))))
  ([state card target-minion-id]
   (let [spell-function (:effect-spell (get-definition card))]
     (if spell-function
       (-> state
           (listener-effect :effect-cast-spell {:card-spell-casted card})
           (spell-function {:target-id target-minion-id :spell-played card}))
       (listener-effect state :effect-cast-spell {:card-spell-casted card})))))

(defn give-minion-plus-attack-and-health
  "Give a targeted minion +value/+value"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n1")]}])
                    (give-minion-plus-attack-and-health "n1" 1)
                    (get-attack "n1"))
                5)
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n1")]}])
                    (give-minion-plus-attack-and-health "n1" 1)
                    (get-total-health "n1"))
                5))}
  [state minion-id value]
  (-> state
      (update-attack minion-id value)
      (update-total-health minion-id value)))

(defn use-battlecry
  {:test (fn []
           ; The battlecry of Novice Engineer is to draw a card so we test if we obtain a card in our hand
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n")]}])
                    (use-battlecry "Novice Engineer")
                    (get-hand "p1")
                    (first)
                    (:name))
                "Nightblade")
           ; We also test if the card is removed from the deck
           (empty? (-> (create-game [{:deck [(create-card "Nightblade" :id "n")]}])
                       (use-battlecry "Novice Engineer")
                       (get-deck "p1")
                       ))
           ; The battlecry of Nightblade is to Deal 3 damage to the enemy hero so we test if the enememy heroe's life decrease.
           (is= (-> (create-game)
                    (use-battlecry "Nightblade")
                    (get-health "h2"))
                27)
           ; The battlecry of "Argent Protector" is to give a divine shield to a targeted minion (a friendly one)
           (is (-> (create-game [{:hand    [(create-card "Argent Protector" :owner-id "p1" :id "a")]
                                  :minions [(create-minion "Defender" :id "d")
                                            (create-minion "Defender" :id "d2")]}])
                   (use-battlecry (create-card "Argent Protector" :owner-id "p1" :id "a") "d")
                   (is-divine-shield? "d")))
           ; The battlecry of "Argent Protector" does not give divine shield to not targeted minion
           (is-not (-> (create-game [{:hand    [(create-card "Argent Protector" :owner-id "p1" :id "a")]
                                      :minions [(create-minion "Defender" :id "d")
                                                (create-minion "Defender" :id "d2")]}])
                       (use-battlecry (create-card "Argent Protector" :owner-id "p1" :id "a") "d")
                       (is-divine-shield? "d2")))
           ; The battlecry of "Argent Protector" should give an error if we try to target an invalid minion
           (error? (-> (create-game [{:minions [(create-minion "Defender" :id "d")]}])
                       (add-minion-to-board "p2" (create-minion "Defender" :id "d2") 0)
                       (use-battlecry (create-card "Argent Protector" :owner-id "p1") "d2")
                       (is-divine-shield? "d2")))
           ; If the card doesn't have battlecry (as Defender for exemple), the state should not change
           (is= (-> (create-game)
                    (use-battlecry "Defender"))
                (create-game))
           ; Test "Earthen Ring Farseer"
           (is= (-> (create-game [{:minions [(create-minion "Defender" :id "d" :health 10 :damage-taken 5)]}])
                    (use-battlecry (create-card "Earthen Ring Farseer" :owner-id "p1") "d")
                    (get-health "d"))
                8)
           ; Test King Mukla
           (is= (-> (create-game)
                    (use-battlecry (create-card "King Mukla" :owner-id "p1"))
                    (get-hand "p2")
                    (first)
                    (:name))
                "Bananas")
           (is= (-> (create-game)
                    (use-battlecry (create-card "King Mukla" :owner-id "p1"))
                    (get-hand "p2")
                    (count))
                2)
           )}
  ([state card]
   (let [battlecry-function ((get-definition card) :battlecry)]
     (if battlecry-function
       (battlecry-function state {:played-card card}) state)))
  ([state card target-id]
   (let [battlecry-function ((get-definition card) :battlecry)]
     (if battlecry-function
       (battlecry-function state {:played-card card :target-id target-id}) state))
   ))

(defn start-turn-reset
  "reset the stuff for the new turn : mana, attacks-performed-this-turn"
  {:test (fn []
           (is= (-> (create-game [{:mana 6}])
                    (start-turn-reset)
                    (get-mana "p1"))
                10)
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n1" :attacks-performed-this-turn 1)]}])
                    (start-turn-reset)
                    (get-minion "n1")
                    (:attacks-performed-this-turn))
                0))}
  [state]
  (-> state
    (assoc-in [:players (get-player-id-in-turn state) :mana] 10)
    (update-minions (map :id (get-minions state (get-player-id-in-turn state))) :attacks-performed-this-turn 0)))