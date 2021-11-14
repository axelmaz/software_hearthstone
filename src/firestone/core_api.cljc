(ns firestone.core-api
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [add-card-to-deck
                                         add-card-to-hand
                                         add-minion-to-board
                                         create-card
                                         create-game
                                         decrease-mana-with-card
                                         draw-card
                                         enough-mana?
                                         get-card-from-hand
                                         get-deck
                                         get-hand
                                         get-mana
                                         get-minions
                                         get-opposing-player-id
                                         get-player-id-in-turn
                                         get-players
                                         remove-card-from-hand]]))

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
                    (get-mana "p1")
                    )
                8
                )
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
        (add-minion-to-board player-id card position))))

