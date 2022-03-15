package core;

public abstract class Player {

    GameManager gm;
    Board board;
    public boolean isWhite;

    public Player(GameManager gm, Board board, boolean isWhite) {
        this.gm = gm;
        this.board = board;
        this.isWhite = isWhite;
    }

    public abstract void update();

    public abstract void notifyTurnToMove ();

    public void choseMove(Move move) {
        gm.OnMoveChosen(move);
    }
}
