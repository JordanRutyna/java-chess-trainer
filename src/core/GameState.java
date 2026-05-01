package core;

public class GameState {

    public static final int WHITE = 0;
    public static final int BLACK = 1;

    public static final int CASTLE_WHITE_KINGSIDE  = 0b0001;
    public static final int CASTLE_WHITE_QUEENSIDE = 0b0010;
    public static final int CASTLE_BLACK_KINGSIDE  = 0b0100;
    public static final int CASTLE_BLACK_QUEENSIDE = 0b1000;

    public static final int NO_EN_PASSANT = -1;

    public BitBoard board;
    public int sideToMove;       // WHITE or BLACK
    public int castlingRights;   // 4-bit flags above
    public int enPassantSquare;  // target square index, or NO_EN_PASSANT
    public int halfMoveClock;    // for fifty-move rule

    // Private constructor used by copy()
    private GameState() {}

    // Starting position
    public static GameState newGame() {
        GameState gs = new GameState();
        gs.board           = BitBoard.startingPosition();
        gs.sideToMove      = WHITE;
        gs.castlingRights  = CASTLE_WHITE_KINGSIDE | CASTLE_WHITE_QUEENSIDE | CASTLE_BLACK_KINGSIDE | CASTLE_BLACK_QUEENSIDE;
        gs.enPassantSquare = NO_EN_PASSANT;
        gs.halfMoveClock   = 0;
        return gs;
    }

    // Deep copy - used by copy-make move application
    public GameState copy() {
        GameState gs = new GameState();
        gs.board           = this.board.copy();  // see note below
        gs.sideToMove      = this.sideToMove;
        gs.castlingRights  = this.castlingRights;
        gs.enPassantSquare = this.enPassantSquare;
        gs.halfMoveClock   = this.halfMoveClock;
        return gs;
    }

    // Convenience: is it white's turn?
    public boolean isWhiteToMove() { return sideToMove == WHITE; }

    // Flip the side to move
    public void flipSide() {
        sideToMove = (sideToMove == WHITE) ? BLACK : WHITE;
    }

    // Castling right helpers
    public boolean canCastle(int flag) {
        return (castlingRights & flag) != 0;
    }

    public void revokeCastling(int flag) {
        castlingRights &= ~flag;
    }
}