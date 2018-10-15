import java.io.*;
import java.util.*;

/**
 * The AI class.
 */
@SuppressWarnings("WeakerAccess")
public class AI {
  /**
   * The constant EMPTY.
   */
  private static final int EMPTY = 10000;
  /**
   * The constant BLACK.
   */
  private static final int BLACK = 10001;
  /**
   * The constant WHITE.
   */
  private static final int WHITE = 10002;
  /**
   * The constant CONTINUE.
   */
  private static final int CONTINUE = 10004;
  private Board board;
  private int aiNum;
  private double weight;


  /**
   * Instantiates a new Ai.
   *
   * @param board  the chess board
   * @param aiNum  the player number of AI
   * @param weight the weight
   */
  public AI(Board board, int aiNum, double weight) {
    this.board = board;
    if (aiNum != BLACK && aiNum != WHITE) {
      throw new ValueOutOfRangeException();
    }
    this.aiNum = aiNum;
    if (weight < 0 || weight > 2) {
      throw new ValueOutOfRangeException();
    }
    this.weight = weight;
  }

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    AI ai = new AI(new Board(), WHITE, 0.8);
    File file = new File("boardCache.cache");
    try {
      //noinspection ResultOfMethodCallIgnored
      file.createNewFile();
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    ai.initialize(4, false);

    try (FileOutputStream fileOutputStream = new FileOutputStream("boardCache.cache")) {
      ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
      outputStream.writeObject(ai.board.boardScoreCache);
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Initialize board score cache tto speed up calculation in first few steps
   *
   * @param deep depth of min-max search (disabled then read is true)
   * @param read whether to read data from file
   */
  public void initialize(int deep, boolean read) {
    if (read) {
      boolean success = false;
      try (FileInputStream file = new FileInputStream("boardCache.cache")) {
        ObjectInputStream inputStream = new ObjectInputStream(file);
        //noinspection unchecked
        board.boardScoreCache = (Map<Integer, Map<Integer, Integer>>) inputStream.readObject();
        success = true;
      } catch (ClassNotFoundException e) {
        System.out.println("Data file load fail! File format wrong");
      } catch (FileNotFoundException e) {
        System.out.println("Data file does not exist");
      } catch (IOException e) {
        System.out.println("Open data file failed!");
      }
      if (success)
        return;
      else
        System.out.println("Load data failed!");
    }
    this.board.setChess(7, 7, false);
    minMaxSearch(deep, false);
    this.board.reset();

  }


  /**
   * Generate points to be judged
   *
   * @param player the player number
   * @return List contain all valid points in order
   */
  @SuppressWarnings("ConstantConditions")
  public List<int[]> generatePossiblePoints(int player) {
    Timer.set("generatePossiblePoints");
    final int neighborDist = 2;

    List<int[]> five = new ArrayList<>();
    List<int[]> four = new ArrayList<>();
    List<int[]> blockedFour = new ArrayList<>();
    List<int[]> doubleThree = new ArrayList<>();
    List<int[]> three = new ArrayList<>();
    List<int[]> two = new ArrayList<>();
    List<int[]> neighbor = new ArrayList<>();
    List<int[]> eFour = new ArrayList<>();
    List<int[]> eBlockedFour = new ArrayList<>();
    List<int[]> eDoubleThree = new ArrayList<>();
    List<int[]> eThree = new ArrayList<>();
    List<int[]> eTwo = new ArrayList<>();
    ArrayList<int[]> result = new ArrayList<>();

    Map<String, Integer> standards = Board.STANDARDS;

    for (int row = 0; row < 15; row++) {
      for (int column = 0; column < 15; column++) {
        if (board.getBoard()[row][column] != EMPTY)
          continue;
        if (hasNeighbor(row, column, neighborDist, 1)) {
//          int aiScore = board.scorePoint(row, column, player);
          int aiScore = 0;
//          int humanScore = board.scorePoint(row, column, (player == WHITE) ? BLACK : WHITE);
          int humanScore = 0;
          for (int i = 1; i < 5; i++) {
            aiScore += board.pointScoreCache.get(player)[i][row][column];
            humanScore += board.pointScoreCache.get(player == BLACK ? WHITE : BLACK)[i][row][column];
          }

          if (aiScore >= standards.get("5+")) {
            five.add(0, new int[]{row, column});
          } else if (humanScore >= standards.get("5+"))
            five.add(new int[]{row, column});
          else if (aiScore >= standards.get("4+"))
            four.add(0, new int[]{row, column});
          else if (humanScore >= standards.get("4+"))
            eFour.add(new int[]{row, column});
          else if (aiScore >= standards.get("4-"))
            blockedFour.add(0, new int[]{row, column});
          else if (humanScore >= standards.get("4-"))
            eBlockedFour.add(new int[]{row, column});
          else if (aiScore >= standards.get("3+") * 2)
            doubleThree.add(0, new int[]{row, column});
          else if (humanScore >= standards.get("3+") * 2)
            eDoubleThree.add(new int[]{row, column});
          else if (aiScore >= standards.get("3+"))
            three.add(0, new int[]{row, column});
          else if (humanScore >= standards.get("3+"))
            eThree.add(new int[]{row, column});
          else if (aiScore >= standards.get("2+"))
            two.add(0, new int[]{row, column});
          else if (humanScore >= standards.get("2+"))
            eTwo.add(new int[]{row, column});
          else
            neighbor.add(new int[]{row, column});
        }
      }
    }

    if (five.size() > 0) {
      Collections.shuffle(five);
      return five;
    }

    if (four.size() > 0) {
      Collections.shuffle(four);
      return four;
    }
    if (eFour.size() > 0) {
      Collections.shuffle(eFour);
      return eFour;
    }

    if (eFour.size() > 0 && blockedFour.size() == 0) {
      Collections.shuffle(eFour);
      return eFour;
    }
    if (four.size() > 0 && eBlockedFour.size() == 0) {
      Collections.shuffle(four);
      return four;
    }

    Collections.shuffle(eFour);
    Collections.shuffle(four);
    Collections.shuffle(blockedFour);
    Collections.shuffle(eBlockedFour);
    ArrayList<int[]> totalFour = new ArrayList<>(four);
    totalFour.addAll(eFour);
    ArrayList<int[]> totalBlockFour = new ArrayList<>(blockedFour);
    totalBlockFour.addAll(eBlockedFour);
    if (totalFour.size() > 0) {
      totalFour.addAll(totalBlockFour);
      return totalFour;
    }

    Collections.shuffle(doubleThree);
    Collections.shuffle(eDoubleThree);
    Collections.shuffle(three);
    Collections.shuffle(eThree);
    result.addAll(doubleThree);
    result.addAll(eDoubleThree);
    result.addAll(blockedFour);
    result.addAll(eBlockedFour);
    result.addAll(three);
    result.addAll(eThree);

    if (doubleThree.size() > 0 || eDoubleThree.size() > 0)
      return result;

    ArrayList<int[]> totalTwo = new ArrayList<>();
    Collections.shuffle(two);
    Collections.shuffle(eTwo);
    totalTwo.addAll(two);
    totalTwo.addAll(eTwo);
    if (totalTwo.size() > 0)
      result.addAll(totalTwo);
    else {
      Collections.shuffle(neighbor);
      result.addAll(neighbor);
    }

    if (result.size() > 20)
      return result.subList(0, 20);
    else
      return result;
  }

  private boolean hasNeighbor(int row, int column, int neighborDist, int count) {
    for (int i = (row - neighborDist >= 0 ? row - neighborDist : 0);
         i < (row + neighborDist <= 14 ? row + neighborDist : 14);
         i++) {
      for (int j = (column - neighborDist >= 0 ? column - neighborDist : 0);
           j < (column + neighborDist <= 14 ? column + neighborDist : 14);
           j++) {
        if (board.getBoard()[i][j] != EMPTY) {
          if (--count == 0)
            return true;
        }
      }
    }
    return false;
  }


  /**
   * Iterative deepening
   *
   * @param depth the depth of search
   * @return the point ai choose
   */
  public int[] iterativeDeepening(int depth) {
    if (depth == 0) {
      int count = board.count();
      depth = 4;
      if (count >= 10)
        depth = 6;
      if (count >= 14)
        depth = 8;
    }

    int[] aiChess = new int[0];
    for (int i = 2; i <= depth; i += 2) {
      aiChess = minMaxSearch(i, false);
      board.setChess(aiChess[0], aiChess[1], aiNum);
      if (board.scoreBoard(aiNum, weight) >= Board.STANDARDS.get("4+")) {
        board.setChess(aiChess[0], aiChess[1], Board.EMPTY);
        return aiChess;
      }
      board.setChess(aiChess[0], aiChess[1], Board.EMPTY);
    }
    return aiChess;
  }


  /**
   * Min max search
   *
   * @return the point ai choose
   */
  public int[] minMaxSearch() {
    return minMaxSearch(5, false);
  }

  /**
   * Min max search
   *
   * @param deep  the depth of search
   * @param print whether print intermediate results
   * @return the point ai choose
   */
  int[] minMaxSearch(int deep, boolean print) {
//    If the board is empty
    boolean find = false;
    for (int[] row : board.getBoard()) {
      for (int i : row) {
        if (i != EMPTY) {
          find = true;
          break;
        }
      }
      if (find) {
        break;
      }
    }
    if (!find) {
      return new int[]{7, 7};
    }

    int maxV = -999999999;
    List<int[]> points = generatePossiblePoints(aiNum);
    List<int[]> candidates = new ArrayList<>();
    for (int i = 0; i < points.size(); i++) {
      int[] point = points.get(i);
      if (print)
        System.out.printf("%d / %d\n", i + 1, points.size());
      board.setChess(point[0], point[1], aiNum);
      int curV = minSearch(
              deep,
              -999999999,
              999999999,
              board
      );
      if (curV == maxV) {
        candidates.add(point);
      } else if (curV > maxV) {
        maxV = curV;
        candidates.clear();
        candidates.add(point);
      }
      board.setChess(point[0], point[1], Board.EMPTY);

    }
    Collections.shuffle(candidates);
    return candidates.get(0);
  }

  private int minSearch(int deep, int alpha, int beta, Board board) {
    Timer.set("minSearch");
    int score = board.scoreBoard(aiNum == WHITE ? BLACK : WHITE, weight);
    if (deep < 0 || board.isEnd() != CONTINUE)
      return score;

    List<int[]> points = generatePossiblePoints(aiNum == WHITE ? BLACK : WHITE);
    for (int[] currentPoint : points) {
      board.setChess(currentPoint[0], currentPoint[1], (aiNum == BLACK) ? WHITE : BLACK);
      int currentValue = maxSearch(deep - 1, alpha, beta, board);
      board.setChess(currentPoint[0], currentPoint[1], Board.EMPTY);
      beta = Math.min(beta, currentValue);
//      Prune
      if (beta < alpha)
        break;
    }
    return beta;
  }

  private int maxSearch(int deep, int alpha, int beta, Board board) {
    Timer.set("maxSearch");
    int score = board.scoreBoard(aiNum, weight);
    if (deep < 0 || board.isEnd() != CONTINUE) {
      return score;
    }

    List<int[]> points = generatePossiblePoints(aiNum);
    for (int[] currentPoint : points) {
      board.setChess(currentPoint[0], currentPoint[1], aiNum);
      int currentValue = minSearch(deep - 1, alpha, beta, board);
      board.setChess(currentPoint[0], currentPoint[1], Board.EMPTY);
      alpha = Math.max(alpha, currentValue);
//      Prune
      if (beta < alpha)
        break;
    }
    return alpha;
  }

  private String formatBoard(int[][] board) {
    StringBuilder out = new StringBuilder();
    for (int[] line : board) {
      for (int n : line) {
        out.append((n == EMPTY) ? "_ " : ((n == BLACK) ? "●" : "○")).append(" ");
      }
      out.append("\n");
    }
    return String.valueOf(out);
  }
}
