package core.util;

/**
 * Holds a list of all squares occupied by a given piece type.
 */
public class PieceList {

	// Indices of squares occupied by given piece type (only elements up to Count are valid, the rest are unused/garbage)
	public int[] occupiedSquares;
	// Map to go from index of a square, to the index in the occupiedSquares array where that square is stored
	int[] map;
	int numPieces;

	/**
	 * Constructs a new PieceList.
	 * 
	 * @param numPieces The number of pieces of the given type that can be on the board at once.
	 */
	public PieceList(int maxPieceCount) {
		occupiedSquares = new int[maxPieceCount];
		map = new int[64];
		numPieces = 0;
	}

	public PieceList() {
		this(16);
	}

	public int size() {
		return numPieces;
	}

	public void addPieceAtSquare (int square) {
		occupiedSquares[numPieces] = square;
		map[square] = numPieces;
		numPieces++;
	}

	public void removePieceAtSquare (int square) {
		int pieceIndex = map[square];
		occupiedSquares[pieceIndex] = occupiedSquares[numPieces - 1];
		map[occupiedSquares[pieceIndex]] = pieceIndex;
		numPieces--;
	}

	public void movePiece (int startSquare, int targetSquare) {
		int pieceIndex = map[startSquare];
		occupiedSquares[pieceIndex] = targetSquare;
		map[targetSquare] = pieceIndex;
	}

	// public int this [int index] => occupiedSquares[index];
	public int get(int index) {
		return occupiedSquares[index];
	}

}