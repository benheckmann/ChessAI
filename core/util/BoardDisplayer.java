package core.util;

import core.*;

/**
 * Utility class for converting a board to it's unicode string representation.
 */
public class BoardDisplayer {

    private Board board;
    private Perspective perspective;

    private enum Perspective {
        White, Black
    };

    private static final String fileLetteringWhite = "    a   b   c   d   e   f   g   h  ";
    private static final String fileLetteringBlack = "    h   g   f   e   d   c   b   a  ";
    private static final String top = "  ╔═══╤═══╤═══╤═══╤═══╤═══╤═══╤═══╗";
    private static final String middle = "  ╟───┼───┼───┼───┼───┼───┼───┼───╢";
    private static final String bottom = "  ╚═══╧═══╧═══╧═══╧═══╧═══╧═══╧═══╝";

    public BoardDisplayer(Board board, boolean isWhitePerspective) {
        this.board = board;
        this.perspective = isWhitePerspective ? Perspective.White : Perspective.Black;
    }

    public String displayCurrentState() {
        return boardToString(board, perspective);
    }

    public static String boardToString(Board board) {
        return boardToString(board, Perspective.White);
    }

    public static String boardToString(Board board, Perspective color) {
        int[] squares = board.Square; // { a1, b1, ..., a2, b2, ..., h8 }
        StringBuilder sb = new StringBuilder();
        sb.append(top + "\n");
        for (int i = 0; i < 8; i++) {
            int sideIndex = color.equals(Perspective.White) ? 8 - i : i + 1;
            sb.append(sideIndex + " ║ ");
            for (int j = 0; j < 8; j++) {
                int pieceIndex = color.equals(Perspective.White) ? (7 - i) * 8 + j : i * 8 + (7 - j);
                int piece = squares[pieceIndex];
                sb.append(Piece.pieceToAscii(piece));
                // ...or sb.append(Piece.pieceToUnicode(piece));
                if (j < 7) {
                    sb.append(" │ ");
                } else {
                    sb.append(" ║\n");
                }
            }
            if (i < 7) {
                sb.append(middle + "\n");
            }
        }
        sb.append(bottom + "\n");
        sb.append(color.equals(Perspective.White) ? fileLetteringWhite : fileLetteringBlack + "\n");
        return sb.toString();
    }

    // public static void main(String[] args) {
    //     Board b = new Board();
    //     b.LoadStartPosition();
    //     System.out.println(boardToString(b));
    // }
}
