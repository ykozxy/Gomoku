import java.util.*;

class Timer {
  private static Map<String, long[]> record = new LinkedHashMap<>();
  private static Map<String, List<Long>> lastTime = new HashMap<>();
  static boolean valid = true;

  public static void main(String[] args) throws InterruptedException {
    startRecord("A");
    Thread.sleep(1000);
    endRecord("A");
    print();
  }

  static void startRecord(String flag) {
    if (!valid) return;
    if (!record.containsKey(flag))
      record.put(flag, new long[2]);
    List<Long> t;
    if (!lastTime.containsKey(flag)) {
      t = new ArrayList<>();
    } else {
      t = lastTime.get(flag);
    }
    t.add(System.nanoTime());
    lastTime.put(flag, t);
    record.get(flag)[1]++;
  }

  static void endRecord(String flag) {
    if (!valid) return;
    try {
      record.get(flag)[0] += System.nanoTime() - lastTime.get(flag).get(lastTime.get(flag).size() - 1);
    } catch (NullPointerException | IndexOutOfBoundsException e) {
      System.err.printf("No flag %s started!\n", flag);
    }
    lastTime.get(flag).remove(lastTime.get(flag).size() - 1);
  }

  static void print() {
    if (!valid) return;
    System.out.println("Time record for all flags");
    for (Map.Entry<String, long[]> each : record.entrySet()) {
      System.out.printf(
              "Flag \"%s\":\n  Total time: %.5fs\n  Total call: %d\n  Average time: %.10fs\n",
              each.getKey(),
              each.getValue()[0] / 1000000000.,
              each.getValue()[1],
              each.getValue()[0] / 1000000000. / each.getValue()[1]);
    }
  }

  static void reset() {
    record.clear();
    lastTime.clear();
  }
}
