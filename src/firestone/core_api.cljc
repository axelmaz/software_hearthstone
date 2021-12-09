(ns firestone.core-api
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [add-card-to-deck
                                         add-card-to-hand
                                         add-minion-to-board
                                         create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         decrease-mana
                                         decrease-mana-with-card
                                         enough-mana?
                                         get-armor
                                         get-attack
                                         get-card-from-hand
                                         get-character
                                         get-hand
                                         get-health
                                         get-hero-id-from-player-id
                                         get-mana
                                         get-minion
                                         get-minions
                                         get-opposing-player-id
                                         get-player-id-in-turn
                                         get-players
                                         get-power
                                         listener-effect
                                         remove-card-from-hand
                                         update-minion]]
            [firestone.definitions :refer [get-definition]]
            [firestone.core :refer [cast-spell
                                    deal-damages
                                    draw-card
                                    start-turn-reset
                                    summon-minion
                                    use-battlecry
                                    valid-attack?
                                    valid-power?]]))

(defn end-turn
  {:test (fn []
           (is= (-> (create-game)
                    (end-turn "p1")
                    (get-player-id-in-turn))
                "p2")
           (is= (-> (create-game)
                    (end-turn "p1")
                    (end-turn "p2")
                    (get-player-id-in-turn))
                "p1")
           (error? (-> (create-game)
                       (end-turn "p2")))
           (is= (-> (create-game)
                    (add-card-to-deck "p2" "Nightblade")
                    (end-turn "p1")
                    (get-card-from-hand "p2" "c1")
                    (:name))
                "Nightblade")
           (is= (-> (create-game [{:mana 6}])
                    (end-turn "p1")
                    (end-turn "p2")
                    (get-mana "p1"))
                10)
           (is= (-> (create-game [{}
                                  {:board-entities [(create-card "Nightblade" :id "n1" :attacks-performed-this-turn 1)]}])
                    (end-turn "p1")
                    (get-minion "n1")
                    (:attacks-performed-this-turn))
                0))}
  [state player-id]
  (when-not (= (get-player-id-in-turn state) player-id)
    (error "The player with id " player-id " is not in turn."))
  (let [player-change-fn {"p1" "p2"
                          "p2" "p1"}]
    (-> state
        (listener-effect :states-end-turn)
        (update :player-id-in-turn player-change-fn)
        (listener-effect :states-start-turn)
        (draw-card (get-opposing-player-id state))
        (start-turn-reset))))

(defn play-minion-card
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
           ; The card should be removed from hand
           (is (-> (create-game [{:hand [(create-card "Defender" :id "d")]}])
                   (play-minion-card "p1" "d" 0)
                   (get-hand "p1")
                   (empty?)))
           ; The minion should appear on the game
           (is-not (-> (create-game [{:hand [(create-card "Defender" :id "d")]}])
                       (play-minion-card "p1" "d" 0)
                       (get-minions "p1")
                       (empty?)))
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
                    (:name)
                    )
                "Nightblade"))}
  ([state player-id card-id position]
   (when-not (= (get-player-id-in-turn state) player-id)
     (error "The player with id " player-id " is not in turn."))
   (let [card (get-card-from-hand state player-id card-id)]
     (when-not card
       (error "The card with id " card-id " is not in the hand of the given player."))
     (when-not (enough-mana? state player-id card)
       (error "Not enough mana."))
     (-> state
         (decrease-mana-with-card player-id card)
         (remove-card-from-hand player-id card-id)
         (summon-minion player-id card position)
         (use-battlecry card))))
  ([state player-id card-id position target-id]
   (when-not (= (get-player-id-in-turn state) player-id)
     (error "The player with id " player-id " is not in turn."))
   (let [card (get-card-from-hand state player-id card-id)]
     (when-not card
       (error "The card with id " card-id " is not in the hand of the given player."))
     (when-not (enough-mana? state player-id card)
       (error "Not enough mana."))
     (-> state
         (decrease-mana-with-card player-id card)
         (remove-card-from-hand player-id card-id)
         (use-battlecry card target-id)
         (summon-minion player-id card position)
         ))))

(defn play-spell-card
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
           ; The card should be removed from hand
           (is (-> (create-game [{:hand [(create-card "Battle Rage" :id "d")]}])
                   (play-spell-card "p1" "d")
                   (get-hand "p1")
                   (empty?)))
           ; The mana of the player should decrease by the mana cost of the card
           (is= (-> (create-game [{:hand [(create-card "Battle Rage" :id "d")]
                                   :mana 9}])
                    (play-spell-card "p1" "d")
                    (get-mana "p1"))
                7)
           ;The effect of the spell (if there is one) is applied
           (is= (-> (create-game [{:board-entities [(create-card "Nightblade" :id "n" :damage-taken 1)]
                                   :hand           [(create-card "Battle Rage" :id "br")]
                                   :deck           [(create-card "Nightblade" :id "n2")]}])
                    (play-spell-card "p1" "br")
                    (get-hand "p1")
                    (count))
                1)
           )}
  ([state player-id card-id]
   (when-not (= (get-player-id-in-turn state) player-id)
     (error "The player with id " player-id " is not in turn."))
   (let [card (get-card-from-hand state player-id card-id)]
     (when-not card
       (error "The card with id " card-id " is not in the hand of the given player."))
     (when-not (enough-mana? state player-id card)
       (error "Not enough mana."))
     (-> state
         (remove-card-from-hand player-id card-id)
         (decrease-mana-with-card player-id card)
         (cast-spell card))))
  ([state player-id card-id target-id]
   (when-not (= (get-player-id-in-turn state) player-id)
     (error "The player with id " player-id " is not in turn."))
   (let [card (get-card-from-hand state player-id card-id)]
     (when-not card
       (error "The card with id " card-id " is not in the hand of the given player."))
     (when-not (enough-mana? state player-id card)
       (error "Not enough mana."))
     (-> state
         (remove-card-from-hand player-id card-id)
         (decrease-mana-with-card player-id card)
         (cast-spell card target-id)))))

(defn attack-minion
  {:test (fn []
           ;The target should loose health points
           (is= (-> (create-game)
                    (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                    (add-minion-to-board "p2" (create-minion "Nightblade" :id "nb") 0)
                    (attack-minion "p1" "ne" "nb")
                    (get-health "nb"))
                3)
           ;The attacker should loose health points
           (is= (-> (create-game)
                    (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne" :health 6) 0)
                    (add-minion-to-board "p2" (create-minion "Nightblade" :id "nb") 0)
                    (attack-minion "p1" "ne" "nb")
                    (get-health "ne"))
                2)
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
           ;When we attack the hero, it should loose health
           (is= (-> (create-game)
                    (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                    (attack-minion "p1" "ne" "h2")
                    (get-health "h2"))
                29)
           ; Should not be possible to attack twice
           (error? (-> (create-game)
                       (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                       (attack-minion "p1" "ne" "h2")
                       (attack-minion "p1" "ne" "h2")
                       )))}
  [state player-id minion-attack-id minion-defense-id]
  (when-not (valid-attack? state player-id minion-attack-id minion-defense-id)
    (error "This attack is not possible"))
  (let [minion-attack (get-minion state minion-attack-id)
        minion-defense (get-minion state minion-defense-id)
        value-attack-attack (or (get-attack state minion-attack-id) 0)
        value-attack-defense (or (get-attack state minion-defense-id) 0)]
      (-> state
          (deal-damages minion-defense-id value-attack-attack {:minion-attacker minion-attack})
          (update-minion minion-attack-id :attacks-performed-this-turn 1)
          (deal-damages minion-attack-id value-attack-defense {:minion-attacker minion-defense}))))

(defn attack-hero
  {:test
   (fn []
     ;When we attack the hero, it should loose health
     (is= (-> (create-game)
              (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
              (attack-hero "p1" "ne")
              (get-health "h2"))
          29)
     ; Should not be possible to attack twice
     (error? (-> (create-game)
                 (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                 (attack-hero "p1" "ne")
                 (attack-hero "p1" "ne")
                 ))
     )}
  [state player-id minion-attack-id]
  (let [attacked-player-id (if (= player-id "p1") "p2" "p1")
        value-attack-attack (get-attack state minion-attack-id)]
    (when-not (valid-attack? state player-id minion-attack-id (get-hero-id-from-player-id state attacked-player-id))
      (error "This attack is not possible"))
    (-> state
        (deal-damages attacked-player-id value-attack-attack {:minion-attacker-id minion-attack-id})
        (update-minion minion-attack-id :attacks-performed-this-turn 1))))


(defn use-hero-power
  "Allow the player to use the hero power
  TODO : - valid-power? to check the mana and if it has already be used this turn
  - handle the target or not target powers
  - use functions instead of all in this one"
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
           ;The effect of the power is applied
           (is= (-> (create-game [{:hero "Garrosh Hellscream"}])
                    (use-hero-power "p1")
                    (get-armor "h1"))
                2)
           ;The effect of the power is applied
           (is= (-> (create-game)
                    (use-hero-power "p1" "h2")
                    (get-health "h2"))
                29)
           )}
  ([state player-id target-id]
   (when-not (= (get-player-id-in-turn state) player-id)
     (error "The player with id " player-id " is not in turn."))
   (let [power (get-power state player-id)
         mana-cost (:mana-cost power)
         effect (:states (get-definition (:name power)))]
     (when-not (valid-power? state player-id)
       (error "Not valid power."))
     (-> state
         (update-in [:players player-id :hero :power :used-this-turn] inc)
         (decrease-mana player-id mana-cost)
         (effect {:target-id target-id}))))
  ([state player-id]
   (use-hero-power state player-id nil)))