# Chess AI

A simple chess program based on alpha–beta pruning, implemented in Java.

It can be played directly on the command line. The user inputs a move using long algebraic notation (e.g. "e2e4"). The current game state is then printed in unicode.

Example:
```
AI chose move: b8c6
  ╔═══╤═══╤═══╤═══╤═══╤═══╤═══╤═══╗
8 ║ ♜ │   │ ♝ │ ♛ │ ♚ │ ♝ │ ♞ │ ♜ ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
7 ║ ♟ │ ♟ │ ♟ │ ♟ │ ♟ │ ♟ │ ♟ │ ♟ ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
6 ║   │   │ ♞ │   │   │   │   │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
5 ║   │   │   │   │   │   │   │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
4 ║   │   │   │   │ ♙ │   │   │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
3 ║   │   │   │   │   │   │   │   ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
2 ║ ♙ │ ♙ │ ♙ │ ♙ │   │ ♙ │ ♙ │ ♙ ║
  ╟───┼───┼───┼───┼───┼───┼───┼───╢
1 ║ ♖ │ ♘ │ ♗ │ ♕ │ ♔ │ ♗ │ ♘ │ ♖ ║
  ╚═══╧═══╧═══╧═══╧═══╧═══╧═══╧═══╝
    a   b   c   d   e   f   g   h  

Enter next move for white: 
...
```

To play, run the main method of the GameManager class. The program will read the player type for both players before starting the game.
The search depth for the minimax algorithm as well as a delay can be set as constants in the Search class.
