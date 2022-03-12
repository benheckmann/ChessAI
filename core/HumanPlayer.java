package core;

import java.util.Scanner;

public class HumanPlayer extends Player {

    Board board;

    public HumanPlayer(GameManager gm, Board board) {
        this.gm = gm;
        this.board = board;
    }

    @Override
    public void NotifyTurnToMove() {
        System.out.println("Enter next move: ");
        Update();
    }

    @Override
    public void Update() {
        HandleInput();
    }

    void HandleInput() {
        Scanner scanner = new Scanner(System.in);
        Move move;
        while (true) {
            String input = scanner.nextLine();
            try {
                move = new Move(board, input);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid move: " + input);
                System.out.println("Please enter a valid move (e.g. e2e4, e7e5, e1g1 (white short castling), e7e8q (for promotion)): ");
                input = scanner.nextLine();
            }
        }
        ChoseMove(move);
        scanner.close();
    }
}

