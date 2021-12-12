(ns firestone.core
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [ysera.collections :refer [seq-contains?]]
            [clojure.string :refer [includes?]]
            [firestone.definitions :refer [get-definition]]
            [firestone.construct :refer [add-card-to-hand
                                         add-minion-to-board
                                         add-secret
                                         card-to-minion
                                         create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         create-power
                                         create-secret
                                         enough-mana?
                                         get-armor
                                         get-attack
                                         get-card-from-hand
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
                                         get-power
                                         get-random-character
                                         get-secrets
                                         get-taunt-minions-id
                                         get-total-health
                                         is-effect?
                                         listener-effect
                                         listener-effect-in-hand
                                         remove-card-from-deck
                                         remove-effect
                                         remove-minion
                                         remove-secret
                                         set-effect
                                         update-minion
                                         update-minions]]))


(defn sleepy?
  "Checks if the minion with given id is sleepy."
  {:test (fn []
           (is (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}]
                                :minion-ids-summoned-this-turn ["n"])
                   (sleepy? "n")))
           (is-not (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                       (sleepy? "n"))))}
  [state id]
  (seq-contains? (:minion-ids-summoned-this-turn state) id))

(defn can-attack?
  {:test (fn []
           ; Should not be able to attack because already attacked
           (is-not (as-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n" :attacks-performed-this-turn 1)]}]) $
                         (can-attack? $ (get-minion $ "n"))))
           ; Should not be able to attack because sleepy
           (is-not (as-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}]
                                      :minion-ids-summoned-this-turn ["n"]) $
                         (can-attack? $ (get-minion $ "n")))))}
  [state minion]
  (let [minion-id (:id minion)
        number-attack-max (if (is-effect? state minion-id :windfury)
                            2
                            1)
        player-id (:owner-id minion)]
    (and (not (sleepy? state minion-id))
         (< (:attacks-performed-this-turn minion) number-attack-max)
         (not (is-effect? minion :cant-attack))
         (= (:player-id-in-turn state) player-id)
         )))

(defn valid-attack?
  "Checks if the attack is valid"
  {:test (fn []
           ; Should be able to attack an enemy minion
           (is (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}
                                 {:board-entities [(create-minion "Defender" :id "d")]}])
                   (valid-attack? "p1" "n" "d")))
           ; Should be able to attack an enemy hero
           (is (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                   (valid-attack? "p1" "n" "h2")))
           ; Should not be able to attack your own minions
           (is-not (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")
                                                       (create-minion "Defender" :id "d")]}])
                       (valid-attack? "p1" "n" "d")))
           ; Should not be able to attack if it is not your turn
           (is-not (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}
                                     {:board-entities [(create-minion "Defender" :id "d")]}]
                                    :player-id-in-turn "p2")
                       (valid-attack? "p1" "n" "d")))
           ; Should not be able to attack if you are sleepy
           (is-not (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}
                                     {:board-entities [(create-minion "Defender" :id "d")]}]
                                    :minion-ids-summoned-this-turn ["n"])
                       (valid-attack? "p1" "n" "d")))
           ; Should not be able to attack if you already attacked this turn
           (is-not (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n" :attacks-performed-this-turn 1)]}
                                     {:board-entities [(create-minion "Defender" :id "d")]}])
                       (valid-attack? "p1" "n" "d")))
           ; Ragnaros the Firelord shouldn't be able to attack
           (is-not (-> (create-game [{:board-entities [(create-minion "Ragnaros the Firelord" :id "n")]}
                                     {:board-entities [(create-minion "Defender" :id "d")]}])
                       (valid-attack? "p1" "n" "d")))
           ; We could not attack an enemy if another has taunt
           (is-not (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}
                                     {:board-entities [(create-minion "Defender" :id "d")
                                                       (create-minion "Defender" :id "d-taunt" :states [:taunt])]}])
                       (valid-attack? "p1" "n" "d")))
           ; We could attack an enemy if this enemy has taunt
           (is (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}
                                 {:board-entities [(create-minion "Defender" :id "d")
                                                   (create-minion "Defender" :id "d-taunt" :states [:taunt])]}])
                   (valid-attack? "p1" "n" "d-taunt"))))}
  [state player-id attacker-id target-id]
  (let [attacker (get-minion state attacker-id)
        target (get-character state target-id)
        taunt-minions (get-taunt-minions-id state (get-opposing-player-id state player-id))]
    (and attacker
         (or (empty? taunt-minions) (some #{target-id} taunt-minions))
         target
         (not= (:owner-id attacker) (:owner-id target))
         (can-attack? state attacker))))

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
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :damage-taken 1)]}])
                    (update-attack "n" +2)
                    (get-attack "n"))
                6)
           ;If it is already defined
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :damage-taken 1 :attack 7)]}])
                    (update-attack "n" +2)
                    (get-attack "n"))
                9)
           ;Decrease
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :attack 7)]}])
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
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :damage-taken 1)]}])
                    (update-total-health "n" +2)
                    (get-total-health "n"))
                6)
           ;If it is already defined
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :damage-taken 1 :health 7)]}])
                    (update-total-health "n" +2)
                    (get-total-health "n"))
                9)
           ;Decrease
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :damage-taken 1 :health 7)]}])
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
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :damage-taken 3)]}])
                    (restore-health-minion "n" +2)
                    (get-health "n"))
                3)
           ;Should not restore more than total health
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :damage-taken 1)]}])
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
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 12)}])
                    (restore-health "h1" 10)
                    (get-health "h1"))
                28)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 8)}])
                    (restore-health "p1" 10)
                    (get-health "h1"))
                30)
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1" :damage-taken 2)]}])
                    (restore-health "n1" 1)
                    (get-health "n1"))
                3)
           )}
  [state id value]
  (or (restore-health-hero-by-hero-id state id value)
      (restore-health-hero-by-player-id state id value)
      (restore-health-minion state id value)))

(defn kill-if-dead
  "If a minion has a negative life, should be remove of the board"
  {:test (fn []
           ; Should remove a minion that has no life
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1" :damage-taken 4)]}])
                    (kill-if-dead "n1")
                    (get-minions "p1")
                    (count))
                0)
           ; Should return nil if a minion has no life
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (kill-if-dead "n1"))
                nil)
           )}
  [state id]
  (let [minion-health (get-health state id)]
    (if (<= minion-health 0)
      (remove-minion state id)
      nil)))

(defn kill-if-damaged-by-poisonous
  "If a minion has been damaged by a poisonous minion, should be removed of the state"
  {:test (fn []
           ; Should remove a minion has been damaged by a poisonous minion
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1" :damage-taken 4)]}
                                  {:board-entities [(create-minion "Maexxna" :id "m")]}])
                    (kill-if-damaged-by-poisonous "n1" (create-minion "Maexxna" :id "m"))
                    (get-minions "p1")
                    (count))
                0)
           ; Should return the state if attacker-id is nil
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1" :damage-taken 4)]}
                                  {:board-entities [(create-minion "Maexxna" :id "m")]}])
                    (kill-if-damaged-by-poisonous "n1" nil))
                (create-game [{:board-entities [(create-minion "Nightblade" :id "n1" :damage-taken 4)]}
                              {:board-entities [(create-minion "Maexxna" :id "m")]}]))
           ; Should return the state if the attacker is not poisonous
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1" :damage-taken 4)]}
                                  {:board-entities [(create-minion "Defender" :id "m")]}])
                    (kill-if-damaged-by-poisonous "n1" "m"))
                (create-game [{:board-entities [(create-minion "Nightblade" :id "n1" :damage-taken 4)]}
                              {:board-entities [(create-minion "Defender" :id "m")]}]))
           )}
  [state victim-id attacker]
  (if (and (some? attacker) (is-effect? attacker :poisonous))
    (remove-minion state victim-id)
    state))


(defn deal-damages-to-minion
  "Deal the value of damage to the corresponding minion"
  {:test (fn []
           ;A minion with the given id should take damage so its life decreases
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages-to-minion "n1" 1 {})
                    (get-health "n1"))
                3)
           ;If the minion has a divine-shield it does not loose any life point
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (set-effect "n1" :divine-shield)
                    (deal-damages-to-minion "n1" 1 {})
                    (get-health "n1"))
                4)
           ; Same with Argent Squire that already have a divine shield
           (is= (-> (create-game [{:board-entities [(create-minion "Argent Squire" :id "n1")]}])
                    (deal-damages-to-minion "n1" 1 {})
                    (get-health "n1"))
                1)
           ;If the minion has a divine-shield it should loose it
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (set-effect "n1" :divine-shield)
                    (deal-damages-to-minion "n1" 1 {})
                    (is-effect? "n1" :divine-shield))
                false)
           ; Same with Argent Squire that already have a divine shield
           (is= (-> (create-game [{:board-entities [(create-minion "Argent Squire" :id "n1")]}])
                    (deal-damages-to-minion "n1" 1 {})
                    (is-effect? "n1" :divine-shield))
                false)
           ;If the id doesn't correspond we return nil (uselful for deal-damages function
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages-to-minion "doesn't exist" 1 {}))
                nil)
           ;Is the effect of Armorsmith working ?
           (is= (-> (create-game [{:board-entities [(create-card "Armorsmith" :id "a")
                                                    (create-card "Nightblade" :id "n")]}])
                    (deal-damages-to-minion "n" 1 {})
                    (get-armor "h1"))
                1)
           ; Is a minion without life deleted from the board ?
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages-to-minion "n1" 4 {})
                    (get-minions "p1")
                    (count))
                0)
           ; Is a minion damaged by a poisonous other deleted from the board ?
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}
                                  {:board-entities [(create-minion "Maexxna" :id "m")]}])
                    (deal-damages-to-minion "n1" 1 {:minion-attacker (create-minion "Maexxna" :id "m")})
                    (get-minions "p1")
                    (count))
                0)
           ;A minion already damaged should be more damaged
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1" :damage-taken 1)]}])
                    (deal-damages-to-minion "n1" 1 {})
                    (get-health "n1"))
                2)
           )}
  [state minion-id value-damages other-args]
  (let [is-minion-id? (reduce (fn [a v]
                                (if (= v minion-id)
                                  true
                                  a))
                              false
                              (map :id (get-minions state)))]
    (if-not is-minion-id?                                   ; test if minion-id is the id of a minion
      nil
      (if (is-effect? state minion-id :divine-shield)
        (remove-effect state minion-id :divine-shield)
        (as-> state $
              (listener-effect $ :states-minion-takes-damage {:minion-takes-damage (get-minion state minion-id)})
              (update-minion $ minion-id :damage-taken (fn [damage-taken] (+ (or damage-taken 0) value-damages)))
              (or (kill-if-dead $ minion-id)
                  (kill-if-damaged-by-poisonous $ minion-id (:minion-attacker other-args))))))))

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
                nil))}
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
                    (deal-damages "h1" 10 {})
                    (get-health "h1"))
                20)
           (is= (-> (create-game)
                    (deal-damages "p1" 10 {})
                    (get-health "h1"))
                20)
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages "n1" 1 {})
                    (get-health "n1"))
                3)
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (deal-damages "n1" 4 {})
                    (get-minions "p1")
                    (count))
                0)
           ;A that has a divine shield should not lose it if it takes 0 damages
           (is (-> (create-game [{:board-entities [(create-minion "Argent Squire" :id "n1")]}])
                   (deal-damages "n1" 0 {})
                   (is-effect? "n1" :divine-shield)))
           )}
  [state id value-damages other-args]
  (if (= value-damages 0)
    state
    (or (deal-damages-to-minion state id value-damages other-args)
        (deal-damages-to-heroe-by-heroe-id state id value-damages)
        (deal-damages-to-heroe-by-player-id state id value-damages))))

(defn damage-random
  "Add random random seed in the future"
  {:test (fn []
           ; With this seed, it is the Nightblade that take a damage.
           (is= (-> (create-game [{:board-entities [(create-minion "Armorsmith" :id "a")
                                                    (create-minion "Nightblade" :id "n")]
                                   :hero           (create-hero "Jaina Proudmoore" :id "h1")}])
                    (damage-random 1 "p1")
                    (get-health "n"))
                3)
           ; With this seed, it is the h2 that take a damage.
           (is= (-> (create-game [{:board-entities [(create-minion "Armorsmith" :id "a")
                                                    (create-minion "Nightblade" :id "n")]
                                   :hero           (create-hero "Jaina Proudmoore" :id "h1")}])
                    (damage-random 1)
                    (get-health "h2"))
                29))}
  ([state value-damages player-targeted-id]
   (let [random-id (:id (get-random-character state player-targeted-id))]
     (deal-damages state random-id value-damages {})))
  ([state value-damages]
   (let [random-id (:id (get-random-character state))]
     (deal-damages state random-id value-damages {}))))


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
    (let [fatigue (get-in state [:players player-id :hero :fatigue] 1)]
      (-> state
          (deal-damages player-id fatigue {})
          (assoc-in [:players player-id :hero :fatigue] (+ 1 fatigue))))
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
                15))}
  [state player-id number-of-cards]
  (if (<= number-of-cards 0)
    state
    (draw-cards (draw-card state player-id) player-id (- number-of-cards 1))))

(defn draw-for-each-damaged
  "Make the player draw for each minion damaged he has"
  {:test (fn []
           ;No damaged minion = no draw
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n1")]}])
                    (draw-for-each-damaged "p1")
                    (get-card-from-hand "p1" "n1"))
                nil)
           (is= (-> (create-game [{:deck           [(create-card "Nightblade" :id "n1")]
                                   :board-entities [(create-minion "Argent Protector")]}])
                    (draw-for-each-damaged "p1")
                    (get-card-from-hand "p1" "n1")
                    (count))
                0)
           ;if one minion is damaged then draw a card.
           (is= (-> (create-game [{:deck           [(create-card "Nightblade" :id "n1")
                                                    (create-card "Nightblade" :id "n2")]
                                   :board-entities [(create-minion "Argent Protector" :damage-taken 1)]}])
                    (draw-for-each-damaged "p1")
                    (get-hand "p1")
                    (count))
                1)
           ;if two minions are damaged then draw two card.
           (is= (-> (create-game [{:deck           [(create-card "Nightblade" :id "n1")
                                                    (create-card "Nightblade" :id "n2")]
                                   :board-entities [(create-minion "Argent Protector" :damage-taken 1 :id "a1")
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


(defn summon-minion
  "Summon the given minion card to the board at the given position (and play the effect if there is one)"
  {:test (fn []
           ; Adding a minion to an empty board
           (is= (as-> (create-game) $
                      (summon-minion $ "p1" (create-card "Nightblade" :id "n") 0)
                      (get-minions $ "p1")
                      (map (fn [m] {:name (:name m)}) $))
                [{:name "Nightblade"}])
           ; play the listener effect corresponding : Knife Juggler give on damage to the hero enemy
           (is= (-> (create-game [{:board-entities [(create-minion "Armorsmith")
                                                    (create-minion "Knife Juggler" :owner-id "p1")]}])
                    (summon-minion "p1" (create-card "Nightblade" :id "n") 0)
                    (get-health "h2"))
                29)
           ; play the listener effect corresponding : Steward of Darkshire give a divine-shield to the 1-health minions
           (is (-> (create-game [{:board-entities [(create-minion "Steward of Darkshire")]}])
                   (summon-minion "p1" (create-card "Defender" :id "n") 0)
                   (is-effect? "n" :divine-shield)))

           ; play the listener effect in hand corresponding
           (is= (-> (create-game [{:hand [(create-card "Blubber Baron" :id "b")]}])
                    (summon-minion "p1" (create-card "Nightblade" :id "n" :owner-id "p1") 0)
                    (get-card-from-hand "p1" "b")
                    (get-attack))
                2))}

  [state player-id card position]
  (let [minion (card-to-minion card)]
    (-> state
        (add-minion-to-board player-id minion position)
        (update
          :minion-ids-summoned-this-turn
          (fn [ids]
            (conj ids (:id minion))))
        (listener-effect :states-summon-minion {:player-summon player-id :minion-summoned minion})
        (listener-effect-in-hand :states-summon-minion-in-hand {:card-minion-summoned card})
        )))

(defn cast-spell
  "Summon the given minion card to the board at the given position (and play the effect if there is one"
  {:test (fn []
           ; play the listener effect corresponding
           (is= (-> (create-game [{:board-entities [(create-minion "Lorewalker Cho")]}])
                    (cast-spell (create-card "Battle Rage" :owner-id "p1"))
                    (get-hand "p2")
                    (first)
                    (:name))
                "Battle Rage")
           ; effect of battle rage should be done
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :damage-taken 1)]
                                   :deck           [(create-card "Defender")]}])
                    (cast-spell (create-card "Battle Rage" :owner-id "p1"))
                    (get-hand "p1")
                    (first)
                    (:name))
                "Defender")
           ; effect of Whirlwind should be done
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n")]}])
                    (cast-spell (create-card "Whirlwind" :owner-id "p1"))
                    (get-health "n"))
                3)
           ; effect of Shield Slam should be done
           (is= (as-> (create-game [{:hero (create-hero "Jaina Proudmoore" :armor 2)}
                                    {:board-entities [(create-minion "Nightblade" :id "n")]}]) $
                      (cast-spell $ (create-card "Shield Slam" :owner-id "p1") "n")
                      (get-health $ "n"))
                2)
           ; Test "Blessed champion effect : should double the attack
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n")]
                                   :hand           [(create-card "Blessed Champion" :id "ne")]}])
                    (cast-spell (create-card "Blessed Champion") "n")
                    (get-attack "n"))
                8)
           ; Test "Bananas effect : should give +1/+1
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n")]}])
                    (cast-spell (create-card "Bananas") "n")
                    (get-attack "n"))
                5)
           )}
  ([state card]
   (let [spell-function ((get-definition card) :states-spell)]
     (if spell-function
       (-> state
           (listener-effect :states-cast-spell {:card-spell-casted card})
           (spell-function {:spell-played card}))
       (listener-effect state :states-cast-spell {:card-spell-casted card}))))
  ([state card target-minion-id]
   (let [spell-function (:states-spell (get-definition (:name card)))]
     (if spell-function
       (-> state
           (listener-effect :states-cast-spell {:card-spell-casted card})
           (spell-function {:target-id target-minion-id :spell-played card}))
       (listener-effect state :states-cast-spell {:card-spell-casted card})))))

(defn give-minion-plus-attack-and-health
  "Give a targeted minion +value/+value"
  {:test (fn []
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
                    (give-minion-plus-attack-and-health "n1" 1)
                    (get-attack "n1"))
                5)
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
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
           (is (-> (create-game [{:hand           [(create-card "Argent Protector" :owner-id "p1" :id "a")]
                                  :board-entities [(create-minion "Defender" :id "d")
                                                   (create-minion "Defender" :id "d2")]}])
                   (use-battlecry (create-card "Argent Protector" :owner-id "p1" :id "a") "d")
                   (is-effect? "d" :divine-shield)))
           ; The battlecry of "Argent Protector" does not give divine shield to not targeted minion
           (is-not (-> (create-game [{:hand           [(create-card "Argent Protector" :owner-id "p1" :id "a")]
                                      :board-entities [(create-minion "Defender" :id "d")
                                                       (create-minion "Defender" :id "d2")]}])
                       (use-battlecry (create-card "Argent Protector" :owner-id "p1" :id "a") "d")
                       (is-effect? "d2" :divine-shield)))
           ; The battlecry of "Argent Protector" should give an error if we try to target an invalid minion
           (error? (-> (create-game [{:board-entities [(create-minion "Defender" :id "d")]}])
                       (add-minion-to-board "p2" (create-minion "Defender" :id "d2") 0)
                       (use-battlecry (create-card "Argent Protector" :owner-id "p1") "d2")
                       (is-effect? "d2" :divine-shield)))
           ; If the card doesn't have battlecry (as Defender for exemple), the state should not change
           (is= (-> (create-game)
                    (use-battlecry "Defender"))
                (create-game))
           ; Test "Earthen Ring Farseer"
           (is= (-> (create-game [{:board-entities [(create-minion "Defender" :id "d" :health 10 :damage-taken 5)]}])
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
           ; Test Deathwing
           ; Should remove all the card of the player
           (is= (-> (create-game [{:hand [(create-card "Nightblade" :id "n1")
                                          (create-card "Nightblade" :id "n2")]}
                                  {:hand [(create-card "Nightblade" :id "n3")
                                          (create-card "Nightblade" :id "n4")]}])
                    (use-battlecry (create-card "Deathwing" :owner-id "p1"))
                    (get-hand "p1")
                    (count))
                0)
           ; Should destroy all the minions
           (is= (as-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")
                                                      (create-minion "Nightblade" :id "n2")]}
                                    {:board-entities [(create-minion "Nightblade" :id "n3")
                                                      (create-minion "Nightblade" :id "n4")]}]) $
                      (use-battlecry $ (create-card "Deathwing" :owner-id "p1"))
                      (get-minions $)
                      (count $))
                0)
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
  "reset the stuff for the new turn : mana, attacks-performed-this-turn, sleepy"
  {:test (fn []
           (is= (-> (create-game [{:hero {:mana 6}}])
                    (start-turn-reset)
                    (get-mana "p1"))
                10)
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1" :attacks-performed-this-turn 1)]}])
                    (start-turn-reset)
                    (get-minion "n1")
                    (:attacks-performed-this-turn))
                0)
           (is-not (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1" :attacks-performed-this-turn 1)]}]
                                    :minion-ids-summoned-this-turn ["n1"])
                       (start-turn-reset)
                       (sleepy? "n1"))))}
  [state]
  (-> state
      (assoc :minion-ids-summoned-this-turn [])
      (assoc-in [:players (get-player-id-in-turn state) :mana] 10)
      (update-minions (map :id (get-minions state (get-player-id-in-turn state))) :attacks-performed-this-turn 0)
      (assoc-in [:players (get-player-id-in-turn state) :hero :power :used-this-turn] 0)))


(defn valid-power?
  "Checks if the use-of the power is valid"
  {:test (fn []
           ; Should not be able to use your power if it is not your turn
           (is-not (-> (create-game)
                       (valid-power? "p2")))
           ; Should not be able to use your power if have you already used your power this turn
           (is-not (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :power (create-power "Fireblast" :used-this-turn 1))}])
                       (valid-power? "p1")))
           ; Should not be able to use your power if you don't have enough mana
           (is-not (-> (create-game [{:mana 1}])
                       (valid-power? "p1")))
           ; Should in other cases
           (is (-> (create-game [{:mana 2}])
                   (valid-power? "p1"))))}
  [state player-id]
  (let [power (get-power state player-id)
        max-number-use 1]
    (and (enough-mana? state player-id power) (< (:used-this-turn power) max-number-use) (= player-id (get-player-id-in-turn state)))))


(defn secret-effect
  "Apply the effect of the secret which correspond to the event"
  {:test (fn []
           ; The effect of the secret is applied
           (is= (-> (create-game)
                    (add-secret "p2" (create-secret "Explosive Trap"))
                    (secret-effect :secret-attack {:attacked-character (create-hero "Jaina Proudmoore" :id "h2")})
                    (get-health "h1"))
                28)
           ;But just once and then disapear
           (is= (-> (create-game)
                    (add-secret "p2" (create-secret "Explosive Trap"))
                    (secret-effect :secret-attack {:attacked-character (create-hero "Jaina Proudmoore" :id "h2")})
                    (get-secrets "p2"))
                []))}
  ([state event other-args]
   (let [secrets (get-secrets state)
         function-of-the-effect (fn [a secret]
                                  (let [function-result (event (get-definition (:name secret)))]
                                    (if (and (some? function-result))
                                      (-> a
                                          (function-result (assoc other-args :secret-played secret))
                                          (remove-secret (:owner-id secret) (:id secret)))
                                      a)))]
     (reduce function-of-the-effect state secrets)))
  ([state event]
   (listener-effect state event {})))