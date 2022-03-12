package core;

import core.util.*;

import java.util.List;
import java.util.Scanner;
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

	// public AISettings aiSettings;

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
		// aiSettings.diagnostics = new Search.SearchDiagnostics (); // TODO uncomment
		// when search is implemented
		whitePlayerType = askPlayerType(true);
		blackPlayerType = askPlayerType(false);
		NewGame(whitePlayerType, blackPlayerType);
	}

	void Update() {
		zobristDebug = board.ZobristKey;

		if (gameResult == Result.Playing) {
			// LogAIDiagnostics ();
			playerToMove.Update();
		}
	}

	public void OnMoveChosen(Move move) {
		board.MakeMove(move);
		searchBoard.MakeMove(move);

		gameMoves.add(move);
		NotifyPlayerToMove();
		// display new state
	}

	public void NewComputerVersusComputerGame() {
		NewGame(PlayerType.AI, PlayerType.AI);
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
		boolean isWhitePerspective = blackPlayerType.equals(PlayerType.Human) && whitePlayerType.equals(PlayerType.AI)
				? false
				: true;
		ui = new BoardDisplayer(board, isWhitePerspective);

		CreatePlayer(whitePlayer, whitePlayerType);
		CreatePlayer(blackPlayer, blackPlayerType);

		gameResult = Result.Playing;
		PrintGameResult(gameResult);

		NotifyPlayerToMove();
	}

	void NotifyPlayerToMove() {
		gameResult = GetGameState();
		PrintGameResult(gameResult);

		if (gameResult == Result.Playing) {
			playerToMove = (board.WhiteToMove) ? whitePlayer : blackPlayer;
			playerToMove.NotifyTurnToMove();

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
		var moves = moveGenerator.GenerateMoves(board);

		// Look for mate/stalemate
		if (moves.size() == 0) {
			if (moveGenerator.InCheck()) {
				return (board.WhiteToMove) ? Result.WhiteIsMated : Result.BlackIsMated;
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

		// Look for insufficient material (not all cases implemented yet)
		int numPawns = board.pawns[Board.WhiteIndex].Count() + board.pawns[Board.BlackIndex].Count();
		int numRooks = board.rooks[Board.WhiteIndex].Count() + board.rooks[Board.BlackIndex].Count();
		int numQueens = board.queens[Board.WhiteIndex].Count() + board.queens[Board.BlackIndex].Count();
		int numKnights = board.knights[Board.WhiteIndex].Count() + board.knights[Board.BlackIndex].Count();
		int numBishops = board.bishops[Board.WhiteIndex].Count() + board.bishops[Board.BlackIndex].Count();

		if (numPawns + numRooks + numQueens == 0) {
			if (numKnights == 1 || numBishops == 1) {
				return Result.InsufficientMaterial;
			}
		}

		return Result.Playing;
	}

	void CreatePlayer(Player player, PlayerType playerType) {
		if (playerType == PlayerType.Human) {
			player = new HumanPlayer(this, board);
		} else {
			// player = new AIPlayer(searchBoard, aiSettings);
		}
	}

	private PlayerType askPlayerType(boolean forWhite) {
		String color = forWhite ? "white" : "black";
		System.out.println("Please chose human or computer as " + color + ". (h/c)");
		Scanner sc = new Scanner(System.in);
		String input = "";
		while (!input.equals("h") && !input.equals("c")) {
			sc.nextLine();
		}
		sc.close();
		return input.equals("c") ? PlayerType.Human : PlayerType.AI;
	}

	public static void main(String[] args) {
		GameManager gm = new GameManager();
		gm.run();
	}
	
}