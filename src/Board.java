import java.util.*;

/**
 * 主棋盘类
 * <p>
 * The Main Board.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Board {
	/**
	 * 常量：表示一个空位
	 * <p>
	 * The constant represents EMPTY position.
	 */
	public static final short EMPTY = 10000;
	/**
	 * 常量：表示黑子
	 * <p>
	 * The constant represents BLACK chess.
	 */
	public static final short BLACK = 10001;
	/**
	 * 常量：表示白子
	 * <p>
	 * The constant represent WHITE chess.
	 */
	public static final short WHITE = 10002;
	/**
	 * 常量：表示平局（游戏状态）
	 * 在判断棋盘输赢时会用，若双方平局则返回这个常量
	 * <p>
	 * The constant represent the TIE condition.
	 * It is used when evaluating winning condition
	 * when all the places on the board is filled and no player win.
	 */
	public static final int TIE = 10003;
	/**
	 * 常量：继续游戏（游戏状态）
	 * 在判断棋盘输赢时会用，若没有玩家获胜或平局（游戏继续），则返回这个常量
	 * <p>
	 * The constant represent the CONTINUE condition.
	 * It is used when evaluating winning condition when there's no player win.
	 * (the game continue).
	 */
	public static final int CONTINUE = 10004;
	/**
	 * 常量：储存所有的棋形评分标准
	 * 例如："4+"表示活四棋形，其所对应的分数是100000
	 * 用法：STANDARDS.get(String chessType) -> int correspondScore
	 * <p>
	 * The constant STANDARDS.
	 * It stores the corresponding points for each types of chess.
	 * eg. 4+ represent continue 4 chess without being blocked, and its score is 100000
	 * Usage: STANDARDS.get(String chessType) -> int correspondScore
	 */
	public static final Map<String, Integer> STANDARDS = Map.of(
					"5+", 10000000,
					"4+", 100000,
					"3+", 1000,
					"2+", 100,
					"1+", 10,
					"4-", 1000,
					"3-", 100,
					"2-", 10,
					"1-", 1
	);
	/**
	 * 棋盘得分缓存,用于加速计算棋盘估分
	 * 每一次计算棋盘的得分都会缓存在此，棋盘以哈希码表示
	 * 键是棋盘的哈希码；
	 * 值是一个Score类，其中储存单点的分和双方总分
	 * 用法：boardScoreCache.get(int boardHashCode) -> Score object
	 * <p>
	 * Cache of scores of different boards
	 * Each time the score of the board is cached here (when newly calculated)
	 * The board is represented by its hash code
	 * Key: Hashcode of board
	 * Value: A score object storing the pointScoreCache and total score of both players
	 * Usage: boardScoreCache.get(int boardHashCode) -> Score object
	 */
	Map<Integer, Score> boardScoreCache = new HashMap<>();
	/**
	 * 单点得分缓存，用于加速单点评分
	 * 当附近的位置发生变动时（新下棋，悔棋），会有函数更新其中的得分
	 * 每一个位置分别储存了四个方向上单独的得分
	 * 水平：1
	 * 竖直：2
	 * 两个对角：3,4
	 * 用法：pointScoreCache.get(short player)[int direction][int row][int column] -> int score
	 */
	Map<Short, int[][][]> pointScoreCache = Map.of(BLACK, new int[5][15][15],
					WHITE, new int[5][15][15]);
	/**
	 * 当前棋盘上棋子总数
	 */
	int chessCount = 0;
	/**
	 * 主要棋盘
	 * 注意：所有的下棋操作必须由setChess方法完成，否则不会更新单点分数缓存
	 */
	private short[][] board;
	/**
	 * 记录下棋步骤，用于悔棋
	 */
	private ArrayList<int[]> operations = new ArrayList<>();
	/**
	 * 记录当前下棋玩家，默认黑棋开始
	 */
	private short playerTurn = BLACK;

	/**
	 * 默认构造器
	 */
	public Board() {
		reset();
	}

	/**
	 * 带传入数据的构造器
	 *
	 * @param data       棋盘（必须是二维数组形式）
	 * @param playerTurn 当前玩家
	 */
	public Board(short[][] data, short playerTurn) {
		// 传入参数格式
		if (data.length != 15 || data[0].length != 15) {
			System.out.println("Illegal board size");
			throw new ArrayIndexOutOfBoundsException();
		}
		for (short[] i : data) {
			for (int j : i) {
				if (j != EMPTY && j != BLACK && j != WHITE) {
					throw new ValueOutOfRangeException();
				}
			}
		}

		this.board = data.clone();
		if (playerTurn != WHITE && playerTurn != BLACK) {
			throw new ValueOutOfRangeException();
		}
		this.playerTurn = playerTurn;

		// 更新棋盘上所有点的分数
		for (int i = 0; i < 15; i++) {
			for (int j = 0; j < 15; j++) {
				updateScore(i, j);
			}
		}
	}

	/**
	 * The entry point of application.
	 *
	 * @param args the input arguments
	 */
	public static void main(String[] args) {
		Board board = new Board();
		board.setChess(7, 5, BLACK);
		board.setChess(7, 6, WHITE);
		board.setChess(7, 7, WHITE);
		board.setChess(7, 8, WHITE);
		board.setChess(7, 10, WHITE);
		board.setChess(7, 11, BLACK);
		System.out.println(board);
		board.reset();
		System.out.println(board);
	}

	/**
	 * Gets player turn.
	 *
	 * @return the player turn
	 */
	public int getPlayerTurn() {
		return playerTurn;
	}

	/**
	 * Gets board.
	 *
	 * @return the board
	 */
	public short[][] getBoard() {
		return board;
	}

	/**
	 * 下棋，不记录当前操作
	 * 这个方法多是在ai计算和调试时用，真正下棋需要用另一个setChess方法
	 *
	 * @param row    行数
	 * @param column 列数
	 * @param player 下棋玩家（BLACK, WHITE, EMPTY）
	 * @throws ValueOutOfRangeException 三个参数中任意一个超出范围
	 */
	public void setChess(int row, int column, short player) throws ValueOutOfRangeException {
		if (row >= 15 || column >= 15 || row < 0 || column < 0) {
			throw new ValueOutOfRangeException();
		}
		if (player != BLACK && player != WHITE && player != EMPTY) {
			throw new ValueOutOfRangeException();
		}
		chessCount += (player == EMPTY) ? -1 : 1;
		board[row][column] = player;
		updateScore(row, column);
	}

	/**
	 * 下棋
	 *
	 * @param row    行数
	 * @param column 列数
	 * @param record 是否记录
	 * @throws ValueOutOfRangeException 传入参数超出范围
	 * @throws RedundantChessException  所下位置已经有棋子
	 */
	public void setChess(int row, int column, boolean record) throws ValueOutOfRangeException, RedundantChessException {
		if (row >= 15 || column >= 15 || row < 0 || column < 0) {
			throw new ValueOutOfRangeException();
		}
		if (board[row][column] != EMPTY) {
			throw new RedundantChessException();
		}

		chessCount++;
		board[row][column] = playerTurn;
		updateScore(row, column);

		// 记录操作
		if (record) this.operations.add(new int[]{row, column});

		// 转换玩家
		playerTurn = (playerTurn == BLACK) ? WHITE : BLACK;
	}

	/**
	 * 从单点开始更新分数 由于一个点棋子的变化会同时影响到周围的多个点的分数，所以也要同时计算这些点
	 *
	 * @param row    行号
	 * @param column 列数
	 */
	public void updateScore(int row, int column) {
		// Timer.startRecord("Update");

//		如果当前棋盘已经在缓存中，则直接将 pointScoreCache 更改为缓存中的值
		if (boardScoreCache.containsKey(Arrays.hashCode(board))) {
			pointScoreCache = boardScoreCache.get(Arrays.hashCode(board)).pointScoreCache;
			return;
		}

		// 更新分数的范围
		final int range = 6;
		// 横向查找
		for (int i = -range; i <= range; i++) {
			int y = column + i;
			if (row < 0 || row > 14 || y < 0 || y > 14)
				continue;
			// 这里计算的点只更新横向分数
			update(row, y, 1);
		}
		// 纵向查找
		for (int i = -range; i <= range; i++) {
			int x = row + i;
			if (x < 0 || x > 14 || column < 0 || column > 14)
				continue;
			// 同理只计算纵向
			update(x, column, 2);
		}
		// 斜向查找（左上->右下）
		for (int i = -range; i <= range; i++) {
			int x = row + i, y = column + i;
			if (x < 0 || x > 14 || y < 0 || y > 14)
				continue;
			// 同上，只记录斜向
			update(x, y, 3);
		}
		// 斜向查找（右上->左下）
		for (int i = -range; i <= range; i++) {
			int x = row + i, y = column - i;
			if (x < 0 || x > 14 || y < 0 || y > 14)
				continue;
			// 同上，只记录斜向
			update(x, y, 4);
		}
		// Timer.endRecord("Update");
	}

	/**
	 * 更新单点分数
	 *
	 * @param row       行数
	 * @param column    列数
	 * @param direction 更新分数的方向（1, 2, 3, 4 分别是横纵斜线）
	 */
	@SuppressWarnings("Duplicates")
	private void update(int row, int column, int direction) {
		int player = board[row][column];
		// 新下黑子或变为空位都需要计算黑棋
		if (player == EMPTY || player == BLACK) {
			int score = scorePoint(row, column, BLACK, direction);
			// 保存到缓存中
			pointScoreCache.get(BLACK)[direction][row][column] = score;
		} else
			pointScoreCache.get(BLACK)[direction][row][column] = 0; /* 若为白棋黑棋分数清零 */
		// 新下白子或变为空位都需要计算白子
		if (player == EMPTY || player == WHITE) {
			int score = scorePoint(row, column, WHITE, direction);
			// 缓存
			pointScoreCache.get(WHITE)[direction][row][column] = score;
		} else
			pointScoreCache.get(WHITE)[direction][row][column] = 0; /* 若为黑棋白棋分数清零 */
	}

	/**
	 * 给整个棋盘打分（包装函数）
	 *
	 * @param player 要打分的棋子
	 * @param weight 打分权重（权重越大，算分越偏向于防守） 默认情况是建议 weight = 1
	 * @return 整个棋盘的分数 int
	 */
	public int scoreBoard(short player, double weight) {
		// Timer.startRecord("scoreBoard");

		// 计算当前棋盘的哈希码
		int hashCode = Arrays.deepHashCode(board);
		// 若缓存中有当前棋盘则直接返回分数
		Score b = boardScoreCache.get(hashCode);
		if (b != null && b.getScore(player) != -1)
			return b.getScore(player);
		int result = _scoreBoard(player, weight);
		// 将结果添加到缓存
		if (b == null) {
			b = new Score(new HashMap<>(pointScoreCache));
			boardScoreCache.put(hashCode, b);
		}
		b.setScore(player, result);

		// Timer.endRecord("scoreBoard");
		return result;
	}

	/**
	 * 计算整个棋盘的分数（真正计算）
	 *
	 * @param player 要打分的棋子
	 * @param weight 权重，同上
	 * @return 棋盘分数
	 */
	private int _scoreBoard(short player, double weight) {
		int selfScore = 0, enemyScore = 0;
		// 分别计算每个点的分数
		for (int i = 0; i < 15; i++) {
			for (int j = 0; j < 15; j++) {
				if (board[i][j] == player)
					// 注意：一共有四个方向，所以一个点的分数不同方向都要加
					for (int direction = 1; direction < 5; direction++)
						// 直接从单点分数缓存中取值
						selfScore += pointScoreCache.get(player)[direction][i][j];
				else if (board[i][j] != EMPTY)
					for (int direction = 1; direction < 5; direction++)
						// 直接从单点分数缓存中取值
						enemyScore += pointScoreCache.get(player == WHITE ? BLACK : WHITE)[direction][i][j];
			}
		}
		return (int) (selfScore - weight * enemyScore);
	}

	/**
	 * 单点评分（计算）
	 *
	 * @param row       行号
	 * @param column    列数
	 * @param player    要评分的棋子
	 * @param direction 评分方向（1为横向、2为竖直、3为左上->右下、4为右上->左下）
	 * @return 单点得分 int
	 */
	@SuppressWarnings("Duplicates")
	public int scorePoint(int row, int column, short player, int... direction) {
		// Timer.startRecord("scorePoint");
		int emptyPosition, count, block;
		int score = 0;
		boolean horizontal = false, vertical = false, diagonal1 = false, diagonal2 = false;
		for (int i : direction) {
			switch (i) {
				case 1:
					horizontal = true;
					break;
				case 2:
					vertical = true;
					break;
				case 3:
					diagonal1 = true;
					break;
				case 4:
					diagonal2 = true;
					break;
			}
		}

		// Horizontal
		if (horizontal) {
			emptyPosition = -1;
			count = 1;
			block = 0;
			// To the right
			for (int i = column + 1; true; i++) {
				if (i > 14) {
					// Reach the end of chessboard
					block++;
					break;
				}
				int chess = board[row][i];
				// If the place is empty
				if (chess == EMPTY) {
					if (emptyPosition == -1 && /* No empty has not been recorded */
									i < 14 && /* Have at least 1 block to the boarder */
									board[row][i + 1] == player /* Has chess after empty space */
					)
						emptyPosition = count;
					else
						break;
				} else if (chess == player)
					count++;
				else {
					block++;
					break;
				}
			}
			// To the left
			for (int i = column - 1; true; i--) {
				if (i < 0) {
					block++;
					break;
				}
				int chess = board[row][i];
				if (chess == EMPTY) {
					if (emptyPosition == -1 && i > 0 && board[row][i - 1] == player)
						emptyPosition = 0;
					else
						break;
				} else if (chess == player) {
					count++;
					// Here, if there are still chess on the left side of empty space,
					// then the empty position will increase by 1
					emptyPosition += (emptyPosition == -1) ? 0 : 1;
				} else {
					block++;
					break;
				}
			}
			score += calculateScore(emptyPosition, count, block);
		}

		// Vertical
		if (vertical) {
			emptyPosition = -1;
			count = 1;
			block = 0;
			// Downward
			for (int i = row + 1; true; i++) {
				if (i > 14) {
					block++;
					break;
				}
				int chess = board[i][column];
				if (chess == EMPTY) {
					if (emptyPosition == -1 && i < 14 && board[i + 1][column] == player)
						emptyPosition = count;
					else
						break;
				} else if (chess == player)
					count++;
				else {
					block++;
					break;
				}
			}
			// Upward
			for (int i = row - 1; true; i--) {
				if (i < 0) {
					block++;
					break;
				}
				int chess = board[i][column];
				if (chess == EMPTY) {
					if (emptyPosition == -1 && i > 0 && board[i - 1][column] == player)
						emptyPosition = 0;
					else
						break;
				} else if (chess == player) {
					count++;
				} else {
					block++;
					break;
				}
			}
			score += calculateScore(emptyPosition, count, block);
		}

		// Upper-left to lower-right
		if (diagonal1) {
			emptyPosition = -1;
			count = 1;
			block = 0;
			// Toward right
			for (int i = 1; true; i++) {
				int x = row + i, y = column + i;
				if (x > 14 || y > 14) {
					block++;
					break;
				}
				int chess = board[x][y];
				if (chess == EMPTY) {
					if (emptyPosition == -1 && x < 14 && y < 14 && board[x + 1][y + 1] == player)
						emptyPosition = count;
					else
						break;
				} else if (chess == player)
					count++;
				else {
					block++;
					break;
				}
			}
			// Toward left
			for (int i = 1; true; i++) {
				int x = row - i;
				int y = column - i;
				if (x < 0 || y < 0) {
					block++;
					break;
				}
				int chess = board[x][y];
				if (chess == EMPTY) {
					if (emptyPosition == -1 && x > 0 && y > 0 && board[x - 1][y - 1] == player)
						emptyPosition = 0;
					else
						break;
				} else if (chess == player) {
					count++;
					emptyPosition += (emptyPosition == -1) ? 0 : 1;
				} else {
					block++;
					break;
				}
			}
			score += calculateScore(emptyPosition, count, block);
		}

		// Upper-right to lower left
		if (diagonal2) {
			emptyPosition = -1;
			count = 1;
			block = 0;
			// Downward
			for (int i = 1; true; i++) {
				int x = row + i, y = column - i;
				if (x < 0 || x > 14 || y < 0 || y > 14) {
					block++;
					break;
				}
				int chess = board[x][y];
				if (chess == EMPTY) {
					if (emptyPosition == -1 && x < 14 && y > 0 && board[x + 1][y - 1] == player)
						emptyPosition = count;
					else
						break;
				} else if (chess == player)
					count++;
				else {
					block++;
					break;
				}
			}
			// Upward
			for (int i = 1; true; i++) {
				int x = row - i;
				int y = column + i;
				if (x < 0 || x > 14 || y < 0 || y > 14) {
					block++;
					break;
				}
				int chess = board[x][y];
				if (chess == EMPTY) {
					if (emptyPosition == -1 && x > 0 && y < 14 && board[x - 1][y + 1] == player)
						emptyPosition = 0;
					else
						break;
				} else if (chess == player) {
					count++;
					emptyPosition += (emptyPosition == -1) ? 0 : 1;
				} else {
					block++;
					break;
				}
			}
			score += calculateScore(emptyPosition, count, block);
		}

		// Timer.endRecord("scorePoint");
		return score;
	}

	/**
	 * 根据棋形（位置信息）计算具体得分
	 *
	 * @param emptyPosition 空位位置
	 * @param count         己方棋子
	 * @param block         阻挡棋子数量
	 * @return 具体分数
	 */
	@SuppressWarnings("Duplicates")
	private int calculateScore(int emptyPosition, int count, int block) {
		int five = Board.STANDARDS.get("5+"), four = Board.STANDARDS.get("4+"), three = Board.STANDARDS.get("3+"),
						two = Board.STANDARDS.get("2+"), one = Board.STANDARDS.get("1+"), blockFour = Board.STANDARDS.get("4-"),
						blockThree = Board.STANDARDS.get("3-"), blockTwo = Board.STANDARDS.get("2-"),
						blockOne = Board.STANDARDS.get("1-");
		// No empty space
		if (emptyPosition <= 0) {
			if (count >= 5)
				return five;
			if (block == 0) {
				switch (count) {
					case 1:
						return one;
					case 2:
						return two;
					case 3:
						return three;
					case 4:
						return four;
				}
			} else if (block == 1) {
				switch (count) {
					case 1:
						return blockOne;
					case 2:
						return blockTwo;
					case 3:
						return blockThree;
					case 4:
						return blockFour;
				}
			}
		} else if (emptyPosition == 1 || emptyPosition == count - 1) {
			// Empty on the first position
			if (count >= 6)
				return five;
			if (block == 0) {
				switch (count) {
					case 2:
						return two / 2;
					case 3:
						return three;
					case 4:
						return blockFour;
					case 5:
						return four;
				}
			} else if (block == 1) {
				switch (count) {
					case 2:
						return blockTwo;
					case 3:
						return blockThree;
					case 4:
						return blockFour;
					case 5:
						return blockFour;
				}
			}

		} else if (emptyPosition == 2 || emptyPosition == count - 2) {
			// Empty on the second position
			if (count >= 7)
				return five;
			if (block == 0) {
				switch (count) {
					case 3:
						return three;
					case 4:
					case 5:
						return blockFour;
					case 6:
						return four;
				}
			} else if (block == 1) {
				switch (count) {
					case 3:
						return blockThree;
					case 4:
					case 5:
						return blockFour;
					case 6:
						return four;
				}
			} else if (block == 2) {
				switch (count) {
					case 4:
					case 5:
					case 6:
						return blockFour;
				}
			}

		} else if (emptyPosition == 3 || emptyPosition == count - 3) {
			// Empty on the third position
			if (count >= 8)
				return five;
			if (block == 0) {
				switch (count) {
					case 4:
					case 5:
						return three;
					case 6:
						return blockFour;
					case 7:
						return four;
				}
			} else if (block == 1) {
				switch (count) {
					case 4:
					case 5:
					case 6:
						return blockFour;
					case 7:
						return four;
				}
			} else if (block == 2) {
				switch (count) {
					case 4:
					case 5:
					case 6:
					case 7:
						return blockFour;
				}
			}
		} else if (emptyPosition == 4 || emptyPosition == count - 4) {
			// Empty on the fourth position
			if (count > 9)
				return five;
			if (block == 0) {
				switch (count) {
					case 5:
					case 6:
					case 7:
					case 8:
						return four;
				}
			} else if (block == 1) {
				switch (count) {
					case 4:
					case 5:
					case 6:
					case 7:
						return blockFour;
					case 8:
						return four;
				}
			} else if (block == 2) {
				switch (count) {
					case 5:
					case 6:
					case 7:
					case 8:
						return blockFour;
				}
			}
		} else if (emptyPosition == 5 || emptyPosition == count - 5)
			return five;
		return 0;
	}

	/**
	 * 悔一步棋
	 *
	 * @return 最后一步棋的信息 {row, column, playerNumber}
	 * @throws ArrayIndexOutOfBoundsException 如果当前没有下棋
	 */
	public List<Integer> withdraw() throws ArrayIndexOutOfBoundsException {
		if (operations.isEmpty())
			throw new ArrayIndexOutOfBoundsException();
		List<Integer> list = new ArrayList<>();
		int[] last = operations.remove(operations.size() - 1);
		list.add(last[0]);
		list.add(last[1]);
		list.add((int) board[last[0]][last[1]]);
		board[last[0]][last[1]] = EMPTY;
		updateScore(last[0], last[1]);
		return list;
	}

	/**
	 * 重设棋盘
	 */
	public void reset() {
		board = new short[15][15];
		for (int i = 0; i < 15; i++) {
			for (int j = 0; j < 15; j++) {
				board[i][j] = EMPTY;
			}
		}
		playerTurn = BLACK;
	}

	/**
	 * 计算当前棋盘上的棋子数量
	 *
	 * @return 棋子数量 int
	 */
	public int count() {
		int num = 0;
		for (int i = 0; i < 15; i++) {
			for (int j = 0; j < 15; j++) {
				if (board[i][j] != EMPTY)
					num++;
			}
		}
		return chessCount = num;
	}

	/**
	 * 判断游戏是否结束
	 *
	 * @return 整数状态码 (BLACK, WHITE, TIE, CONTINUE)
	 */
	public int isEnd() {
		// Timer.startRecord("isEnd");
		boolean jump = false;
		for (short[] line : board) {
			for (int i : line) {
				if (i == EMPTY) {
					jump = true;
					break;
				}
			}
		}
		if (!jump) {
			// Timer.endRecord("isEnd");
			return TIE;
		}
		for (short[] line : splitBoard()) {
			int current = 0;
			int count = 0;
			for (int i : line) {
				if (i != current) {
					count = 1;
					current = i;
				} else {
					if (i == EMPTY)
						continue;
					if (++count == 5) {
						// Timer.endRecord("isEnd");
						return current;
					}
				}
			}
		}
		// Timer.endRecord("isEnd");
		return CONTINUE;
	}

	/**
	 * 判断游戏是否结束 这个重载方法只是判断某一个点的输赢
	 *
	 * @param row    行数
	 * @param column 列数
	 * @return 整数状态码 (WHITE, BLACK, CONTINUE)
	 */
	@SuppressWarnings("Duplicates")
	public int isEnd(int row, int column) {
		int startRow, startColumn, endRow, endColumn, count;
		int player = board[row][column];
		// Horizontal
		count = 0;
		startColumn = column - 4 >= 0 ? column - 4 : 0;
		endColumn = column + 4 <= 14 ? column + 4 : 14;
		for (int i = startColumn; i <= endColumn; i++) {
			if (board[row][i] == player) {
				count++;
			} else {
				count = 0;
			}
			if (count == 5) {
				return player;
			}
		}
		// Vertical
		count = 0;
		startRow = row - 4 >= 0 ? row - 4 : 0;
		endRow = row + 4 <= 14 ? row + 4 : 14;
		for (int i = startRow; i <= endRow; i++) {
			if (board[i][column] == player) {
				count++;
			} else {
				count = 0;
			}
			if (count == 5) {
				return player;
			}
		}
		// Diagonal 1
		int currentRow, currentColumn;
		count = 0;
		for (int i = 0; i < Math.min(endRow - startRow, endColumn - startColumn); i++) {
			currentRow = startRow + i;
			currentColumn = startColumn + i;
			if (board[currentRow][currentColumn] == player) {
				count++;
			} else {
				count = 0;
			}
			if (count == 5) {
				return player;
			}
		}
		// Diagonal 2
		count = 0;
		for (int i = 0; i < Math.min(endRow - startRow, endColumn - startColumn); i++) {
			currentRow = endRow - i;
			currentColumn = startColumn + i;
			if (board[currentRow][currentColumn] == player) {
				count++;
			} else {
				count = 0;
			}
			if (count == 5) {
				return player;
			}
		}
		return CONTINUE;
	}

	/**
	 * 分离棋盘 分别输出棋盘中每列、每行、和每个对角线
	 *
	 * @return 所有的行 、列、对角线
	 */
	public ArrayList<short[]> splitBoard() {
		ArrayList<short[]> out = new ArrayList<>();
		// Horizontal
		for (int i = 0; i < 15; i++)
			out.add(board[i]);
		// Vertical
		for (int i = 0; i < 15; i++) {
			short[] row = new short[15];
			for (int j = 0; j < 15; j++) {
				row[j] = board[j][i];
			}
			out.add(row);
		}
		// Diagonal 1
		for (int base = 0; base < 29; base++) {
			short[] line = new short[(base <= 14 ? base + 1 : 15) - (base <= 14 ? 0 : base - 14)];
			int p = 0;
			for (int x = (base <= 14 ? 0 : base - 14); x < (base <= 14 ? base + 1 : 15); x++) {
				line[p++] = board[x][base - x];
			}
			out.add(line);
		}
		// Diagonal 2
		for (int diff = -14; diff < 15; diff++) {
			short[] line = new short[(diff < 0 ? 15 : 15 - diff) - (diff < 0 ? Math.abs(diff) : 0)];
			int p = 0;
			for (int x = (diff < 0 ? Math.abs(diff) : 0); x < (diff < 0 ? 15 : 15 - diff); x++) {
				line[p++] = board[x][diff + x];
			}
			out.add(line);
		}
		return out;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder().append("    0  1  2  3  4  5  6  7  8  9  10 11 12 13 14\n");
		for (int i = 0; i < board.length; i++) {
			short[] x = board[i];
			str.append(i >= 10 ? "" : " ").append(i).append(" ");
			for (int y : x) {
				str.append(y == EMPTY ? " _" : (y == BLACK ? "●" : "○")).append(" ");
			}
			str.append("\n");
		}
		return String.valueOf(str);
	}

	/**
	 * Score 类
	 * 储存一个棋盘的相关分数
	 */
	class Score {
		/**
		 * 当前棋盘的 pointScoreCache，直接是原来的 clone
		 */
		Map<Short, int[][][]> pointScoreCache;
		private int blackScore, whiteScore;

		/**
		 * Instantiates a new Score.
		 *
		 * @param pointScoreCache the point score cache
		 */
		Score(Map<Short, int[][][]> pointScoreCache) {
			this.blackScore = this.whiteScore = -1;
			this.pointScoreCache = pointScoreCache;
		}

		/**
		 * Gets score.
		 *
		 * @param player the player number
		 * @return the score
		 */
		int getScore(short player) {
			return (player == BLACK) ? blackScore : whiteScore;
		}

		/**
		 * Sets score.
		 *
		 * @param player the player number
		 * @param score  the score
		 */
		void setScore(short player, int score) {
			if (player == BLACK) blackScore = score;
			else whiteScore = score;
		}
	}
}