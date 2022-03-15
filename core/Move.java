package core;

import java.util.List;

import core.util.BoardUtility;

public class Move {

    public static class Flag {
        public static final int None = 0;
        public static final int EnPassantCapture = 1;
        public static final int Castling = 2;
        public static final int PromoteToQueen = 3;
        public static final int PromoteToKnight = 4;
        public static final int PromoteToRook = 5;
        public static final int PromoteToBishop = 6;
        public static final int PawnTwoForward = 7;
    }

    // Bits 0-5 store the index of the start square
    // Bits 6-11 store the index of the target square
    // Bits 12-15 store the flag
    static final short START_SQUARE_MASK = (short) 0b0000000000111111;
    static final short TARGET_SQUARE_MASK = (short) 0b0000111111000000;
    static final short FLAG_MASK = (short) 0b0111000000000000;

    public final short moveValue;
    public Object[] moves;

    public Move(short moveValue) {
        this.moveValue = moveValue;
    }

    public Move(int startSquare, int targetSquare) {
        moveValue = (short) (startSquare | targetSquare << 6);
    }

    public Move(int startSquare, int targetSquare, int flag) {
        moveValue = (short) (startSquare | targetSquare << 6 | flag << 12);
    }

    /**
     * Tries to generate a move from an input string in long algebraic notation and
     * the current board. (e.g. e2e4, e7e5, e1g1 (white short castling), e7e8q (for
     * promotion))
     */
    public Move(Board board, String lan) {
        lan = lan.toLowerCase();
        short lanMoveValue = parseMove(board, lan);
        moveValue = lanMoveValue;
    }

    /**
     * Tries to generate a move from an input string in long algebraic notation and
     * a board. Will throw an exception if the move is not legal.
     */
    public static short parseMove(Board board, String lan) {
        lan = lan.toLowerCase();
        if (!isValidLan(lan)) {
            throw new IllegalArgumentException("The input '" + lan + "' does not represent long algebraic notation.");
        }
        int startSquare = BoardUtility.getIndexFromSquareName(lan.substring(0, 2));
        int targetSquare = BoardUtility.getIndexFromSquareName(lan.substring(2, 4));
        int wantedPromotionFlag = lan.length() == 5 ? "qnrb".indexOf(lan.substring(4)) + 3 : 0;
        boolean moveIsLegal = false;
        Move chosenMove = null;
        MoveGenerator moveGenerator = new MoveGenerator();
        List<Move> legalMoves = moveGenerator.generateMoves(board);
        for (Move legalMove : legalMoves) {
            if (legalMove.getStartSquare() == startSquare && legalMove.getTargetSquare() == targetSquare) {
                if (legalMove.isPromotion()){
                    if(legalMove.getMoveFlag() == Move.Flag.PromoteToQueen && (wantedPromotionFlag == Move.Flag.PromoteToKnight)){
                        break;
                    }
                    if(legalMove.getMoveFlag() != Move.Flag.PromoteToQueen && (wantedPromotionFlag != Move.Flag.PromoteToKnight)){
                        continue;
                    }
                }
                moveIsLegal = true;
                chosenMove = legalMove;
                break;
            }
        }
        
        if (moveIsLegal) {
            return chosenMove.moveValue;
        } else {
            throw new IllegalArgumentException("Illegal move: " + lan);
        }
    }

    public int getStartSquare() {
        return moveValue & START_SQUARE_MASK;
    }

    public int getTargetSquare() {
        return (moveValue & TARGET_SQUARE_MASK) >> 6;
    }

    public boolean isPromotion() {
        int flag = getMoveFlag();
        return flag == Flag.PromoteToQueen || flag == Flag.PromoteToRook || flag == Flag.PromoteToKnight
                || flag == Flag.PromoteToBishop;
    }

    public int getMoveFlag() {
        return moveValue >> 12;
    }

    public int getPromotionPieceType() {
        switch (getMoveFlag()) {
            case Flag.PromoteToRook:
                return Piece.Rook;
            case Flag.PromoteToKnight:
                return Piece.Knight;
            case Flag.PromoteToBishop:
                return Piece.Bishop;
            case Flag.PromoteToQueen:
                return Piece.Queen;
            default:
                return Piece.None;
        }

    }

    public static Move getInvalidMove() {
        return new Move((short) 0);

    }

    public boolean equals(Move other) {
        return this.moveValue == other.moveValue;
    }

    public short Value() {
        return moveValue;

    }

    public boolean IsInvalid() {
        return moveValue == 0;

    }

    @Override
    public String toString() {
        return BoardUtility.getSquareNameFromIndex(getStartSquare()) + ""
                + BoardUtility.getSquareNameFromIndex(getTargetSquare());
    }

    private static boolean isValidLan(String lan) {
        lan = lan.toLowerCase();
        return lan.matches("^[a-h][1-8][a-h][1-8][qrbn]?$");
    }

}