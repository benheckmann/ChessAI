package core.ai;

import core.*;
import core.data.*;
import core.util.*;

public class Evaluation {
    public static final int PAWN_VAL = 100;
    public static final int KNIGHT_VAL = 300;
    public static final int BISHOP_VAL = 320;
    public static final int ROOK_VAL = 500;
    public static final int QUEEN_VAL = 900;

    static final float ENDGAME_MATERIAL_START = ROOK_VAL * 2 + BISHOP_VAL + KNIGHT_VAL;
    Board board;

    // Performs static evaluation of the current position.
    // The position is assumed to be 'quiet', i.e no captures are available that could drastically affect the evaluation.
    // The score that's returned is given from the perspective of whoever's turn it is to move.
    // So a positive score means the player who's turn it is to move has an advantage, while a negative score indicates a disadvantage.
    public int Evaluate (Board board) {
        this.board = board;
        int whiteEval = 0;
        int blackEval = 0;

        int whiteMaterial = CountMaterial (Board.WHITE_INDEX);
        int blackMaterial = CountMaterial (Board.BLACK_INDEX);

        int whiteMaterialWithoutPawns = whiteMaterial - board.pawns[Board.WHITE_INDEX].size() * PAWN_VAL;
        int blackMaterialWithoutPawns = blackMaterial - board.pawns[Board.BLACK_INDEX].size() * PAWN_VAL;
        float whiteEndgamePhaseWeight = EndgamePhaseWeight (whiteMaterialWithoutPawns);
        float blackEndgamePhaseWeight = EndgamePhaseWeight (blackMaterialWithoutPawns);

        whiteEval += whiteMaterial;
        blackEval += blackMaterial;
        whiteEval += MopUpEval (Board.WHITE_INDEX, Board.BLACK_INDEX, whiteMaterial, blackMaterial, blackEndgamePhaseWeight);
        blackEval += MopUpEval (Board.BLACK_INDEX, Board.WHITE_INDEX, blackMaterial, whiteMaterial, whiteEndgamePhaseWeight);

        whiteEval += EvaluatePieceSquareTables (Board.WHITE_INDEX, blackEndgamePhaseWeight);
        blackEval += EvaluatePieceSquareTables (Board.BLACK_INDEX, whiteEndgamePhaseWeight);

        int eval = whiteEval - blackEval;

        int perspective = (board.whiteToMove) ? 1 : -1;
        return eval * perspective;
    }

    float EndgamePhaseWeight (int materialCountWithoutPawns) {
        float multiplier = 1 / ENDGAME_MATERIAL_START;
        return 1 - Math.min (1, materialCountWithoutPawns * multiplier);
    }

    int MopUpEval (int friendlyIndex, int opponentIndex, int myMaterial, int opponentMaterial, float endgameWeight) {
        int mopUpScore = 0;
        if (myMaterial > opponentMaterial + PAWN_VAL * 2 && endgameWeight > 0) {

            int friendlyKingSquare = board.KingSquare[friendlyIndex];
            int opponentKingSquare = board.KingSquare[opponentIndex];
            mopUpScore += PrecomputedMoveData.centreManhattanDistance[opponentKingSquare] * 10;
            // use ortho dst to promote direct opposition
            mopUpScore += (14 - PrecomputedMoveData.NumRookMovesToReachSquare (friendlyKingSquare, opponentKingSquare)) * 4;

            return (int) (mopUpScore * endgameWeight);
        }
        return 0;
    }

    int CountMaterial (int colourIndex) {
        int material = 0;
        material += board.pawns[colourIndex].size() * PAWN_VAL;
        material += board.knights[colourIndex].size() * KNIGHT_VAL;
        material += board.bishops[colourIndex].size() * BISHOP_VAL;
        material += board.rooks[colourIndex].size() * ROOK_VAL;
        material += board.queens[colourIndex].size() * QUEEN_VAL;

        return material;
    }

    int EvaluatePieceSquareTables (int colourIndex, float endgamePhaseWeight) {
        int value = 0;
        boolean isWhite = colourIndex == Board.WHITE_INDEX;
        value += EvaluatePieceSquareTable (PieceSquareTable.pawns, board.pawns[colourIndex], isWhite);
        value += EvaluatePieceSquareTable (PieceSquareTable.rooks, board.rooks[colourIndex], isWhite);
        value += EvaluatePieceSquareTable (PieceSquareTable.knights, board.knights[colourIndex], isWhite);
        value += EvaluatePieceSquareTable (PieceSquareTable.bishops, board.bishops[colourIndex], isWhite);
        value += EvaluatePieceSquareTable (PieceSquareTable.queens, board.queens[colourIndex], isWhite);
        int kingEarlyPhase = PieceSquareTable.Read (PieceSquareTable.kingMiddle, board.KingSquare[colourIndex], isWhite);
        value += (int) (kingEarlyPhase * (1 - endgamePhaseWeight));
        return value;
    }

    static int EvaluatePieceSquareTable (int[] table, PieceList pieceList, boolean isWhite) {
        int value = 0;
        for (int i = 0; i < pieceList.size(); i++) {
            value += PieceSquareTable.Read (table, pieceList.get(i), isWhite);
        }
        return value;
    }
}
