import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * The AI class.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class AI {
	/**
	 * The constant EMPTY.
	 */
	private static final short EMPTY = 10000;
	/**
	 * The constant BLACK.
	 */
	private static final short BLACK = 10001;
	/**
	 * The constant WHITE.
	 */
	private static final short WHITE = 10002;
	/**
	 * The constant CONTINUE.
	 */
	private static final int CONTINUE = 10004;
	private static double weight;
	/**
	 * 所用的棋盘类
	 */
	Board board;
	/**
	 * AI 的玩家角色 (BLACK or WHITE)
	 */
	short aiNum;


	/**
	 * 构造器
	 *
	 * @param board  棋盘类
	 * @param aiNum  AI 玩家角色
	 * @param weight 计算敌我得分时的权重。
	 *               权重越小，越偏向于防守。
	 *               Prediction: 0 < weight <= 1
	 */
	public AI(Board board, short aiNum, double weight) {
		this.board = board;
		if (aiNum != BLACK && aiNum != WHITE) {
			throw new ValueOutOfRangeException();
		}
		this.aiNum = aiNum;
		if (weight < 0 || weight > 2) {
			throw new ValueOutOfRangeException();
		}
		AI.weight = weight;
	}

	/**
	 * The entry point of application.
	 * 这里只是在测试一个被遗弃的功能。。。
	 *
	 * @param args the input arguments
	 */
	public static void main(String[] args) {
		AI ai = new AI(new Board(), WHITE, 1);
		File file = new File("boardCache.cache");
		try {
			//noinspection ResultOfMethodCallIgnored
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		ai.initialize(6, false);

		try (FileOutputStream fileOutputStream = new FileOutputStream("boardCache.cache")) {
			ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
			outputStream.writeObject(ai.board.boardScoreCache);
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialize board score cache to speed up calculation in first few steps
	 * **现在已经没用了，因为为了节省内存 boardScoreCache 每次计算时都会清零
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
				board.boardScoreCache = (Map<Integer, Board.Score>) inputStream.readObject();
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
		minMaxSearch(deep, new ArrayList<>());
		this.board.reset();
	}


	/**
	 * 生成候选点以供计算
	 *
	 * @param player the player number
	 * @return List contain all valid points in order
	 */
	@SuppressWarnings({"ConstantConditions", "Duplicates"})
	public List<int[]> generatePossiblePoints(short player) {
		// Timer.startRecord("generatePossiblePoints");
		List<int[]> vcxResult = vcx();
		if (!vcxResult.isEmpty()) return vcxResult;

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
				if (hasNeighbor(row, column, (board.chessCount < 6) ? 1 : 2, (board.chessCount < 6) ? 1 : 2)) {
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

//		以下所有的 shuffle 都是为了确保每次下子的随机性
		if (five.size() > 0) {
			Collections.shuffle(five);
			// Timer.endRecord("generatePossiblePoints");
			return five;
		}

		if (four.size() > 0) {
			Collections.shuffle(four);
			// Timer.endRecord("generatePossiblePoints");
			return four;
		}
		if (eFour.size() > 0) {
			Collections.shuffle(eFour);
			// Timer.endRecord("generatePossiblePoints");
			return eFour;
		}

		if (eFour.size() > 0 && blockedFour.size() == 0) {
			Collections.shuffle(eFour);
			// Timer.endRecord("generatePossiblePoints");
			return eFour;
		}
		if (four.size() > 0 && eBlockedFour.size() == 0) {
			Collections.shuffle(four);
			// Timer.endRecord("generatePossiblePoints");
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
			// Timer.endRecord("generatePossiblePoints");
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

		if (doubleThree.size() > 0 || eDoubleThree.size() > 0) {
			// Timer.endRecord("generatePossiblePoints");
			return result;
		}

		Collections.shuffle(two);
		Collections.shuffle(eTwo);
		if (two.size() > 0 || eTwo.size() > 0) {
			result.addAll(two);
			result.addAll(eTwo);
		} else {
			Collections.shuffle(neighbor);
			result.addAll(neighbor);
		}

		if (result.size() > 20) {
			// Timer.endRecord("generatePossiblePoints");

			return result.subList(0, 20);
		} else {
			// Timer.endRecord("generatePossiblePoints");
			return result;
		}
	}

	/**
	 * 判断一个点是否有邻居
	 *
	 * @param row          行号
	 * @param column       列数
	 * @param neighborDist 最大检测邻居的范围
	 * @param count        至少满足有几个邻居
	 * @return 是否有邻居
	 */
	private boolean hasNeighbor(int row, int column, int neighborDist, int count) {
		// Timer.startRecord("hasNeighbor");
		for (int i = (row - neighborDist >= 0 ? row - neighborDist : 0);
		     i < (row + neighborDist <= 14 ? row + neighborDist : 14);
		     i++) {
			for (int j = (column - neighborDist >= 0 ? column - neighborDist : 0);
			     j < (column + neighborDist <= 14 ? column + neighborDist : 14);
			     j++) {
				if (board.getBoard()[i][j] != EMPTY) {
					if (--count == 0) {
						// Timer.endRecord("hasNeighbor");
						return true;
					}
				}
			}
		}
		// Timer.endRecord("hasNeighbor");
		return false;
	}


	/**
	 * 迭代加深包装函数
	 * 已经基本上不用迭代加深，因为在 DFS 时已经根据搜索深度进行加权
	 *
	 * @param depth 搜索深度
	 *              若为0则自动判断
	 * @param iter  是否进行迭代加深
	 * @return AI 落子点
	 */
	public int[] iterativeDeepening(int depth, boolean iter) {
//		根据棋盘上棋子数自动生成相应搜索深度
		if (depth == 0) {
			int count = board.count();
			depth = 5;
			if (count >= 6) depth = 7;
//			if (count >= 12) depth = 9;
		}

		List<int[]> candidates = new ArrayList<>();
		if (!iter) {
			minMaxSearch(depth, candidates);
			return candidates.get(0);
		}

		boolean goodSituation = false;
		for (int i = 2; i <= depth; i += 2) {
			int result = minMaxSearch(i, candidates);
			if (result >= Board.STANDARDS.get("4+")) return candidates.get(0);
			goodSituation = result > 0;
		}
		return goodSituation ? candidates.get(0) : candidates.get(candidates.size() - 1);
	}

	/**
	 * 极大极小值搜索
	 *
	 * @param depth   搜索深度
	 * @param outcome 储存候选点的列表
	 * @return 当前点的分数
	 */
	int minMaxSearch(int depth, List<int[]> outcome) {
		board.boardScoreCache.clear();
		if (board.count() == 0) {
			outcome.add(new int[]{7, 7});
			return 0;
		}

		int maxV = Integer.MIN_VALUE;
		List<int[]> points = generatePossiblePoints(aiNum);
//		若候选点太多，则只计算前十个
		points = points.size() > 10 ? points.subList(0, 10) : points;
		if (points.size() == 1) {
			outcome.add(points.get(0));
			return 0;
		}
		List<int[]> candidates = new ArrayList<>();
		int c = 1;
		System.out.print(points.size() + ": ");
		for (int[] point : points) {
			System.out.printf("%d ", c++);
			board.setChess(point[0], point[1], aiNum);
			int curV = (int) minSearch(
							depth,
							Integer.MIN_VALUE,
							Integer.MAX_VALUE
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
		System.out.println();
		Collections.shuffle(candidates);
		outcome.add(candidates.get(0));
		return maxV;
	}

	/**
	 * 最小值搜索
	 * 在模拟敌方下棋时，要搜索最小值
	 *
	 * @param deep  当前深度
	 * @param alpha 最大值
	 * @param beta  最小值
	 * @return 最小分数
	 */
	double minSearch(int deep, double alpha, double beta) {
		// Timer.startRecord("minSearch");
		int score = board.scoreBoard(aiNum == WHITE ? BLACK : WHITE, weight);
//		若到达底层或胜负已分，则直接返回
		if (deep < 0 || board.isEnd() != CONTINUE) {
			// Timer.endRecord("minSearch");
			return score;
		}

		List<int[]> points = generatePossiblePoints(aiNum == WHITE ? BLACK : WHITE);
		//		若候选点太多，则只计算前十个
		points = points.size() > 10 ? points.subList(0, 10) : points;
		for (int[] currentPoint : points) {
			board.setChess(currentPoint[0], currentPoint[1], (aiNum == BLACK) ? WHITE : BLACK);
//			计算得分并根据深度加权
			double currentValue = maxSearch(deep - 1, alpha, beta) * (1 + deep / 10.);
			board.setChess(currentPoint[0], currentPoint[1], Board.EMPTY);
			beta = Math.min(beta, currentValue);
//      剪枝
			if (beta < alpha)
				break;
		}
		// Timer.endRecord("minSearch");
		return beta;
	}

	/**
	 * 最大值搜索
	 * 在模拟AI方落子时，要搜索最大值
	 * 最大值搜索其实和最小值搜索大同小异
	 *
	 * @param deep  当前深度
	 * @param alpha 最大值
	 * @param beta  最小值
	 * @return 最大分数
	 */
	private double maxSearch(int deep, double alpha, double beta) {
		// Timer.startRecord("maxSearch");
		int score = board.scoreBoard(aiNum, weight);
		if (deep < 0 || board.isEnd() != CONTINUE) {
			// Timer.endRecord("maxSearch");
			return score;
		}

		List<int[]> points = generatePossiblePoints(aiNum);
		points = points.size() > 10 ? points.subList(0, 10) : points;
		for (int[] currentPoint : points) {
			board.setChess(currentPoint[0], currentPoint[1], aiNum);
			double currentValue = minSearch(deep - 1, alpha, beta) * (1 + deep / 10.);
			board.setChess(currentPoint[0], currentPoint[1], Board.EMPTY);
			alpha = Math.max(alpha, currentValue);
//      Prune
			if (beta < alpha)
				break;
		}
		// Timer.endRecord("maxSearch");
		return alpha;
	}

	/**
	 * 算杀
	 * 尚未决定是否需要实现
	 *
	 * @return 候选名单
	 */
	public List<int[]> vcx() {
		List<int[]> outcome = new ArrayList<>();
//		TODO


		return outcome;
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
