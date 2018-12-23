import java.util.Arrays;
import java.util.Scanner;

/**
 * The type Cmd game.
 */
public class cmdGame {
	/**
	 * The entry point of application.
	 *
	 * @param args the input arguments
	 */
	@SuppressWarnings("Duplicates")
	public static void main(String[] args) {
		Timer.valid = false;
		Board board = new Board();
		AI aiBlack = new AI(board, Board.BLACK, .9);
		AI aiWhite = new AI(board, Board.WHITE, .8);

		Scanner scanner = new Scanner(System.in);
		System.out.print("Input \"y\" to start player vs. AI.\nOtherwise, two AIs will start to play with each other" +
						"\nInput: ");
		boolean involvePlayer = scanner.next().toLowerCase().equals("y");
		System.out.println(board);

		do {
			if (involvePlayer) {
				System.out.println("Please Input: ");
				int x = scanner.nextInt(), y = scanner.nextInt();
				if (x < 0 || y < 0 || x > 14 || y > 14 || board.getBoard()[x][y] != Board.EMPTY) continue;
				board.setChess(x, y, true);
			} else {
				System.out.println("Black●:");
				System.out.println("Thinking...");
				int[] aiInput = aiBlack.iterativeDeepening(0, false);
				System.out.println("boardScoreCache = " + board.boardScoreCache.size());
				System.out.println(Arrays.toString(aiInput));
				board.setChess(aiInput[0], aiInput[1], true);
			}
			System.out.println(board);
			System.out.printf("White: %d\nBlack: %d\n\n", board.scoreBoard(Board.WHITE, 1),
							board.scoreBoard(Board.BLACK, 1));
			if (board.isEnd() != Board.CONTINUE) break;

			System.out.println("White○:");
			System.out.println("Thinking...");
			int[] aiInput = aiWhite.iterativeDeepening(0, false);
			System.out.println("boardScoreCache = " + board.boardScoreCache.size());
			System.out.println(Arrays.toString(aiInput));
			board.setChess(aiInput[0], aiInput[1], true);
			System.out.println(board);
			System.out.printf("White: %d\nBlack: %d\n\n", board.scoreBoard(Board.WHITE, 1),
							board.scoreBoard(Board.BLACK, 1));
		} while (board.isEnd() == Board.CONTINUE);
		Timer.print();
		System.out.println();
	}
}
