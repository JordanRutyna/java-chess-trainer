
import app.GameController;
import app.HumanPlayer;
import core.GameState;
import core.MoveEncoder;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        HumanPlayer white = new HumanPlayer();
        HumanPlayer black = new HumanPlayer();

        GameController controller = new GameController(white, black);
        controller.setListener(new GameController.GameListener() {
            public void onMoveMade(GameState gs, int move) {
                if (move != -1) {
                    System.out.println("Move made: " + MoveEncoder.toAlgebraic(move));
                }
            }

            public void onCheckmate(int winner) {
                System.out.println("Checkmate! Winner: " + (winner == GameState.WHITE ? "White" : "Black"));
            }

            public void onStalemate() {
                System.out.println("Stalemate!");
            }

            public void onCheck(int side) {
                System.out.println("Check! " + (side == GameState.WHITE ? "White" : "Black") + " is in check.");
            }
        });

        controller.startGame();

        // Simulate e2e4, e7e5
        Thread.sleep(100);
        white.submitMove(MoveEncoder.encode(12, 28, MoveEncoder.DOUBLE_PUSH)); // e2e4
        Thread.sleep(100);
        black.submitMove(MoveEncoder.encode(52, 36, MoveEncoder.DOUBLE_PUSH)); // e7e5
        Thread.sleep(100);

        controller.stopGame();
    }
}
