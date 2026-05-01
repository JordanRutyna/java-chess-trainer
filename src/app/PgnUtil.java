package app;

import core.GameState;
import core.MoveEncoder;
import core.MoveGenerator;
import core.BitBoard;

import java.util.List;

public class PgnUtil {

    // Convert an encoded move to standard algebraic notation given the state BEFORE the move
    public static String toSan(int move, GameState before) {
        int from = MoveEncoder.getFrom(move);
        int to = MoveEncoder.getTo(move);
        int flag = MoveEncoder.getFlag(move);
        boolean white = before.isWhiteToMove();

        // Castling
        if (flag == MoveEncoder.CASTLE_KING) {
            return "O-O";
        }
        if (flag == MoveEncoder.CASTLE_QUEEN) {
            return "O-O-O";
        }

        StringBuilder sb = new StringBuilder();

        String pieceChar = getPieceChar(before, from);
        boolean isPawn = pieceChar.isEmpty();

        // Piece letter (empty for pawns)
        sb.append(pieceChar);

        // Disambiguation: needed when two pieces of the same type can reach the same square
        if (!isPawn) {
            String disambig = getDisambiguation(before, from, to, pieceChar);
            sb.append(disambig);
        }

        // Capture indicator
        boolean isCapture = MoveEncoder.isCapture(move);
        if (isPawn && isCapture) {
            // Pawn captures include the source file
            sb.append((char) ('a' + BitBoard.fileOf(from)));
        }
        if (isCapture) {
            sb.append("x");
        }

        // Destination square
        sb.append(MoveEncoder.squareName(to));

        // Promotion
        if (MoveEncoder.isPromotion(move)) {
            String[] promoChars = {"N", "B", "R", "Q"};
            sb.append("=").append(promoChars[MoveEncoder.getPromo(move)]);
        }

        // Check or checkmate indicator
        GameState after = before.copy();
        MoveGenerator.applyMove(after, move);
        List<Integer> opponentMoves = MoveGenerator.generateLegalMoves(after);
        boolean inCheck = MoveGenerator.isKingInCheck(after, after.sideToMove);
        if (inCheck) {
            sb.append(opponentMoves.isEmpty() ? "#" : "+");
        }

        return sb.toString();
    }

    // Returns the piece letter (N, B, R, Q, K) or empty string for pawns
    private static String getPieceChar(GameState gs, int square) {
        long bit = 1L << square;
        if ((gs.board.whitePawns & bit) != 0 || (gs.board.blackPawns & bit) != 0) {
            return "";
        }
        if ((gs.board.whiteKnights & bit) != 0 || (gs.board.blackKnights & bit) != 0) {
            return "N";
        }
        if ((gs.board.whiteBishops & bit) != 0 || (gs.board.blackBishops & bit) != 0) {
            return "B";
        }
        if ((gs.board.whiteRooks & bit) != 0 || (gs.board.blackRooks & bit) != 0) {
            return "R";
        }
        if ((gs.board.whiteQueens & bit) != 0 || (gs.board.blackQueens & bit) != 0) {
            return "Q";
        }
        if ((gs.board.whiteKing & bit) != 0 || (gs.board.blackKing & bit) != 0) {
            return "K";
        }
        return "";
    }

    // Determine if disambiguation is needed and return the disambiguating string
    private static String getDisambiguation(GameState gs, int from, int to, String pieceChar) {
        List<Integer> allMoves = MoveGenerator.generateLegalMoves(gs);
        boolean sameFile = false, sameRank = false, ambiguous = false;

        for (int move : allMoves) {
            int mFrom = MoveEncoder.getFrom(move);
            int mTo = MoveEncoder.getTo(move);
            if (mFrom == from || mTo != to) {
                continue;
            }
            if (!getPieceChar(gs, mFrom).equals(pieceChar)) {
                continue;
            }
            // Another piece of the same type can reach the same square
            ambiguous = true;
            if (BitBoard.fileOf(mFrom) == BitBoard.fileOf(from)) {
                sameFile = true;
            }
            if (BitBoard.rankOf(mFrom) == BitBoard.rankOf(from)) {
                sameRank = true;
            }
        }

        if (!ambiguous) {
            return "";
        }
        if (!sameFile) {
            return String.valueOf((char) ('a' + BitBoard.fileOf(from)));
        }
        if (!sameRank) {
            return String.valueOf((char) ('1' + BitBoard.rankOf(from)));
        }
        return MoveEncoder.squareName(from); // both file and rank needed
    }
}
