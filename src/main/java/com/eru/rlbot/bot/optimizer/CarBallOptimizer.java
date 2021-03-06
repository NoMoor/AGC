package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.prediction.CarBallCollision;
import com.eru.rlbot.common.Matrix3;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optimizes a car-ball interaction.
 */
public final class CarBallOptimizer {

  private static final Logger logger = LogManager.getLogger("CarBallOptimizer");

  private static final float AVERAGE_SPEED = 1200;

  private static final ImmutableList<Double> STEP_SIZES = ImmutableList.of(.25, .05, .01);

  public static CarData getOptimalApproach(BallData ball, Vector3 target) {
    // The ball and target are close enough, just hit it toward the target.
    if (ball.position.distance(target) < 1000) {
      return makeCar(ball, target.minus(ball.position).normalize());
    }

    long startTime = System.nanoTime();
    Vector3 workingAngle = target.minus(ball.position).normalize();
    for (double nextStepSize : STEP_SIZES) {
      workingAngle = refineApproach(ball, target, workingAngle, nextStepSize);
    }
    logger.warn("Optimization time: " + (System.nanoTime() - startTime));
    return makeCar(ball, workingAngle);
  }

  private static Vector3 refineApproach(BallData ball, Vector3 target, Vector3 previousAngle, double granularity) {
    // TODO: Find the best vertical angle as well...
    final Vector3 targetAngle = target.minus(ball.position).normalize();

    Vector3 nextAngle = previousAngle;

    BallData prevResult = CarBallCollision.calculateCollision(ball, makeCar(ball, previousAngle));
    double previousOffset = Angles.flatCorrectionAngle(prevResult.velocity, previousAngle);
    double nextOffset = previousOffset;

    Matrix3 rotationMatrix = Angles3.rotationMatrix(granularity);
    Matrix3 rotationMatrixInverse = rotationMatrix.inverse();

    while (Math.signum(previousOffset) == Math.signum(nextOffset)) {
      // Advance the previous values forward one step.
      previousOffset = nextOffset;
      previousAngle = nextAngle;

      // Move the next values forward one step.
      if (nextOffset < 0) {
        nextAngle = rotationMatrix.dot(nextAngle);
      } else {
        nextAngle = rotationMatrixInverse.dot(nextAngle);
      }

      BallData nextResult = CarBallCollision.calculateCollision(ball, makeCar(ball, nextAngle));
      nextOffset = Angles.flatCorrectionAngle(nextResult.velocity, targetAngle);
    }

    return Math.abs(previousOffset) < Math.abs(nextOffset) ? previousAngle : nextAngle;
  }

  private static CarData makeCar(BallData ball, Vector3 noseOrientation) {
    Vector3 sideDoor = noseOrientation.cross(Vector3.of(0, 0, 1)).normalize();
    Vector3 roofOrientation = sideDoor.cross(noseOrientation);
    Orientation carOrientation = Orientation.noseRoof(noseOrientation, roofOrientation);
    Vector3 carPosition = ball.position.minus(noseOrientation.toMagnitude(Constants.BALL_RADIUS + BoundingBox.frontToRj));

    return CarData.builder()
        .setOrientation(carOrientation)
        .setVelocity(noseOrientation.toMagnitude(AVERAGE_SPEED))
        .setPosition(carPosition)
        .setTime(ball.time)
        .build();
  }

  // TODO: Create multiple optimizers out of this (order, clamp, range, precision, etc.)
  public static OptimizationResult getOptimalApproach(BallData ball, Vector3 target, final CarData car) {
    // Optimize x offset.
    XOptimizer xOptimizer = new XOptimizer();
    ZOptimizer zOptimizer = new ZOptimizer(ball, car);
    AOptimizer aOptimizer = new AOptimizer(ball);
    SpeedOptimizer speedOptimizer = new SpeedOptimizer(car);

    ImmutableList<Optimizer> optimizers = ImmutableList.of(xOptimizer, zOptimizer, aOptimizer, speedOptimizer);

    while (optimizers.stream().anyMatch(optimizer -> !optimizer.isDone())) {
      for (Optimizer optimizer : optimizers) {
        optimizer.doStep(ball, car, target);
      }
    }

    CarData optimalCar = car;
    for (Optimizer optimizer : optimizers) {
      optimalCar = optimizer.adjust(optimalCar, optimizer.currentValue);
    }

    return OptimizationResult.create(optimalCar, xOptimizer.currentValue, zOptimizer.currentValue, aOptimizer.currentValue, speedOptimizer.currentValue);
  }

  public static OptimizationResult xSpeed(Moment moment, Vector3 target, final CarData car) {
    return xSpeed(moment.toBall(), target, car);
  }

  // TODO: Create multiple optimizers out of this (order, clamp, range, precision, etc.)
  public static OptimizationResult xSpeed(BallData ball, Vector3 target, final CarData car) {
    // Optimize x offset.
    XOptimizer xOptimizer = new XOptimizer();
    SpeedOptimizer speedOptimizer = new SpeedOptimizer(car);

    ImmutableList<Optimizer> optimizers = ImmutableList.of(xOptimizer, speedOptimizer);

    while (optimizers.stream().anyMatch(optimizer -> !optimizer.isDone())) {
      for (Optimizer optimizer : optimizers) {
        optimizer.doStep(ball, car, target);
      }
    }

    CarData optimalCar = car;
    for (Optimizer optimizer : optimizers) {
      optimalCar = optimizer.adjust(optimalCar, optimizer.currentValue);
    }

    return OptimizationResult.create(optimalCar, xOptimizer.currentValue, 0, 0, car.groundSpeed);
  }

  private CarBallOptimizer() {
  }
}
