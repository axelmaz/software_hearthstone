(ns firestone.core-test
  (:require [ysera.test :refer [is-not is= error?]]
            [clojure.test :refer [deftest is testing]]
            [firestone.core :refer [cast-spell
                                    deal-damages
                                    deal-damages-to-heroe-by-heroe-id
                                    deal-damages-to-heroe-by-player-id
                                    deal-damages-to-minion
                                    draw-for-each-damaged
                                    summon-minion
                                    use-battlecry
                                    valid-attack?]]
            [firestone.construct :refer [add-card-to-hand
                                         add-minion-to-board
                                         card-to-minion
                                         create-card
                                         create-game
                                         create-minion
                                         get-health
                                         set-effect
                                         is-effect?
                                         get-armor
                                         get-minions
                                         create-hero
                                         get-hand
                                         get-card-from-hand
                                         get-attack]]))

(defn test-valid-attack?
  {:test (fn []
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
                   (valid-attack? "p1" "n" "d-taunt"))))}[])

(defn test-deal-damages-to-minion
  {:test (fn []
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
           )}[])

(defn test-deal-damages-to-heroe-by-player-id
  {:test (fn []
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
           )}[])

(defn test-deal-damages-to-heroe-by-heroe-id
  {:test (fn []
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
                22))}[])

(defn test-deal-damages
  {:test (fn []
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
           )}[])

(defn test-draw-for-each-damaged
  {:test (fn []
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
                2))}[])

(defn test-summon-minion
  {:test (fn []
           ; play the listener effect corresponding : Steward of Darkshire give a divine-shield to the 1-health minions
           (is (-> (create-game [{:board-entities [(create-minion "Steward of Darkshire")]}])
                   (summon-minion "p1" (create-card "Defender" :id "n") 0)
                   (is-effect? "n" :divine-shield)))

           ; play the listener effect in hand corresponding
           (is= (-> (create-game [{:hand [(create-card "Blubber Baron" :id "b")]}])
                    (summon-minion "p1" (create-card "Nightblade" :id "n" :owner-id "p1") 0)
                    (get-card-from-hand "p1" "b")
                    (get-attack))
                2))}[])


(defn test-cast-spell
  {:test (fn []
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
                5))}[])

(defn test-use-battlecry
  {:test (fn []
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
                0))}[])




