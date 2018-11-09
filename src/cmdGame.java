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
  public static void main(String[] args) {
    Board board = new Board();
    AI aiBlack = new AI(board, Board.BLACK, 1);
    AI aiWhite = new AI(board, Board.WHITE, 1);
    System.out.println("Initializing...");
    long t = System.currentTimeMillis();
    aiBlack.initialize(3, true);
    System.out.println(System.currentTimeMillis() - t);
//    Timer.print();
    System.out.println("Finished");

    Scanner scanner = new Scanner(System.in);
    do {
//      int[] userInput = {scanner.nextInt(), scanner.nextInt()};
//      if (userInput[0] == -1 || userInput[1] == -1) {
//        board.withdraw();
//        board.withdraw();
//        System.out.println("Withdraw!");
//        System.out.println(board);
//        continue;
//      }
//      try {
//        board.setChess(userInput[0], userInput[1], true);
//      } catch (RedundantChessException | ValueOutOfRangeException e) {
//        e.printStackTrace();
//        continue;
//      }
//
//      System.out.println(board);

      System.out.println("Black●:");
      System.out.println("Thinking...");
      int[] aiInput = aiBlack.iterativeDeepening(0);
      System.out.println(Arrays.toString(aiInput));
      board.setChess(aiInput[0], aiInput[1], true);
      System.out.println(board);
      System.out.printf(
              "White: %d\nBlack: %d\n\n",
              board.scoreBoard(Board.WHITE, 1),
              board.scoreBoard(Board.BLACK, 1)
      );
      System.out.print("Press enter to continue");
//    scanner.nextLine();

      System.out.println("White○:");
      System.out.println("Thinking...");
      aiInput = aiWhite.iterativeDeepening(0);
      System.out.println(Arrays.toString(aiInput));
      board.setChess(aiInput[0], aiInput[1], true);
      System.out.println(board);
      System.out.printf(
              "White: %d\nBlack: %d\n\n",
              board.scoreBoard(Board.WHITE, 1),
              board.scoreBoard(Board.BLACK, 1)
      );
      System.out.print("Press enter to continue");
//      scanner.nextLine();
    } while (board.isEnd() == Board.CONTINUE);
    Timer.print();
    System.out.println();
  }
}
