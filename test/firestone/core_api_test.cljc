(ns firestone.core-api-test
  (:require [ysera.test :refer [is-not is= error?]]
            [clojure.test :refer [deftest is testing]]
            [firestone.core :refer [summon-minion]]
            [firestone.construct :refer [add-minion-to-board
                                         add-secret
                                         create-card
                                         create-game
                                         create-minion
                                         create-secret
                                         get-health
                                         get-mana
                                         get-minion
                                         get-player-id-in-turn
                                         get-minions
                                         get-hand
                                         get-attack]]
            [firestone.core-api :refer [attack-minion
                                        end-turn
                                        play-minion-card
                                        play-spell-card
                                        use-hero-power]]))

(defn test-end-turn
  {:test (fn []
           ; End twice
           (is= (-> (create-game)
                    (end-turn "p1")
                    (end-turn "p2")
                    (get-player-id-in-turn))
                "p1")
           ; Does-not work if the player is not in turn
           (error? (-> (create-game)
                       (end-turn "p2")))

           ; Reset the mana
           (is= (-> (create-game [{:mana 6}])
                    (end-turn "p1")
                    (end-turn "p2")
                    (get-mana "p1"))
                10)
           ; Reset the attacks performed
           (is= (-> (create-game [{}
                                  {:board-entities [(create-card "Nightblade" :id "n1" :attacks-performed-this-turn 1)]}])
                    (end-turn "p1")
                    (get-minion "n1")
                    (:attacks-performed-this-turn))
                0))}
  [])

(defn test-play-minion-card
  {:test (fn []
           ; Shouldn't be able to play a card when not in turn
           (error? (-> (create-game [{:hand [(create-card "Defender" :id "d")]}]
                                    :player-id-in-turn "p2")
                       (play-minion-card "p1" "d" 0)))
           ; Shouldn't not be able to play cards not in our hand
           (error? (-> (create-game)
                       (play-minion-card "p1" "n" 0)))
           ; Shouldn't be able to play a card if not enough mana
           (error? (-> (create-game [{:hand [(create-card "Nightblade" :id "n")]
                                      :mana 3}])
                       (play-minion-card "p1" "n" 0)))
           ; The mana of the player should decrease by the mana cost of the card
           (is= (-> (create-game [{:hand [(create-card "Defender" :id "d")]
                                   :mana 9}])
                    (play-minion-card "p1" "d" 0)
                    (get-mana "p1"))
                8)
           ;The battlecry of the card (if there is one) is applied
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n")]
                                   :hand [(create-card "Novice Engineer" :id "ne")]}])
                    (play-minion-card "p1" "ne" 0)
                    (get-hand "p1")
                    (first)
                    (:name))
                "Nightblade"))} [])

(defn test-play-spell-card
  {:test (fn []
           ; Shouldn't be able to play a card when not in turn
           (error? (-> (create-game [{:hand [(create-card "Battle Rage" :id "d")]}]
                                    :player-id-in-turn "p2")
                       (play-spell-card "p1" "d")))
           ; Shouldn't not be able to play cards not in our hand
           (error? (-> (create-game)
                       (play-spell-card "p1" "n")))
           ; Shouldn't be able to play a card if not enough mana
           (error? (-> (create-game [{:hand [(create-card "Battle Rage" :id "n")]
                                      :mana 1}])
                       (play-spell-card "p1" "n")))
           ; The mana of the player should decrease by the mana cost of the card
           (is= (-> (create-game [{:hand [(create-card "Battle Rage" :id "d")]
                                   :mana 9}])
                    (play-spell-card "p1" "d")
                    (get-mana "p1"))
                7))} [])

(defn test-play-attack-minion
  {:test (fn []
           ;The attack has to be invalid : the minion is sleepy
           (error? (-> (create-game)
                       (summon-minion "p1" (create-card "Novice Engineer" :id "ne") 0)
                       (summon-minion "p2" (create-card "Nightblade" :id "nb") 0)
                       (attack-minion "p2" "ne" "nb")))
           ;The attacker could not attack twice a tour
           (error? (-> (create-game)
                       (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                       (add-minion-to-board "p2" (create-minion "Nightblade" :id "nb") 0)
                       (attack-minion "p1" "ne" "nb")
                       (attack-minion "p1" "ne" "nb")))
           ; If a minion is attacked by a poisonous it should be deleted from the board.
           (is= (-> (create-game [{:board-entities [(create-minion "Maexxna" :id "m")]}
                                  {:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (attack-minion "p1" "m" "n1")
                    (get-minions "p2")
                    (count))
                0)
           ;The active secret should be played
           (is= (-> (create-game [{:board-entities [(create-minion "Nightblade" :id "n1")]}])
                    (add-secret "p2" (create-secret "Explosive Trap"))
                    (attack-minion "p1" "n1" "h2")
                    (get-health "h1"))
                28)
           ; Should not be possible to attack twice
           (error? (-> (create-game)
                       (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                       (attack-minion "p1" "ne" "h2")
                       (attack-minion "p1" "ne" "h2"))))} [])

(defn test-use-hero-power
  {:test (fn []
           ; Shouldn't be able to use the power when not in turn
           (error? (-> (create-game :player-id-in-turn "p2")
                       (use-hero-power "p1")))
           ; Shouldn't be able to use the power if not enough mana
           (error? (-> (create-game [{:mana 1}])
                       (use-hero-power "p1")))
           ; Shouldn't be able to use the power twice a tour
           (error? (-> (create-game)
                       (use-hero-power "p1")
                       (use-hero-power "p1")))
           ; The mana of the player should decrease by the mana cost of the card
           (is= (-> (create-game [{:hero "Garrosh Hellscream"}])
                    (use-hero-power "p1")
                    (get-mana "p1"))
                8)
           ;The inspire effect is applied ex Lowly Squire gain +1 attack
           (is= (-> (create-game [{:board-entities [(create-minion "Lowly Squire" :id "l")]}])
                    (use-hero-power "p1" "h2")
                    (get-attack "l"))
                2))} [])