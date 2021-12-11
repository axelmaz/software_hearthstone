# Firestone

A project for DD2487 Large scale development.

## Important note

This code base is strictly for educational use. It is not allowed to publish any content of the course outside of KTH
github.

## REPL dependency

### Starting the REPL and the code is broken

Not that the REPL is a live environment, where you can define symbols to have a meaning. If you load a function into the
REPL and then rename it and load it again into the REPL, both function will be loaded into the REPL. This means that
test might work, but if you restart the REPL your code will be broken.

Another common mistake is that the order in a namespace does matter. If you rearrange functions you need to restart the
REPL to make sure you didn't break anything.

It is a good habit to restart the REPL and then run all tests before you commit your code.

### Cyclic dependency

The entity (card/hero/...) definitions need to access the engine of the game (firestone.construct/firestone/core) to be
able to do battlecries and other mechanisms. The engine also needs to access the definitions in order to get attack,
health, battlecries, etc. This cyclic dependency are implemented as follows:

1. Both the engine and the entity definitions have a dependency to a namespace called **firestone.definitions**.
2. The namespace **firestone.definitions** has an atom in which the different entity definitions upload themselves into.
3. After the entity definitions are loaded into the atom the engine can get them via the function
   get-definitions/get-definition.

If you are a user of the game you can simply load all definitions with the namespace **firestone.definitions_loader**
and then everything is fine.

However, when starting a new REPL, you must load the definitions before being able to run the engine tests.


### Our functions
1. In construct
   - create-hero [name & kvs]
   - create-card [name & kvs]
   - create-minion [name & kvs]
   - create-empty-state [ ] [heroes]
   - get-player [state player-id]
   - get-player-id-in-turn [state]
   - get-opposing-player-id [state]
   - get-minions [state] [state player-id]
   - get-deck [state player-id]
   - get-hand [state player-id]
   - generate-id [state]
   - generate-time-id [state]
   - add-minion-to-board [state player-id minion position]
   - add-minions-to-board [state player-id minions]
   - add-card-to [state player-id card-or-name place]
   - add-card-to-deck [state player-id card]
   - add-card-to-hand [state player-id card]
   - add-cards-to-deck [state player-id cards]
   - add-cards-to-hand [state player-id cards]
   - create-game [data & kvs] []
   - get-minion [state id]
   - get-players [state]
   - get-heroes [state]
   - replace-minion [state new-minion]
   - update-minion [state id key function-or-value]
   - remove-minion [state id]
   - remove-minions [state & ids]
   - remove-card-from-hand [state player-id card-id]
   - remove-card-from-deck [state player-id card-id]
   - get-card-from-hand [state player-id card-id]
   - get-mana [state player-id]
   - enough-mana? [state player-id card] ;TODO : add possibility of use for power
   - decrease-mana [state player-id decrease-number]
   - decrease-mana-with-card [state player-id card] ;TODO : put it in decrease-mana with a map in argument
   - draw-card [state player-id]
   - set-divine-shield [state minion-id] ;TODO : to modify
   - remove-divine-shield [state minion-id]
   - is-divine-shield? [state minion-id]
   - give-minion-plus-one [state player-id minion-name minion-id pos] TODO : useless minion-name + maybe create a + attack, +health functions, and 1 as argument
   - draw-for-each-damaged [state player-id] ;TODO : create draw-cards, with how many
   - draw-specific-card [state player-id card amount] ;TODO : that's add to hand and not draw
   - friendly-when-not-on-board? ;Used when the minion being placed on the board needs to target a friendly minion that is on the board. In this case, we cannot          check in the state for the newly added card since it is yet to be added on the board there. 








 


 

