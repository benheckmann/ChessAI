package core;

import core.ai.*;
import core.util.*;

import java.util.List;
import java.util.LinkedList;

public class GameManager implements Runnable {

	public enum Result {
		Playing, WhiteIsMated, BlackIsMated, Stalemate, Repetition, FiftyMoveRule, InsufficientMaterial
	}

	public enum PlayerType {
		Human, AI
	}

	public boolean loadCustomPosition;
	public String customPosition = "1rbq1r1k/2pp2pp/p1n3p1/2b1p3/R3P3/1BP2N2/1P3PPP/1NBQ1RK1 w - - 0 1";

	public PlayerType whitePlayerType;
	public PlayerType blackPlayerType;

	Result gameResult;

	Player whitePlayer;
	Player blackPlayer;
	Player playerToMove;
	List<Move> gameMoves;

	public long zobristDebug;
	public Board board;
	Board searchBoard; // board clone for ai search

	BoardDisplayer ui;

	public void run() {
		gameMoves = new LinkedList<Move>();
		board = new Board();
		searchBoard = new Board();
		whitePlayerType = askPlayerType(true);
		blackPlayerType = askPlayerType(false);
		NewGame(whitePlayerType, blackPlayerType);
	}

	void Update() {
		zobristDebug = board.ZobristKey;

		if (gameResult == Result.Playing) {
			playerToMove.update();
		}
	}

	public void OnMoveChosen(Move move) {
		board.MakeMove(move);
		searchBoard.MakeMove(move);

		gameMoves.add(move);
		NotifyPlayerToMove();
	}

	void NewGame(PlayerType whitePlayerType, PlayerType blackPlayerType) {
		gameMoves.clear();
		if (loadCustomPosition) {
			board.LoadPosition(customPosition);
			searchBoard.LoadPosition(customPosition);
		} else {
			board.LoadStartPosition();
			searchBoard.LoadStartPosition();
		}
		boolean isWhitePerspective = blackPlayerType == PlayerType.Human && whitePlayerType == PlayerType.AI
				? false
				: true;
		ui = new BoardDisplayer(board, isWhitePerspective);

		whitePlayer = CreatePlayer(whitePlayerType, true);
		blackPlayer = CreatePlayer(blackPlayerType, false);

		gameResult = Result.Playing;
		PrintGameResult(gameResult);

		NotifyPlayerToMove();
	}

	void NotifyPlayerToMove() {
		gameResult = GetGameState();
		ui.displayCurrentState();
		PrintGameResult(gameResult);

		if (gameResult == Result.Playing) {
			getPlayerToMove().notifyTurnToMove();

		} else {
			System.out.println("Game Over!");
		}
	}

	void PrintGameResult(Result result) {
		String rs = "";
		if (result == Result.Playing) {
			rs = "";
		} else if (result == Result.WhiteIsMated || result == Result.BlackIsMated) {
			rs = "Checkmate!";
		} else if (result == Result.FiftyMoveRule) {
			rs = "Draw";
			rs += "\n(50 move rule)";
		} else if (result == Result.Repetition) {
			rs = "Draw";
			rs += "\n(3-fold repetition)";
		} else if (result == Result.Stalemate) {
			rs = "Draw";
			rs += "\n(Stalemate)";
		} else if (result == Result.InsufficientMaterial) {
			rs = "Draw";
			rs += "\n(Insufficient material)";
		}
		System.out.println(rs);
	}

	Result GetGameState() {
		MoveGenerator moveGenerator = new MoveGenerator();
		var moves = moveGenerator.generateMoves(board);

		// Look for mate/stalemate
		if (moves.size() == 0) {
			if (moveGenerator.isInCheck()) {
				return (board.whiteToMove) ? Result.WhiteIsMated : Result.BlackIsMated;
			}
			return Result.Stalemate;
		}

		// Fifty move rule
		if (board.fiftyMoveCounter >= 100) {
			return Result.FiftyMoveRule;
		}

		// Threefold repetition
		int repCount = (int) board.RepetitionPositionHistory.stream().filter(x -> x.equals(board.ZobristKey)).count();
		if (repCount == 3) {
			return Result.Repetition;
		}

		// look for insufficient material
		int numPawns = board.pawns[Board.WHITE_INDEX].size() + board.pawns[Board.BLACK_INDEX].size();
		int numRooks = board.rooks[Board.WHITE_INDEX].size() + board.rooks[Board.BLACK_INDEX].size();
		int numQueens = board.queens[Board.WHITE_INDEX].size() + board.queens[Board.BLACK_INDEX].size();
		int numKnights = board.knights[Board.WHITE_INDEX].size() + board.knights[Board.BLACK_INDEX].size();
		int numBishops = board.bishops[Board.WHITE_INDEX].size() + board.bishops[Board.BLACK_INDEX].size();

		if (numPawns + numRooks + numQueens == 0) {
			if (numKnights == 1 || numBishops == 1) {
				return Result.InsufficientMaterial;
			}
		}

		return Result.Playing;
	}

	private Player CreatePlayer(PlayerType playerType, boolean isWhite) {
		return playerType == PlayerType.Human ? new HumanPlayer(this, board, isWhite) : new AIPlayer(this, board, isWhite);
	}

	private PlayerType askPlayerType(boolean forWhite) {
		String color = forWhite ? "white" : "black";
		System.out.println("Please chose human or computer as " + color + ". (h/c)");
		String input = System.console().readLine();
		if (input.equals("h")) {
			return PlayerType.Human;
		} else if (input.equals("c")) {
			return PlayerType.AI;
		} else {
			System.out.println("Invalid input. Please try again.");
			return askPlayerType(forWhite);
		}
	}

	public Player getPlayerToMove() {
		return (board.whiteToMove) ? whitePlayer : blackPlayer;
	}

	public static void main(String[] args) {
		GameManager gm = new GameManager();
		gm.run();
	}

}