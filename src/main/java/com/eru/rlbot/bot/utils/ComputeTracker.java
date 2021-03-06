package com.eru.rlbot.bot.utils;

import com.eru.rlbot.bot.common.Constants;
import com.google.common.base.Preconditions;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how long each control output takes to compute.
 */
public class ComputeTracker {

  private static final ConcurrentHashMap<Integer, ComputeTracker> MAP = new ConcurrentHashMap<>();

  private final int serialNumber;
  private final Queue<Double> times;

  private volatile StopWatch tickWatch;

  public ComputeTracker(int serialNumber) {
    this.serialNumber = serialNumber;
    this.times = new LinkedList<>();
  }

  /**
   * Start the tracker.
   */
  public static void init(int serialNumber) {
    get(serialNumber).track();
  }

  public static double stop(int serialNumber) {
    return get(serialNumber).stop();
  }

  public static double averageSeconds(int serialNumber) {
    return get(serialNumber).times.stream()
        .mapToDouble(i -> i)
        .average()
        .orElse(0);
  }

  private double stop() {
    Preconditions.checkState(tickWatch != null);
    double time = tickWatch.stop();
    times.add(time);
    if (times.size() > Constants.STEP_SIZE_COUNT) {
      times.remove();
    }
    tickWatch = null;
    return time;
  }

  private void track() {
    tickWatch = StopWatch.start("");
  }

  private static ComputeTracker get(int serialNumber) {
    return MAP.computeIfAbsent(serialNumber, ComputeTracker::new);
  }
}
