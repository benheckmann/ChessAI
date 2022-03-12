package ui;

// import java.util.*;

// import core.*;

// public class Runner implements Runnable {
    // public String boardToString(Board board) {
    //     return null;
    // }

    // public String help() {
    //     return "Enter a move in long algebraic chess notation (e.g. e2e4, e7e5, e1g1 (white short castling), e7e8q (for promotion)) or enter 'q' to quit.";
    // }

    // @Override
    // public void run() {
    //     Scanner scanner = new Scanner(System.in);

    //     Board board = new Board();
    //     board.LoadStartPosition();

    //     while(true) {
    //         System.out.println(boardToString(board));
    //         System.out.println("Enter move: ");
    //         String input = scanner.nextLine();
    //         if (input.equals("q")) {
    //             break;
    //         } else if (input.equals("?") || input.equals("help")) {
    //             System.out.println(help());
    //         } else {
    //             Move move = new Move(input);
    //             board.MakeMove(move);
    //         }
    //     }

    //     scanner.close();
    // }

    // public static void main(String[] args) {
    //     Client client = new Client();
    //     client.run();
    // }
// }