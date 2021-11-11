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







 


 

