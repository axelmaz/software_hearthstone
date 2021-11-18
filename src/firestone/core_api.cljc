(ns firestone.core-api
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [add-card-to-deck
                                         add-card-to-hand
                                         add-minion-to-board
                                         create-card
                                         create-game
                                         create-minion
                                         decrease-mana-with-card
                                         draw-card
                                         enough-mana?
                                         get-card-from-hand
                                         get-deck
                                         get-hand
                                         get-mana
                                         get-minion
                                         get-minions
                                         get-opposing-player-id
                                         get-player-id-in-turn
                                         get-players
                                         remove-card-from-hand
                                         update-minion]]
            [firestone.definitions :refer [get-definition]]
            [firestone.core :refer [get-attack
                                    get-health
                                    valid-attack?
                                    get-hero-id-from-player-id]]))

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
                10))}
  [state player-id]
  (when-not (= (get-player-id-in-turn state) player-id)
    (error "The player with id " player-id " is not in turn."))
  (let [player-change-fn {"p1" "p2"
                          "p2" "p1"}]
    (-> state
        (update :player-id-in-turn player-change-fn)
        (draw-card (get-opposing-player-id state))
        (assoc-in [:players (get-opposing-player-id state) :mana] 10))))


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
                "Nightblade")
           )}
  [state player-id card-id position]
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
        (add-minion-to-board player-id card position)
        (use-battlecry card)
        )))

(defn use-battlecry
  {:test (fn []
           ; The battlecry of Novice Engineer is to draw a card so we test if we obtain a card in our hand
           (is= (-> (create-game [{:deck [(create-card "Nightblade" :id "n")]}])
                    (use-battlecry "Novice Engineer")
                    (get-hand "p1")
                    (first)
                    (:name)
                    )
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
           ; If the card doesn't have battlecry (as Defender for exemple), the state should not change
           (is= (-> (create-game)
                    (use-battlecry "Defender"))
                (create-game)))}
  [state card-name]
  (let [battlecry-function ((get-definition card-name) :battlecry)]
    (if battlecry-function (battlecry-function state) state) ))

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
                    (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                    (add-minion-to-board "p2" (create-minion "Nightblade" :id "nb") 0)
                    (attack-minion "p1" "ne" "nb")
                    (get-health "ne"))
                -3)
           ;The attack has to be valid
           (error? (-> (create-game)
                    (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                    (add-minion-to-board "p2" (create-minion "Nightblade" :id "nb") 0)
                    (attack-minion "p2" "ne" "nb")))
           ;The attacker could not attack twice a tour
           (error? (-> (create-game)
                       (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
                       (add-minion-to-board "p2" (create-minion "Nightblade" :id "nb") 0)
                       (attack-minion "p1" "ne" "nb")
                       (attack-minion "p1" "ne" "nb"))))}
  [state player-id minion-attack-id minion-defense-id]
  (when-not (valid-attack? state player-id minion-attack-id minion-defense-id)
    (error "This attack is not possible"))
  (let [value-attack-attack (get-attack state minion-attack-id)]
    (let [value-attack-defense (get-attack state minion-defense-id)]
      (-> state
          (update-minion minion-defense-id :damage-taken value-attack-attack)
          (update-minion minion-attack-id :damage-taken value-attack-defense)
          (update-minion minion-attack-id :attacks-performed-this-turn 1)))))

(defn attack-hero
  {:test
   (fn []
     (is= (-> (create-game)
              (add-card-to-deck "p1" "Nightblade")
              (add-card-to-deck "p2" "Nightblade")
              (add-minion-to-board "p1" (create-minion "Novice Engineer" :id "ne") 0)
              (end-turn "p1")
              (add-minion-to-board "p2" (create-minion "Nightblade" :id "nb") 0)
              (end-turn "p2")
              (attack-hero "p1" "ne")
              (get-health "h2"))
          29))}
  [state player-id minion-attack-id]
  (let [attacked-player-id (if (= player-id "p1") "p2" "p1") value-attack-attack (get-attack state minion-attack-id)]
    (when-not (valid-attack? state player-id minion-attack-id (get-hero-id-from-player-id state attacked-player-id))
      (error "This attack is not possible"))
    (update-in state [:players attacked-player-id :hero :damage-taken] + value-attack-attack)))


