package core.ai;

import java.util.List;

import core.*;

public class Search {

	// settings
	public static final int MAX_SEARCH_DEPTH = 5;
	public static final int DELAY_PER_MOVE = 500; // in ms
	public static final boolean TT_ENABLED = true;

	static final int TT_SIZE = 64000;
	static final int IMMEDIATE_MATE_SCORE = 100000;
	static final int POSITIVE_INFINITY = 9999999;
	static final int NEGATIVE_INFINITY = -POSITIVE_INFINITY;

	TranspositionTable tt;
	MoveGenerator moveGenerator;

	Move bestMoveThisIteration;
	int bestEvalThisIteration;
	Move bestMove;
	int bestEval;
	int currentIterativeSearchDepth;
	boolean abortSearch;

	Move invalidMove;
	MoveOrdering moveOrdering;
	Board board;
	Evaluation evaluation;
	AIPlayer player;

	// Diagnostics
	public SearchDiagnostics searchDiagnostics;
	int numNodes;
	int numQNodes;
	int numCutoffs;
	int numTranspositions;
	// System.Diagnostics.Stopwatch searchStopwatch;

	public Search(Board board, AIPlayer player) {
		this.board = board;
		evaluation = new Evaluation();
		moveGenerator = new MoveGenerator();
		tt = new TranspositionTable(board, TT_SIZE);
		moveOrdering = new MoveOrdering(moveGenerator, tt);
		invalidMove = Move.getInvalidMove();
		this.player = player;
	}

	public void StartSearch() {
		// Initialize search settings
		bestEvalThisIteration = bestEval = 0;
		bestMoveThisIteration = bestMove = Move.getInvalidMove();
		tt.enabled = TT_ENABLED;
		tt.Clear(); // clearing the transposition table before each search seems to help

		currentIterativeSearchDepth = 0;
		abortSearch = false;
		searchDiagnostics = new SearchDiagnostics();

		// iterative deepening
		int targetDepth = MAX_SEARCH_DEPTH;

		for (int searchDepth = 1; searchDepth <= targetDepth; searchDepth++) {
			SearchMoves(searchDepth, 0, NEGATIVE_INFINITY, POSITIVE_INFINITY);
			if (abortSearch) {
				break;
			} else {
				currentIterativeSearchDepth = searchDepth;
				bestMove = bestMoveThisIteration;
				bestEval = bestEvalThisIteration;

				// Update diagnostics
				searchDiagnostics.lastCompletedDepth = searchDepth;
				searchDiagnostics.move = bestMove.toString();
				searchDiagnostics.eval = bestEval;

				// Exit search if found a mate
				if (IsMateScore(bestEval)) {
					break;
				}
			}
		}
		player.OnSearchComplete(bestMove);
	}

	public Move GetSearchResult() {
		return bestMove;
	}

	public void EndSearch() {
		abortSearch = true;
	}

	int SearchMoves(int depth, int plyFromRoot, int alpha, int beta) {
		if (abortSearch) {
			return 0;
		}

		if (plyFromRoot > 0) {
			// Detect draw by repetition.
			// Returns a draw score even if this position has only appeared once in the game
			// history (for simplicity).
			if (board.RepetitionPositionHistory.contains(board.ZobristKey)) {
				return 0;
			}

			// Skip this position if a mating sequence has already been found earlier in
			// the search, which would be shorter than any mate we could find from here.
			// This is done by observing that alpha can't possibly be worse (and likewise
			// beta can't possibly be better) than being mated in the current position.
			alpha = (int) Math.max(alpha, -IMMEDIATE_MATE_SCORE + plyFromRoot);
			beta = (int) Math.min(beta, IMMEDIATE_MATE_SCORE - plyFromRoot);
			if (alpha >= beta) {
				return alpha;
			}
		}

		// Try looking up the current position in the transposition table.
		// If the same position has already been searched to at least an equal depth
		// to the search we're doing now,we can just use the recorded evaluation.
		int ttVal = tt.LookupEvaluation(depth, plyFromRoot, alpha, beta);
		if (ttVal != TranspositionTable.lookupFailed) {
			numTranspositions++;
			if (plyFromRoot == 0) {
				bestMoveThisIteration = tt.GetStoredMove();
				bestEvalThisIteration = tt.entries[(int) tt.Index()].value;
				// Debug.Log ("move retrieved " + bestMoveThisIteration.Name + " Node type: " +
				// tt.entries[tt.Index].nodeType + " depth: " + tt.entries[tt.Index].depth);
			}
			return ttVal;
		}

		if (depth == 0) {
			int evaluation = QuiescenceSearch(alpha, beta);
			return evaluation;
		}

		List<Move> moves = moveGenerator.generateMoves(board);
		moveOrdering.OrderMoves(board, moves, TT_ENABLED);
		// Detect checkmate and stalemate when no legal moves are available
		if (moves.size() == 0) {
			if (moveGenerator.isInCheck()) {
				int mateScore = IMMEDIATE_MATE_SCORE - plyFromRoot;
				return -mateScore;
			} else {
				return 0;
			}
		}

		int evalType = TranspositionTable.UpperBound;
		Move bestMoveInThisPosition = invalidMove;

		for (int i = 0; i < moves.size(); i++) {
			board.MakeMove(moves.get(i), true);
			int eval = -SearchMoves(depth - 1, plyFromRoot + 1, -beta, -alpha);
			board.UnmakeMove(moves.get(i), true);
			numNodes++;

			// Move was *too* good, so opponent won't allow this position to be reached
			// (by choosing a different move earlier on). Skip remaining moves.
			if (eval >= beta) {
				tt.StoreEvaluation(depth, plyFromRoot, beta, TranspositionTable.LowerBound, moves.get(i));
				numCutoffs++;
				return beta;
			}

			// Found a new best move in this position
			if (eval > alpha) {
				evalType = TranspositionTable.Exact;
				bestMoveInThisPosition = moves.get(i);

				alpha = eval;
				if (plyFromRoot == 0) {
					bestMoveThisIteration = moves.get(i);
					bestEvalThisIteration = eval;
				}
			}
		}

		tt.StoreEvaluation(depth, plyFromRoot, alpha, evalType, bestMoveInThisPosition);

		return alpha;

	}

	// Search capture moves until a 'quiet' position is reached.
	int QuiescenceSearch(int alpha, int beta) {
		// A player isn't forced to make a capture (typically), so see what the
		// evaluation is without capturing anything.
		// This prevents situations where a player ony has bad captures available from
		// being evaluated as bad,
		// when the player might have good non-capture moves available.
		int eval = evaluation.Evaluate(board);
		searchDiagnostics.numPositionsEvaluated++;
		if (eval >= beta) {
			return beta;
		}
		if (eval > alpha) {
			alpha = eval;
		}

		var moves = moveGenerator.generateMoves(board, false);
		moveOrdering.OrderMoves(board, moves, false);
		for (int i = 0; i < moves.size(); i++) {
			board.MakeMove(moves.get(i), true);
			eval = -QuiescenceSearch(-beta, -alpha);
			board.UnmakeMove(moves.get(i), true);
			numQNodes++;

			if (eval >= beta) {
				numCutoffs++;
				return beta;
			}
			if (eval > alpha) {
				alpha = eval;
			}
		}

		return alpha;
	}

	public static boolean IsMateScore(int score) {
		int maxMateDepth = 1000;
		return Math.abs(score) > (IMMEDIATE_MATE_SCORE - maxMateDepth);
	}

	public static int NumPlyToMateFromScore(int score) {
		return IMMEDIATE_MATE_SCORE - Math.abs(score);

	}

	public class SearchDiagnostics {
		public int lastCompletedDepth;
		public String moveVal;
		public String move;
		public int eval;
		public boolean isBook;
		public int numPositionsEvaluated;
	}
}