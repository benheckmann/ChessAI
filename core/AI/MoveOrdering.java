package core.ai;

import core.*;
import core.util.*;

import java.util.List;

public class MoveOrdering {
    
		public int[] moveScores;
		static final int MAX_MOVE_COUNT = 218;

		static final int SQUARE_CONTROLLED_BY_OPP_PAWN_PENALTY = 350;
		static final int CAPTURED_PIECE_VAL_MULTIPLIER = 10;

		MoveGenerator moveGenerator;
		TranspositionTable transpositionTable;
		Move invalidMove;

		public MoveOrdering (MoveGenerator moveGenerator, TranspositionTable tt) {
			moveScores = new int[MAX_MOVE_COUNT];
			this.moveGenerator = moveGenerator;
			this.transpositionTable = tt;
			invalidMove = Move.getInvalidMove();
		}

		public void OrderMoves (Board board, List<Move> moves, boolean useTT) {
			Move hashMove = invalidMove;
			if (useTT && transpositionTable.GetStoredMove() != null) {
				hashMove = transpositionTable.GetStoredMove();
			}

			for (int i = 0; i < moves.size(); i++) {
				int score = 0;
				int movePieceType = Piece.getPieceType (board.Square[moves.get(i).getStartSquare()]);
				int capturePieceType = Piece.getPieceType (board.Square[moves.get(i).getTargetSquare()]);
				int flag = moves.get(i).getMoveFlag();

				if (capturePieceType != Piece.None) {
					// Order moves to try capturing the most valuable opponent piece with least valuable of own pieces first
					// The capturedPieceValueMultiplier is used to make even 'bad' captures like QxP rank above non-captures
					score = CAPTURED_PIECE_VAL_MULTIPLIER * GetPieceValue (capturePieceType) - GetPieceValue (movePieceType);
				}

				if (movePieceType == Piece.Pawn) {

					if (flag == Move.Flag.PromoteToQueen) {
						score += Evaluation.QUEEN_VAL;
					} else if (flag == Move.Flag.PromoteToKnight) {
						score += Evaluation.KNIGHT_VAL;
					} else if (flag == Move.Flag.PromoteToRook) {
						score += Evaluation.ROOK_VAL;
					} else if (flag == Move.Flag.PromoteToBishop) {
						score += Evaluation.BISHOP_VAL;
					}
				} else {
					// Penalize moving piece to a square attacked by opponent pawn
					if (BitBoardUtility.ContainsSquare (moveGenerator.opponentPawnAttackMap, moves.get(i).getTargetSquare())) {
						score -= SQUARE_CONTROLLED_BY_OPP_PAWN_PENALTY;
					}
				}
				if (moves.get(i).equals(hashMove)) {
					score += 10000;
				}

				moveScores[i] = score;
			}

			Sort (moves);
		}

		static int GetPieceValue (int pieceType) {
			switch (pieceType) {
				case Piece.Queen:
					return Evaluation.QUEEN_VAL;
				case Piece.Rook:
					return Evaluation.ROOK_VAL;
				case Piece.Knight:
					return Evaluation.KNIGHT_VAL;
				case Piece.Bishop:
					return Evaluation.BISHOP_VAL;
				case Piece.Pawn:
					return Evaluation.PAWN_VAL;
				default:
					return 0;
			}
		}

		void Sort (List<Move> moves) {
			// Sort the moves list based on scores
			for (int i = 0; i < moves.size() - 1; i++) {
				for (int j = i + 1; j > 0; j--) {
					int swapIndex = j - 1;
					if (moveScores[swapIndex] < moveScores[j]) {
                        Move temp = moves.get(j);
                        moves.set(j, moves.get(swapIndex));
                        moves.set(swapIndex, temp);
                        int tempScore = moveScores[j];
                        moveScores[j] = moveScores[swapIndex];
                        moveScores[swapIndex] = tempScore;
					}
				}
			}
		}
}
