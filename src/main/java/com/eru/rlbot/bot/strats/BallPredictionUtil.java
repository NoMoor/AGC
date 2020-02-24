package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.DllHelper;
import com.eru.rlbot.bot.common.Path;
import com.eru.rlbot.bot.common.Plan;
import com.eru.rlbot.common.StateLogger;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;

public class BallPredictionUtil {

  private static final Logger logger = LogManager.getLogger("BallPredictionUtil");

  private static HashMap<Integer, BallPredictionUtil> BOTS = new HashMap<>();

  private final int playerIndex;

  private BallPredictionUtil(int index) {
    this.playerIndex = index;
  }

  public static BallPredictionUtil forIndex(int index) {
    BOTS.computeIfAbsent(index, BallPredictionUtil::new);
    return BOTS.get(index);
  }

  public static BallPredictionUtil forCar(CarData car) {
    return forIndex(car.playerIndex);
  }

  private List<ExaminedBallData> examinedBallData = new LinkedList<>();

  public List<ExaminedBallData> getPredictions() {
    return examinedBallData;
  }

  public ExaminedBallData getFirstHittableLocation() {
    if (examinedBallData.isEmpty()) {
      return null;
    }

    Optional<ExaminedBallData> firstHittable = examinedBallData.stream()
        .filter(data -> data.isHittable().orElse(false))
        .findFirst();

    return firstHittable.orElse(null);
  }

  public static boolean refresh(DataPacket input) {
    return forCar(input.car).refreshInternal(input.ball);
  }

  private boolean refreshInternal(BallData ball) {
    Optional<BallPrediction> predictionOptional = DllHelper.getBallPrediction();
    if (predictionOptional.isPresent()) {
      BallPrediction prediction = predictionOptional.get();
      if (prediction.slicesLength() > 0) {
        PredictionSlice nextSlice = prediction.slices(0);
        if (hasBeenTouched(nextSlice)) {
          examinedBallData = stream(prediction)
              .map(BallData::fromPredictionSlice)
              .map(ExaminedBallData::new)
              .collect(Collectors.toList());
          return true;
        }
      }
    }

    Iterator<ExaminedBallData> ballIterator = examinedBallData.iterator();
    while (ballIterator.hasNext()) {
      ExaminedBallData next = ballIterator.next();
      if (next.ball.elapsedSeconds >= ball.elapsedSeconds) {
        break;
      }

      ballIterator.remove();
    }

    return false;
  }

  private boolean hasBeenTouched(PredictionSlice nextSlice) {
    BallData prediction = BallData.fromPredictionSlice(nextSlice);
    Iterator<ExaminedBallData> ballIterator = examinedBallData.iterator();
    while (ballIterator.hasNext()) {
      ExaminedBallData nextBall = ballIterator.next();
      if (nextBall.ball.elapsedSeconds >= prediction.elapsedSeconds) {
        if (prediction.elapsedSeconds + Constants.STEP_SIZE * 1 < nextBall.ball.elapsedSeconds) {
          // This prediction is off-cycle of the ones we have. Don't worry about it.
          return false;
        }

        // Logging to check the diffs when the ball prediction is refreshed.
        logger.debug(StateLogger.format(nextBall.ball) + " time: " + nextBall.ball.elapsedSeconds);
        return !prediction.fuzzyEquals(nextBall.ball);
      }
      ballIterator.remove();
    }

    return true;
  }

  private static Stream<PredictionSlice> stream(BallPrediction prediction) {
    return IntStream.range(0, prediction.slicesLength())
        .mapToObj(prediction::slices);
  }

  public static class ExaminedBallData {

    private List<Path> paths = new ArrayList<>();
    private Boolean hittable = null;
    public final BallData ball;
    private CarData fastTarget;
    private Plan fastPlan;

    public ExaminedBallData(BallData ball) {
      this.ball = ball;
    }

    // TODO: This should be per pathing strategy
    public Optional<Boolean> isHittable() {
      return Optional.ofNullable(hittable);
    }

    public void setHittable(boolean value) {
      if (hittable == null || !hittable) {
        hittable = value;
      }
    }

    public void addPath(Path path) {
      paths.add(path);
    }

    public boolean hasPath() {
      return paths.size() > 1;
    }

    public Path getPath() {
      return paths.isEmpty() ? null : paths.get(0);
    }

    public void setFastTarget(CarData targetCar) {
      this.fastTarget = targetCar;
    }

    public CarData getFastTarget() {
      return this.fastTarget;
    }

    public void addFastPlan(Plan time) {
      this.fastPlan = time;
    }

    public Plan getFastPlan() {
      return fastPlan;
    }
  }
}
