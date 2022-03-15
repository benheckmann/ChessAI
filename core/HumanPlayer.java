package core;

import java.util.Scanner;

public class HumanPlayer extends Player {

    public HumanPlayer(GameManager gm, Board board, boolean isWhite) {
        super(gm, board, isWhite);
    }

    @Override
    public void notifyTurnToMove() {
        System.out.println("Enter next move for " + (isWhite ? "white" : "black") + ": ");
        update();
    }

    @Override
    public void update() {
        choseMoveFromInput();
    }

    void choseMoveFromInput() {
        Scanner scanner = new Scanner(System.in);
        Move move;
        while (true) {
            String input = scanner.nextLine();
            if (input.equals("?")) {
                printHelp();
                continue;
            }
            try {
                move = new Move(board, input);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                System.out.println("Please enter a valid move or '?' for help: ");
                continue;
            }
        }
        choseMove(move);
        scanner.close();
    }

    void printHelp() {
        System.out.println("A move is represented in long algebraic notation (e.g. e2e4).");
        System.out.println("The first coordinate is the source square, the second coordinate is the destination square.");
        System.out.println("Special moves include: e1g1 (white short castling), e7e8q (for promotion).");
    }
}

