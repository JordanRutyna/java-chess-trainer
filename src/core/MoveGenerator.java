package core;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {

    public static List<Integer> generateLegalMoves(GameState gs) {
        List<Integer> pseudoLegal = generatePseudoLegal(gs);
        List<Integer> legal = new ArrayList<>();

        for (int move : pseudoLegal) {
            GameState after = gs.copy();
            applyMove(after, move);
            if (!isKingInCheck(after, gs.sideToMove)) {
                legal.add(move);
            }
        }
        return legal;
    }

    private static List<Integer> generatePseudoLegal(GameState gs) {
        List<Integer> moves = new ArrayList<>();
        if (gs.sideToMove == GameState.WHITE) {
            generatePawnMoves(gs, moves, true);
            generateKnightMoves(gs, moves, true);
            generateBishopMoves(gs, moves, true);
            generateRookMoves(gs, moves, true);
            generateQueenMoves(gs, moves, true);
            generateKingMoves(gs, moves, true);
        } else {
            generatePawnMoves(gs, moves, false);
            generateKnightMoves(gs, moves, false);
            generateBishopMoves(gs, moves, false);
            generateRookMoves(gs, moves, false);
            generateQueenMoves(gs, moves, false);
            generateKingMoves(gs, moves, false);
        }
        return moves;
    }

    private static final long NOT_A_FILE = 0xFEFEFEFEFEFEFEFEL;
    private static final long NOT_H_FILE = 0x7F7F7F7F7F7F7F7FL;
    private static final long NOT_AB_FILE = 0xFCFCFCFCFCFCFCFCL;
    private static final long NOT_GH_FILE = 0x3F3F3F3F3F3F3F3FL;

    private static void generateKnightMoves(GameState gs, List<Integer> moves, boolean white) {
        long knights = white ? gs.board.whiteKnights : gs.board.blackKnights;
        long ownPieces = white ? gs.board.whitePieces : gs.board.blackPieces;

        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1; // clear lowest set bit

            long attacks = knightAttacks(from) & ~ownPieces;
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                boolean isCapture = (gs.board.allPieces & (1L << to)) != 0;
                moves.add(MoveEncoder.encode(from, to,
                        isCapture ? MoveEncoder.CAPTURE : MoveEncoder.QUIET));
            }
        }
    }

    private static long knightAttacks(int sq) {
        long b = 1L << sq;
        return ((b << 17) & NOT_A_FILE)
                | ((b << 15) & NOT_H_FILE)
                | ((b << 10) & NOT_AB_FILE)
                | ((b << 6) & NOT_GH_FILE)
                | ((b >> 6) & NOT_AB_FILE)
                | ((b >> 10) & NOT_GH_FILE)
                | ((b >> 15) & NOT_A_FILE)
                | ((b >> 17) & NOT_H_FILE);
    }

    private static void generateKingMoves(GameState gs, List<Integer> moves, boolean white) {
        long king = white ? gs.board.whiteKing : gs.board.blackKing;
        long ownPieces = white ? gs.board.whitePieces : gs.board.blackPieces;
        int from = Long.numberOfTrailingZeros(king);

        long attacks = kingAttacks(from) & ~ownPieces;
        while (attacks != 0) {
            int to = Long.numberOfTrailingZeros(attacks);
            attacks &= attacks - 1;
            boolean isCapture = (gs.board.allPieces & (1L << to)) != 0;
            moves.add(MoveEncoder.encode(from, to,
                    isCapture ? MoveEncoder.CAPTURE : MoveEncoder.QUIET));
        }

        // Castling
        generateCastlingMoves(gs, moves, white);
    }

    private static long kingAttacks(int sq) {
        long b = 1L << sq;
        return ((b << 9) & NOT_A_FILE)
                | (b << 8)
                | ((b << 7) & NOT_H_FILE)
                | ((b << 1) & NOT_A_FILE)
                | ((b >> 1) & NOT_H_FILE)
                | ((b >> 7) & NOT_A_FILE)
                | (b >> 8)
                | ((b >> 9) & NOT_H_FILE);
    }

    private static void generateCastlingMoves(GameState gs, List<Integer> moves, boolean white) {
        if (white) {
            // Kingside: e1=4, f1=5, g1=6
            if (gs.canCastle(GameState.CASTLE_WHITE_KINGSIDE)) {
                long between = (1L << 5) | (1L << 6);
                if ((gs.board.allPieces & between) == 0
                        && !isAttacked(gs, 4, false)
                        && !isAttacked(gs, 5, false)
                        && !isAttacked(gs, 6, false)) {
                    moves.add(MoveEncoder.encode(4, 6, MoveEncoder.CASTLE_KING));
                }
            }
            // Queenside: e1=4, d1=3, c1=2, b1=1
            if (gs.canCastle(GameState.CASTLE_WHITE_QUEENSIDE)) {
                long between = (1L << 1) | (1L << 2) | (1L << 3);
                if ((gs.board.allPieces & between) == 0
                        && !isAttacked(gs, 4, false)
                        && !isAttacked(gs, 3, false)
                        && !isAttacked(gs, 2, false)) {
                    moves.add(MoveEncoder.encode(4, 2, MoveEncoder.CASTLE_QUEEN));
                }
            }
        } else {
            // Kingside: e8=60, f8=61, g8=62
            if (gs.canCastle(GameState.CASTLE_BLACK_KINGSIDE)) {
                long between = (1L << 61) | (1L << 62);
                if ((gs.board.allPieces & between) == 0
                        && !isAttacked(gs, 60, true)
                        && !isAttacked(gs, 61, true)
                        && !isAttacked(gs, 62, true)) {
                    moves.add(MoveEncoder.encode(60, 62, MoveEncoder.CASTLE_KING));
                }
            }
            // Queenside: e8=60, d8=59, c8=58, b8=57
            if (gs.canCastle(GameState.CASTLE_BLACK_QUEENSIDE)) {
                long between = (1L << 57) | (1L << 58) | (1L << 59);
                if ((gs.board.allPieces & between) == 0
                        && !isAttacked(gs, 60, true)
                        && !isAttacked(gs, 59, true)
                        && !isAttacked(gs, 58, true)) {
                    moves.add(MoveEncoder.encode(60, 58, MoveEncoder.CASTLE_QUEEN));
                }
            }
        }
    }

    private static void generateRookMoves(GameState gs, List<Integer> moves, boolean white) {
        long rooks = white ? gs.board.whiteRooks : gs.board.blackRooks;
        long ownPieces = white ? gs.board.whitePieces : gs.board.blackPieces;
        long enemies = white ? gs.board.blackPieces : gs.board.whitePieces;

        while (rooks != 0) {
            int from = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            generateRayMoves(gs, moves, from, ownPieces, enemies, true, false);
        }
    }

    private static void generateBishopMoves(GameState gs, List<Integer> moves, boolean white) {
        long bishops = white ? gs.board.whiteBishops : gs.board.blackBishops;
        long ownPieces = white ? gs.board.whitePieces : gs.board.blackPieces;
        long enemies = white ? gs.board.blackPieces : gs.board.whitePieces;

        while (bishops != 0) {
            int from = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            generateRayMoves(gs, moves, from, ownPieces, enemies, false, true);
        }
    }

    private static void generateQueenMoves(GameState gs, List<Integer> moves, boolean white) {
        long queens = white ? gs.board.whiteQueens : gs.board.blackQueens;
        long ownPieces = white ? gs.board.whitePieces : gs.board.blackPieces;
        long enemies = white ? gs.board.blackPieces : gs.board.whitePieces;

        while (queens != 0) {
            int from = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            generateRayMoves(gs, moves, from, ownPieces, enemies, true, true);
        }
    }

    // dirs: straight = rook directions, diagonal = bishop directions
    private static void generateRayMoves(GameState gs, List<Integer> moves,
            int from, long ownPieces, long enemies, boolean straight, boolean diagonal) {

        int[][] dirs;
        if (straight && diagonal) {
            dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        } else if (straight) {
            dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        } else {
            dirs = new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        }

        int fromFile = BitBoard.fileOf(from);
        int fromRank = BitBoard.rankOf(from);

        for (int[] dir : dirs) {
            int f = fromFile + dir[0];
            int r = fromRank + dir[1];
            while (f >= 0 && f < 8 && r >= 0 && r < 8) {
                int to = r * 8 + f;
                long toBit = 1L << to;
                if ((ownPieces & toBit) != 0) break; // blocked by own piece
                if ((enemies & toBit) != 0) {
                    moves.add(MoveEncoder.encode(from, to, MoveEncoder.CAPTURE));
                    break; // capture and stop
                }
                moves.add(MoveEncoder.encode(from, to, MoveEncoder.QUIET));
                f += dir[0];
                r += dir[1];
            }
        }
    }

    private static void generatePawnMoves(GameState gs, List<Integer> moves, boolean white) {
        long pawns = white ? gs.board.whitePawns : gs.board.blackPawns;
        long empty = ~gs.board.allPieces;
        long enemies = white ? gs.board.blackPieces : gs.board.whitePieces;
        int dir = white ? 8 : -8;  // direction of forward movement
        int startRank = white ? 1 : 6; // rank index where pawns start
        int promoRank = white ? 7 : 0; // rank index where promotion happens

        // System.out.println("Side: " + (white ? "WHITE" : "BLACK"));
        // System.out.println("Pawn count: " + Long.bitCount(pawns));
        // BitBoard.print(pawns);

        // System.out.println("out");
        // BitBoard.print(pawns);
        while (pawns != 0) {
            // System.out.println("in");
            // BitBoard.print(pawns);
            int from = Long.numberOfTrailingZeros(pawns);
            pawns &= pawns - 1;
            int fromRank = BitBoard.rankOf(from);
            int fromFile = BitBoard.fileOf(from);

            // Single push
            int singleTo = from + dir;
            if (singleTo >= 0 && singleTo < 64 && (empty & (1L << singleTo)) != 0) {
                if (BitBoard.rankOf(singleTo) == promoRank) {
                    addPromotions(moves, from, singleTo, false);
                } else {
                    moves.add(MoveEncoder.encode(from, singleTo, MoveEncoder.QUIET));

                    // Double push from start rank
                    if (fromRank == startRank) {
                        int doubleTo = from + dir * 2;
                        if ((empty & (1L << doubleTo)) != 0) {
                            moves.add(MoveEncoder.encode(from, doubleTo, MoveEncoder.DOUBLE_PUSH));
                        }
                    }
                }
            }

            int[] captureFiles = {fromFile - 1, fromFile + 1};
            for (int cf : captureFiles) {
                if (cf < 0 || cf > 7) continue;
                int capRank = fromRank + (white ? 1 : -1);
                int capSquare = capRank * 8 + cf;
                if (capRank < 0 || capRank > 7) continue;

                long capBit = 1L << capSquare;
                if ((enemies & capBit) != 0) {
                    if (capRank == promoRank) {
                        addPromotions(moves, from, capSquare, true);
                    } else {
                        moves.add(MoveEncoder.encode(from, capSquare, MoveEncoder.CAPTURE));
                    }
                }

                // En passant
                if (gs.enPassantSquare == capSquare) {
                    moves.add(MoveEncoder.encode(from, capSquare, MoveEncoder.EN_PASSANT));
                }
            }
        }
    }

    private static void addPromotions(List<Integer> moves, int from, int to, boolean isCapture) {
        int flag = isCapture ? MoveEncoder.PROMO_CAPTURE : MoveEncoder.PROMOTION;
        moves.add(MoveEncoder.encodePromotion(from, to, flag, MoveEncoder.PROMO_QUEEN));
        moves.add(MoveEncoder.encodePromotion(from, to, flag, MoveEncoder.PROMO_ROOK));
        moves.add(MoveEncoder.encodePromotion(from, to, flag, MoveEncoder.PROMO_BISHOP));
        moves.add(MoveEncoder.encodePromotion(from, to, flag, MoveEncoder.PROMO_KNIGHT));
    }

    public static boolean isKingInCheck(GameState gs, int side) {
        long kingBoard = (side == GameState.WHITE) ? gs.board.whiteKing : gs.board.blackKing;
        int kingSquare = Long.numberOfTrailingZeros(kingBoard);
        boolean attackedByWhite = (side == GameState.BLACK);
        return isAttacked(gs, kingSquare, attackedByWhite);
    }

    // Is the given square attacked by the given color?
    public static boolean isAttacked(GameState gs, int square, boolean byWhite) {
        long knights = byWhite ? gs.board.whiteKnights : gs.board.blackKnights;
        long bishops = byWhite ? gs.board.whiteBishops : gs.board.blackBishops;
        long rooks = byWhite ? gs.board.whiteRooks : gs.board.blackRooks;
        long queens = byWhite ? gs.board.whiteQueens : gs.board.blackQueens;
        long pawns = byWhite ? gs.board.whitePawns : gs.board.blackPawns;
        long king = byWhite ? gs.board.whiteKing : gs.board.blackKing;

        if ((knightAttacks(square) & knights) != 0) {
            return true;
        }
        if ((kingAttacks(square) & king) != 0) {
            return true;
        }

        // Pawn attacks (check from the target square's perspective)
        long squareBit = 1L << square;
        if (byWhite) {
            // White pawns attack upward, so from square, white pawns attack from below-diagonal
            if (((squareBit >> 7) & NOT_A_FILE & pawns) != 0) {
                return true;
            }
            if (((squareBit >> 9) & NOT_H_FILE & pawns) != 0) {
                return true;
            }
        } else {
            if (((squareBit << 7) & NOT_H_FILE & pawns) != 0) {
                return true;
            }
            if (((squareBit << 9) & NOT_A_FILE & pawns) != 0) {
                return true;
            }
        }

        // Sliding pieces: cast rays from the target square outward
        int file = BitBoard.fileOf(square);
        int rank = BitBoard.rankOf(square);

        // Rook/queen rays (straight)
        int[][] straight = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : straight) {
            int f = file + dir[0], r = rank + dir[1];
            while (f >= 0 && f < 8 && r >= 0 && r < 8) {
                long bit = 1L << (r * 8 + f);
                if ((gs.board.allPieces & bit) != 0) {
                    if ((rooks & bit) != 0 || (queens & bit) != 0) {
                        return true;
                    }
                    break;
                }
                f += dir[0];
                r += dir[1];
            }
        }

        // Bishop/queen rays (diagonal)
        int[][] diagonal = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] dir : diagonal) {
            int f = file + dir[0], r = rank + dir[1];
            while (f >= 0 && f < 8 && r >= 0 && r < 8) {
                long bit = 1L << (r * 8 + f);
                if ((gs.board.allPieces & bit) != 0) {
                    if ((bishops & bit) != 0 || (queens & bit) != 0) {
                        return true;
                    }
                    break;
                }
                f += dir[0];
                r += dir[1];
            }
        }

        return false;
    }

    public static void applyMove(GameState gs, int move) {
        // System.out.println("Applying: " + MoveEncoder.toAlgebraic(move)
        //         + " white pawns before: " + Long.bitCount(gs.board.whitePawns)
        //         + " black pawns before: " + Long.bitCount(gs.board.blackPawns));
        int from = MoveEncoder.getFrom(move);
        int to = MoveEncoder.getTo(move);
        int flag = MoveEncoder.getFlag(move);
        boolean white = gs.sideToMove == GameState.WHITE;

        long fromBit = 1L << from;
        long toBit = 1L << to;

        // Clear en passant (will be reset if this is a double push)
        gs.enPassantSquare = GameState.NO_EN_PASSANT;

        // Remove any captured piece from the destination
        if (flag == MoveEncoder.CAPTURE || flag == MoveEncoder.PROMO_CAPTURE) {
            clearSquare(gs.board, toBit, !white);
        }

        // Move the piece
        movePiece(gs.board, fromBit, toBit, white);

        // Special cases
        switch (flag) {
            case MoveEncoder.DOUBLE_PUSH:
                gs.enPassantSquare = white ? from + 8 : from - 8;
                break;

            case MoveEncoder.EN_PASSANT:
                long capturedPawn = white ? (toBit >> 8) : (toBit << 8);
                clearSquare(gs.board, capturedPawn, !white);
                break;

            case MoveEncoder.CASTLE_KING:
                if (white) {
                    gs.board.whiteRooks &= ~(1L << 7);
                    gs.board.whiteRooks |= (1L << 5);
                } else {
                    gs.board.blackRooks &= ~(1L << 63);
                    gs.board.blackRooks |= (1L << 61);
                }
                break;

            case MoveEncoder.CASTLE_QUEEN:
                if (white) {
                    gs.board.whiteRooks &= ~(1L << 0);
                    gs.board.whiteRooks |= (1L << 3);
                } else {
                    gs.board.blackRooks &= ~(1L << 56);
                    gs.board.blackRooks |= (1L << 59);
                }
                break;

            case MoveEncoder.PROMOTION:
            case MoveEncoder.PROMO_CAPTURE:
                // Remove the pawn that was just moved to the back rank
                if (white) {
                    gs.board.whitePawns &= ~toBit; 
                }else {
                    gs.board.blackPawns &= ~toBit;
                }
                // Place the promoted piece
                int promo = MoveEncoder.getPromo(move);
                promotePiece(gs.board, toBit, white, promo);
                break;
        }

        // Revoke castling rights
        updateCastlingRights(gs, from, to);

        gs.board.updateComposites();
        gs.flipSide();
    }

    private static void movePiece(BitBoard b, long fromBit, long toBit, boolean white) {
        if (white) {
            if ((b.whitePawns & fromBit) != 0) {
                b.whitePawns ^= fromBit;
                b.whitePawns |= toBit;
            } else if ((b.whiteKnights & fromBit) != 0) {
                b.whiteKnights ^= fromBit;
                b.whiteKnights |= toBit;
            } else if ((b.whiteBishops & fromBit) != 0) {
                b.whiteBishops ^= fromBit;
                b.whiteBishops |= toBit;
            } else if ((b.whiteRooks & fromBit) != 0) {
                b.whiteRooks ^= fromBit;
                b.whiteRooks |= toBit;
            } else if ((b.whiteQueens & fromBit) != 0) {
                b.whiteQueens ^= fromBit;
                b.whiteQueens |= toBit;
            } else if ((b.whiteKing & fromBit) != 0) {
                b.whiteKing ^= fromBit;
                b.whiteKing |= toBit;
            }
        } else {
            if ((b.blackPawns & fromBit) != 0) {
                b.blackPawns ^= fromBit;
                b.blackPawns |= toBit;
            } else if ((b.blackKnights & fromBit) != 0) {
                b.blackKnights ^= fromBit;
                b.blackKnights |= toBit;
            } else if ((b.blackBishops & fromBit) != 0) {
                b.blackBishops ^= fromBit;
                b.blackBishops |= toBit;
            } else if ((b.blackRooks & fromBit) != 0) {
                b.blackRooks ^= fromBit;
                b.blackRooks |= toBit;
            } else if ((b.blackQueens & fromBit) != 0) {
                b.blackQueens ^= fromBit;
                b.blackQueens |= toBit;
            } else if ((b.blackKing & fromBit) != 0) {
                b.blackKing ^= fromBit;
                b.blackKing |= toBit;
            }
        }
    }

    private static void clearSquare(BitBoard b, long bit, boolean clearWhite) {
        if (clearWhite) {
            b.whitePawns &= ~bit;
            b.whiteKnights &= ~bit;
            b.whiteBishops &= ~bit;
            b.whiteRooks &= ~bit;
            b.whiteQueens &= ~bit;
            b.whiteKing &= ~bit;
        } else {
            b.blackPawns &= ~bit;
            b.blackKnights &= ~bit;
            b.blackBishops &= ~bit;
            b.blackRooks &= ~bit;
            b.blackQueens &= ~bit;
            b.blackKing &= ~bit;
        }
    }

    private static void promotePiece(BitBoard b, long toBit, boolean white, int promo) {
        if (white) {
            switch (promo) {
                case MoveEncoder.PROMO_QUEEN:
                    b.whiteQueens |= toBit;
                    break;
                case MoveEncoder.PROMO_ROOK:
                    b.whiteRooks |= toBit;
                    break;
                case MoveEncoder.PROMO_BISHOP:
                    b.whiteBishops |= toBit;
                    break;
                case MoveEncoder.PROMO_KNIGHT:
                    b.whiteKnights |= toBit;
                    break;
            }
        } else {
            switch (promo) {
                case MoveEncoder.PROMO_QUEEN:
                    b.blackQueens |= toBit;
                    break;
                case MoveEncoder.PROMO_ROOK:
                    b.blackRooks |= toBit;
                    break;
                case MoveEncoder.PROMO_BISHOP:
                    b.blackBishops |= toBit;
                    break;
                case MoveEncoder.PROMO_KNIGHT:
                    b.blackKnights |= toBit;
                    break;
            }
        }
    }

    private static void updateCastlingRights(GameState gs, int from, int to) {
        // King moves revoke both rights for that color
        if (from == 4) {
            gs.castlingRights &= ~(GameState.CASTLE_WHITE_KINGSIDE | GameState.CASTLE_WHITE_QUEENSIDE);
        }
        if (from == 60) {
            gs.castlingRights &= ~(GameState.CASTLE_BLACK_KINGSIDE | GameState.CASTLE_BLACK_QUEENSIDE);
        }
        // Rook moves or captures on rook squares revoke the specific right
        if (from == 0 || to == 0) {
            gs.castlingRights &= ~GameState.CASTLE_WHITE_QUEENSIDE;
        }
        if (from == 7 || to == 7) {
            gs.castlingRights &= ~GameState.CASTLE_WHITE_KINGSIDE;
        }
        if (from == 56 || to == 56) {
            gs.castlingRights &= ~GameState.CASTLE_BLACK_QUEENSIDE;
        }
        if (from == 63 || to == 63) {
            gs.castlingRights &= ~GameState.CASTLE_BLACK_KINGSIDE;
        }
    }
}
