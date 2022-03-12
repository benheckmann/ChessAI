package core;

public abstract class Player {

    GameManager gm;

    public abstract void Update ();

    public abstract void NotifyTurnToMove ();

    protected void ChoseMove(Move move) {
        gm.OnMoveChosen(move);
    }
}
