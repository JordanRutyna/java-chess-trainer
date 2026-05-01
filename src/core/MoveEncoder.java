package core;

public class MoveEncoder {

    // Move type flags (bits 12-15)
    public static final int QUIET = 0;
    public static final int DOUBLE_PUSH = 1;  // pawn moves two squares
    public static final int CASTLE_KING = 2;
    public static final int CASTLE_QUEEN = 3;
    public static final int CAPTURE = 4;
    public static final int EN_PASSANT = 5;
    public static final int PROMOTION = 6;
    public static final int PROMO_CAPTURE = 7;  // promotion + capture combined

    // Promotion piece codes (bits 16-17)
    public static final int PROMO_KNIGHT = 0;
    public static final int PROMO_BISHOP = 1;
    public static final int PROMO_ROOK = 2;
    public static final int PROMO_QUEEN = 3;

    // Encoding
    public static int encode(int from, int to, int flag) {
        return from | (to << 6) | (flag << 12);
    }

    public static int encodePromotion(int from, int to, int flag, int promoPiece) {
        return from | (to << 6) | (flag << 12) | (promoPiece << 16);
    }

    // Decoding
    public static int getFrom(int move) {
        return move & 0x3F;
    }

    public static int getTo(int move) {
        return (move >> 6) & 0x3F;
    }

    public static int getFlag(int move) {
        return (move >> 12) & 0xF;
    }

    public static int getPromo(int move) {
        return (move >> 16) & 0x3;
    }

    // Type checks
    public static boolean isCapture(int move) {
        int flag = getFlag(move);
        return flag == CAPTURE || flag == EN_PASSANT || flag == PROMO_CAPTURE;
    }

    public static boolean isPromotion(int move) {
        int flag = getFlag(move);
        return flag == PROMOTION || flag == PROMO_CAPTURE;
    }

    public static boolean isCastle(int move) {
        int flag = getFlag(move);
        return flag == CASTLE_KING || flag == CASTLE_QUEEN;
    }

    // Readable string for debugging, e.g. "e2e4" or "e7e8q"
    public static String toAlgebraic(int move) {
        int from = getFrom(move);
        int to = getTo(move);
        String result = squareName(from) + squareName(to);
        if (isPromotion(move)) {
            String[] pieces = {"n", "b", "r", "q"};
            result += pieces[getPromo(move)];
        }
        return result;
    }

    public static String squareName(int square) {
        char file = (char) ('a' + (square & 7));
        char rank = (char) ('1' + (square >> 3));
        return "" + file + rank;
    }
}
