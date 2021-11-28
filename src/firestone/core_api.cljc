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
                                         is-divine-shield?
                                         remove-card-from-hand
                                         update-minion]]
            [firestone.definitions :refer [get-definition]]
            [firestone.core :refer [deal-damages
                                    get-armor
                                    get-attack
                                    get-health
                                    get-hero-id-from-player-id
                                    valid-attack?]]))

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
           (is (-> (create-game [{:minions [(create-minion "Defender" :id "d")
                                            (create-minion "Defender" :id "d2")]}])
                   (use-battlecry (create-card "Argent Protector" :owner-id "p1") "d")
                   (is-divine-shield? "d")))
           ; The battlecry of "Argent Protector" does not give divine shield to not targeted minion
           (is-not (-> (create-game [{:minions [(create-minion "Defender" :id "d")
                                                (create-minion "Defender" :id "d2")]}])
                       (use-battlecry (create-card "Argent Protector" :owner-id "p1") "d")
                       (is-divine-shield? "d2")))
           ; The battlecry of "Argent Protector" should give an error if we try to target an invalid minion
           (error? (-> (create-game [{:minions [(create-minion "Defender" :id "d")]}])
                       (add-minion-to-board "p2" (create-minion "Defender" :id "d2") 0)
                       (use-battlecry (create-card "Argent Protector" :owner-id "p1") "d2")
                       (is-divine-shield? "d2")))
           ; Divine shield minions are seen as battlecry, and get a shield at this moment
           (is (-> (create-game [{:minions [(create-minion "Argent Squire" :id "a")]}])
                   (use-battlecry (create-card "Argent Squire" :owner-id "p1" :id "a"))
                   (is-divine-shield? "a")))
           ; If the card doesn't have battlecry (as Defender for exemple), the state should not change
           (is= (-> (create-game)
                    (use-battlecry "Defender"))
                (create-game))
           ; Test "Blessed champion battlecry : should double the attack
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 1 :attack 4)]
                                   :hand    [(create-card "Blessed Champion" :id "ne")]}])
                    (use-battlecry (create-card "Blessed Champion" :id "ne") "n")
                    (get-attack "n"))
                8)
           ; Test "Earthen Ring Farseer"
           (is= (-> (create-game)
                    (use-battlecry (create-card "Earthen Ring Farseer" :owner-id "p1"))
                    (get-armor "h1"))
                3)
           ; Test "Whirlwind"
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (use-battlecry (create-minion "Whirlwind"))
                    (get-health "n"))
                3)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (add-minion-to-board "p2" (create-minion "Defender" :id "n2" :health 2) 0)
                    (use-battlecry (create-minion "Whirlwind"))
                    (get-health "n2"))
                1)
           ; Test "Shield Slam"
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1" :armor 3)}])
                    (add-minion-to-board "p2" (create-minion "Defender" :id "d" :health 4) 0)
                    (use-battlecry (create-minion "Shield Slam" :owner-id "p1") "d")
                    (get-health "d"))
                1)
           )}
  ([state card]
   (let [battlecry-function ((get-definition card) :battlecry)]
     (if battlecry-function (battlecry-function state card) state)))
  ([state card target-id]
   (let [battlecry-function ((get-definition card) :battlecry)]
     (if battlecry-function (battlecry-function state card target-id) state))
   ))

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
           ;The effect (battlecry) of the card (if there is one) is applied
           (is= (-> (create-game [{:minions [(create-card "Nightblade" :id "n" :damage-taken 1)]
                                   :hand    [(create-card "Battle Rage" :id "ne")]
                                   :deck    [(create-card "Nightblade" :id "n2")]}])
                    (play-spell-card "p1" "ne")
                    (get-hand "p1")
                    (count))
                1)
           )}
  [state player-id card-id]
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
        (use-battlecry card))))

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
          (deal-damages minion-defense-id value-attack-attack)
          (deal-damages minion-attack-id value-attack-defense)
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
    (deal-damages state attacked-player-id value-attack-attack)))


