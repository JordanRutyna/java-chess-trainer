package core;

public class BitBoard {

    // The 12 piece boards
    public long whitePawns, whiteKnights, whiteBishops;
    public long whiteRooks, whiteQueens, whiteKing;
    public long blackPawns, blackKnights, blackBishops;
    public long blackRooks, blackQueens, blackKing;

    // Derived composites (call updateComposites() after any change)
    public long whitePieces, blackPieces, allPieces;

    public void updateComposites() {
        whitePieces = whitePawns | whiteKnights | whiteBishops | whiteRooks | whiteQueens  | whiteKing;
        blackPieces = blackPawns | blackKnights | blackBishops | blackRooks | blackQueens  | blackKing;
        allPieces   = whitePieces | blackPieces;
    }

    // Return a board in the starting position
    public static BitBoard startingPosition() {
        BitBoard b = new BitBoard();
        b.whitePawns   = 0x000000000000FF00L;
        b.whiteRooks   = 0x0000000000000081L;
        b.whiteKnights = 0x0000000000000042L;
        b.whiteBishops = 0x0000000000000024L;
        b.whiteQueens  = 0x0000000000000008L;
        b.whiteKing    = 0x0000000000000010L;
        b.blackPawns   = 0x00FF000000000000L;
        b.blackRooks   = 0x8100000000000000L;
        b.blackKnights = 0x4200000000000000L;
        b.blackBishops = 0x2400000000000000L;
        b.blackQueens  = 0x0800000000000000L;
        b.blackKing    = 0x1000000000000000L;
        b.updateComposites();
        return b;
    }
    
    // Deep copy - used by GameState.copy()
    public BitBoard copy() {
      BitBoard b = new BitBoard();
      b.whitePawns   = this.whitePawns;
      b.whiteKnights = this.whiteKnights;
      b.whiteBishops = this.whiteBishops;
      b.whiteRooks   = this.whiteRooks;
      b.whiteQueens  = this.whiteQueens;
      b.whiteKing    = this.whiteKing;
      b.blackPawns   = this.blackPawns;
      b.blackKnights = this.blackKnights;
      b.blackBishops = this.blackBishops;
      b.blackRooks   = this.blackRooks;
      b.blackQueens  = this.blackQueens;
      b.blackKing    = this.blackKing;
      b.updateComposites();
      return b;
    }

    // Square index helpers
    public static int squareOf(int file, int rank) {
        return rank * 8 + file;  // file and rank both 0-indexed
    }

    public static int fileOf(int square) { return square & 7; }
    public static int rankOf(int square) { return square >> 3; }

    // Debug: print the board as ASCII
    public static void print(long board) {
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int sq = rank * 8 + file;
                System.out.print((board >> sq & 1L) == 1L ? "1 " : ". ");
            }
            System.out.println("  rank " + (rank + 1));
        }
        System.out.println("a b c d e f g h\n");

        System.out.println("0x" + Long.toHexString(board));
        System.out.println("\nboard:\n"+board);
    }
}
