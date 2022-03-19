# Chess Engine

A simple chess engine, based on an alpha–beta search, implemented in Java.

The engine evaluates a given position by combining the (weighted) piece values with constants representing favorable regions for each piece type (e.g. a knight is likely more valuable in the center than at the edge), as well as some special cases. The minimax search for the best move is optimized by alpha-beta pruning, iterative deepening and lookup tables (storing previously evaluated positions as Zobrist hashes).

It can be played directly on the command line. The user inputs a move using long algebraic notation (e.g. "e2e4"). The current game state is then printed in unicode.

Example:
```text
Computer chose move: c1c4
  ╔═══╤═══╤═══╤═══╤═══╤═══╤═══╤═══╗
8 ║   │   │   │   │   │   │ ♚ │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
7 ║   │   │   │   │   │   │ ♟ │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
6 ║   │   │   │   │   │   │   │ ♟ ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
5 ║ ♙ │   │   │   │   │   │   │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
4 ║   │   │ ♖ │   │   │   │   │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
3 ║   │   │   │   │   │   │   │ ♙ ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
2 ║ ♜ │   │   │   │   │   │   │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
1 ║   │   │   │   │   │   │ ♔ │   ║
  ╚═══╧═══╧═══╧═══╧═══╧═══╧═══╧═══╝
    a   b   c   d   e   f   g   h  

Enter next move for white: 
...
```

To play, run the main method of the GameManager class. The program will read the player type for both players before starting the game.
The maximum search depth as well as a delay can be set as constants in the Search class.


Resources:
https://www.chessprogramming.org/Main_Page
https://github.com/MartinMSPedersen/Crafty-Chess
