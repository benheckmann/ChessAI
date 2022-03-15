package core;

public abstract class Player {

    GameManager gm;
    Board board;

    public Player(GameManager gm, Board board) {
        this.gm = gm;
        this.board = board;
    }

    public abstract void update();

    public abstract void notifyTurnToMove ();

    public void choseMove(Move move) {
        gm.OnMoveChosen(move);
    }
}
