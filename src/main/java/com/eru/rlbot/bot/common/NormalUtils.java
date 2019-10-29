package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.HashMap;
import java.util.Map;

public class NormalUtils {

  private static final Vector3 NOSE_NORTH = Vector3.of(0, 1, 0);

  private DataPacket cacheKey;
  private BallData cacheBall;
  private CarData cacheCar;

  private static final Map<Integer, NormalUtils> CACHE = new HashMap<>();

  public static NormalUtils from(int index) {
    if (!CACHE.containsKey(index)) {
      CACHE.put(index, new NormalUtils());
    }

    return CACHE.get(index);
  }

  public static NormalUtils from(EruBot bot) {
    return from(bot.getIndex());
  }

  public static NormalUtils from(DataPacket input) {
    return from(input.playerIndex);
  }

  private NormalUtils() {}

  public static BallData noseNormal(DataPacket input) {
    return from(input).noseNormalInternal(input);
  }

  /** Returns the ball position / velocity relative to the car position. */
  // Context: https://stackoverflow.com/questions/14607640/rotating-a-vector-in-3d-space
  private BallData noseNormalInternal(DataPacket input) {
    if (input == cacheKey) {
      return cacheBall;
    }

    update(input);
    return cacheBall;
  }


  public static CarData rollNormal(DataPacket input) {
    return from(input).rollNormalInternal(input);
  }

  /** Returns the car position / velocity relative to the ball velocity/postiion. */
  public CarData rollNormalInternal(DataPacket input) {
    if (input == cacheKey) {
      return cacheCar;
    }
    update(input);
    return cacheCar;
  }

  private void update(DataPacket input) {
    cacheKey = input;
    cacheBall = new BallData(
        translateRelative(input.car.position, input.ball.position, input.car.orientation.getNoseVector()),
        translateRelative(input.car.velocity, input.ball.velocity, input.car.orientation.getNoseVector()));

    Vector3 ballRoll = input.ball.velocity;
    if (ballRoll.norm() < 10) {
      // The ball is stationary enough. Normalize to the car.
      ballRoll = input.car.position.minus(input.ball.position).normalized();
    }

    cacheCar = new CarData.Builder()
        .setPosition(translateRelative(input.ball.position, input.car.position, ballRoll))
        .setVelocity(translateRelative(input.ball.velocity, input.car.velocity, ballRoll))
        .build();
  }

  private static Vector3 translateRelative(Vector3 source, Vector3 target, Vector3 referenceOrientation) {
    Vector3 relativeVector = target.minus(source);

    // Translate the vector relative to the reference orientation.
    // This always returns a positive number....
    double translationAngle = referenceOrientation.flatten().correctionAngle(NOSE_NORTH.flatten()); // Negate to row correction angle to north.

    double relativeX = (Math.cos(translationAngle) * relativeVector.x) - (Math.sin(translationAngle) * relativeVector.y);
    double relativeY = (Math.sin(translationAngle) * relativeVector.x) + (Math.cos(translationAngle) * relativeVector.y);
    double relativeZ = relativeVector.z;

    return Vector3.of(relativeX, relativeY, relativeZ);
  }
}