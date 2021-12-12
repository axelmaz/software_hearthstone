(ns firestone.construct-test
  (:require [ysera.test :refer [is-not is= error?]]
            [clojure.test :refer [deftest is testing]]
            [firestone.construct :refer [add-card-to-hand
                                         add-minion-to-board
                                         card-to-minion
                                         create-card
                                         create-empty-state
                                         create-game
                                         create-hero
                                         create-minion
                                         create-power
                                         enough-mana?
                                         friendly?
                                         get-armor
                                         get-attack
                                         get-card-from-hand
                                         get-hand
                                         get-health
                                         get-minions
                                         get-owner-id
                                         get-random-character
                                         is-effect?
                                         listener-effect
                                         remove-effect
                                         set-effect]]))

(defn test-add-minion-to-board
  {:test (fn []
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
             (is= (:counter state) 3))
           )}[])

(defn test-get-random-character
  {:test (fn []
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
                "Nightblade"))}[])

(defn test-get-health
  {:test (fn []

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
                25))}[])

(defn test-is-effect?
  {:test (fn []
           ;Should not if silenced
           (is-not (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
                       (set-effect "n1" :divine-shield)
                       (set-effect "n1" :silenced)
                       (is-effect? "n1" :divine-shield)))
           ;Should no longer have divine-shield if we remove the effect
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n1")]}])
                    (set-effect "n1" :divine-shield)
                    (remove-effect "n1" :divine-shield)
                    (is-effect? "n1" :divine-shield))
                false) )}[])

(defn test-enough-mana?
  {:test (fn []
           (is-not (-> (create-game [{:mana 1}])
                       (enough-mana? "p1" (create-power "Fireblast" :id "f"))))
           (is (-> (create-game [{:mana 2}])
                   (enough-mana? "p1" (create-power "Fireblast")))))}[])


(defn test-get-owner-id
  {:test (fn []
           ;getting owner id from board-entities should work
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           ;getting owner id from card in hand should work
           (is= (-> (create-game [{:hand [(create-card "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           ;getting owner id from a card in the deck should work
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n1")]}])
                    (get-owner-id "n1"))
                "p1")
           ;getting the owner id from an id that does not exist in the game (state) should lead to error
           (error? (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                       (get-owner-id "bad-id"))) )}[])


(defn test-friendly?
  {:test (fn []
           (is (-> (create-game [{:hand [(create-card "Nightblade" :id "n1")]}])
                   (friendly? "n1" "h1")))
           (is-not (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                       (friendly? "n1" "h2")))
           (error? (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                       (friendly? "n1" "bad-id"))))}[])

(defn test-listener-effect
  {:test (fn []
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
                3)
           ;Test silenced
           (is= (-> (create-game [{:board-entities [(create-minion "Defender")
                                                    (create-minion "Doomsayer" :id "d")]}
                                  {:board-entities [(create-minion "Defender")]}])
                    (set-effect "d" :silenced)
                    (listener-effect :states-start-turn)
                    (get-minions)
                    (count))
                3))}[])
















