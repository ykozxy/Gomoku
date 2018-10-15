import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Timer class.
 */
public class Timer {
  private static final Map<String, Long> timeMap = new LinkedHashMap<>();
  private static final Map<String, Long> timeHappenCount = new LinkedHashMap<>();
  private static String lastMark = "start";
  private static long lastTime = System.nanoTime();

  /**
   * Set time mark.
   *
   * @param mark the mark
   */
  public static void set(int mark) {
    set("" + mark);
  }

  /**
   * Set time mark.
   *
   * @param mark the mark
   */
  static void set(String mark) {
    long thisTime = System.nanoTime();
    String key = lastMark + "->" + mark;
    Long lastSummary = timeMap.get(key);
    if (lastSummary == null)
      lastSummary = 0L;

    timeMap.put(key, System.nanoTime() - lastTime + lastSummary);
    Long lastCount = timeHappenCount.get(key);
    if (lastCount == null)
      lastCount = 0L;

    timeHappenCount.put(key, ++lastCount);
    lastTime = thisTime;
    lastMark = mark;
  }


  /**
   * Print out time usage.
   */
  static void print() {
    for (Map.Entry<String, Long> entry : timeMap.entrySet()) {
      System.out.println(
              String.format("%20s, Total times:%20s,  Repeat times:%20s, Avg times:%20s ", entry.getKey(),
                      entry.getValue() / 1000000000.0, timeHappenCount.get(entry.getKey()),
                      entry.getValue() / timeHappenCount.get(entry.getKey()) / 1000000000.0
              ));
    }
  }
}

