package com.eru.rlbot.bot;

import com.eru.rlbot.bot.common.CarBall;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.prediction.CarBallCollision;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Checks the input for touches between the controlling car and the ball.
 */
public final class CarBallContactManager {

  private static final Logger logger = LogManager.getLogger("Prediction");

  private static final ConcurrentHashMap<Integer, CarBallContactManager> MAP = new ConcurrentHashMap<>();

  private boolean headersLogged = false;
  private float touchTime = -1;
  private CarData previousCar;
  private BallData previousBallData;
  private final int playerIndex;

  public static void track(DataPacket input) {
    get(input.serialNumber).trackInternal(input);
  }

  private static CarBallContactManager get(int playerIndex) {
    return MAP.computeIfAbsent(playerIndex, CarBallContactManager::new);
  }

  private CarBallContactManager(int playerIndex) {
    this.playerIndex = playerIndex;
  }

  private void trackInternal(DataPacket input) {
    Vector3 nearestPointWorld = CarBall.nearestPointOnHitBox(input.ball.position, input.car);

    BotRenderer botRenderer = BotRenderer.forIndex(input.serialNumber);

    double distanceToBall = Vector3.from(nearestPointWorld, input.ball.position).magnitude()
        - Constants.BALL_COLLISION_RADIUS;
    botRenderer.setNearestHitboxPoint(nearestPointWorld);

    if (touchTime == -1 && distanceToBall < 2) {
      touchTime = input.car.elapsedSeconds;
      previousBallData = input.ball;
      previousCar = input.car;
    } else if (touchTime != -1) {
      touchTime = -1;
    }
  }

  public static boolean isTouched(DataPacket input) {
    return get(input.serialNumber).isTouched();
  }

  public boolean isTouched() {
    return touchTime != -1;
  }

  private static volatile int renderIndex = -1;
  public void renderAndLogPrediction(DataPacket input) {
    if (playerIndex != renderIndex && renderIndex != -1) {
      return;
    } else {
      renderIndex = playerIndex;
    }

    if (touchTime != -1) {
      BotRenderer botRenderer = BotRenderer.forCar(input.car);
      BallData prediction = CarBallCollision.calculateCollision(input.ball, input.car);
      botRenderer.setPredictionDiff(prediction, input.ball);
      logResult(prediction, input);
      botRenderer.setTouchIndicator(input);
    }
  }

  private void logResult(BallData ballPrediction, DataPacket input) {
    if (!headersLogged) {
      headersLogged = true;
      logger.debug(CarData.csvHeader("cprev")
          + BallData.csvHeader("bprev")
          + BallData.csvHeader("bpred")
          + BallData.csvHeader("bact"));
    }

    String logMessage = previousCar.toCsv() +
        previousBallData.toCsv() +
        ballPrediction.toCsv() +
        input.ball.toCsv();

    logger.log(Level.INFO, logMessage);
  }
}
