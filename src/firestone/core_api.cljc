(ns firestone.core-api
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [add-card-to-deck
                                         add-card-to-hand
                                         add-minion-to-board
                                         add-secret
                                         create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         create-secret
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
                                    secret-effect
                                    summon-minion
                                    use-battlecry
                                    valid-attack?
                                    valid-power?]]))

(defn end-turn
  {:test (fn []
           ; Change the player in turn
           (is= (-> (create-game)
                    (end-turn "p1")
                    (get-player-id-in-turn))
                "p2")
           ; Make the player draw
           (is= (-> (create-game)
                    (add-card-to-deck "p2" "Nightblade")
                    (end-turn "p1")
                    (get-card-from-hand "p2" "c1")
                    (:name))
                "Nightblade"))}
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
           ; The card should be removed from hand
           (is (-> (create-game [{:hand [(create-card "Defender" :id "d")]}])
                   (play-minion-card "p1" "d" 0)
                   (get-hand "p1")
                   (empty?)))
           ; The minion should appear on the game
           (is-not (-> (create-game [{:hand [(create-card "Defender" :id "d")]}])
                       (play-minion-card "p1" "d" 0)
                       (get-minions "p1")
                       (empty?))))}
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
           ; The card should be removed from hand
           (is (-> (create-game [{:hand [(create-card "Battle Rage" :id "d")]}])
                   (play-spell-card "p1" "d")
                   (get-hand "p1")
                   (empty?)))
           ;The effect of the spell is applied : the effect of battle rage is to draw for each damaged minion
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
           ;When we attack the hero, it should loose health
           (is= (-> (create-game)
                    (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "Ne") 0)
                    (attack-minion "p1" "Ne" "h2")
                    (get-health "h2"))
                29))}
  [state player-id minion-attack-id minion-defense-id]
  (when-not (valid-attack? state player-id minion-attack-id minion-defense-id)
    (error "This attack is not possible"))
  (let [old-minion-attack (get-character state minion-attack-id)
        old-minion-defense (get-character state minion-defense-id)
        state (secret-effect state :secret-attack {:attacked-character old-minion-defense})
        minion-attack (get-character state minion-attack-id)
        minion-defense (get-character state minion-defense-id)
        minion-list (sort-by :added-to-board-time-id [minion-attack minion-defense])
        other-minion-function {(:id minion-attack)  minion-defense
                               (:id minion-defense) minion-attack}
        deal-damage-to-minion (fn [state minion]
                                (let [other-minion (other-minion-function (:id minion))
                                      minion-attack (or (get-attack minion) 0)]
                                  (deal-damages state (:id other-minion) minion-attack {:minion-attacker minion})))]
    (if (nil? (and minion-attack minion-defense))
      state
      (as-> state $
            (reduce deal-damage-to-minion $ minion-list)
            (if (some? (get-minion $ minion-attack-id))
              (update-minion $ minion-attack-id :attacks-performed-this-turn 1)
              $)))))


(defn use-hero-power
  "Allow the player to use the hero power
  TODO : - valid-power? to check the mana and if it has already be used this turn
  - handle the target or not target powers
  - use functions instead of all in this one"
  {:test (fn []
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
         (listener-effect :inspire {:power-owner-id player-id})
         (decrease-mana player-id mana-cost)
         (effect {:target-id target-id}))))
  ([state player-id]
   (use-hero-power state player-id nil)))