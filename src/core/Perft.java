package core;

import java.util.List;

public class Perft {

    // Returns total node count at the given depth
    public static long perft(GameState gs, int depth) {
        if (depth == 0) {
            return 1;
        }
        List<Integer> moves = MoveGenerator.generateLegalMoves(gs);
        if (depth == 1) {
            return moves.size();
        }
        long nodes = 0;
        for (int move : moves) {
            GameState next = gs.copy();
            MoveGenerator.applyMove(next, move);
            nodes += perft(next, depth-1);
        }
        return nodes;
    }

    // Divide: shows node count per root move, useful for isolating bugs
    public static void divide(GameState gs, int depth) {
        List<Integer> moves = MoveGenerator.generateLegalMoves(gs);
        long total = 0;
        for (int move : moves) {
            GameState next = gs.copy();
            MoveGenerator.applyMove(next, move);
            long count = perft(next, depth - 1);
            System.out.println(MoveEncoder.toAlgebraic(move) + ": " + count);
            total += count;
        }
        System.out.println("Total: " + total);
    }

    // Run standard depth suite from starting position and compare to known values
    public static void runStandardSuite() {
        long[] expected = {20, 400, 8902, 197281, 4865609};
        GameState gs = GameState.newGame();
        System.out.println("Running perft suite from starting position...\n");
        boolean allPassed = true;
        for (int depth = 1; depth <= 5; depth++) {
            long result = perft(gs, depth);
            boolean passed = result == expected[depth - 1];
            if (!passed) {
                allPassed = false;
            }
            System.out.printf("Depth %d: %,d %s (expected %,d)%n",
                    depth, result, passed ? "PASS" : "FAIL", expected[depth - 1]);
        }
        System.out.println(allPassed ? "\nAll tests passed." : "\nOne or more tests failed.");
    }
}
