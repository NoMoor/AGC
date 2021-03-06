package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.RelativeUtils;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.Recover;
import com.eru.rlbot.bot.maneuver.WallHelper;
import com.eru.rlbot.bot.optimizer.CarBallOptimizer;
import com.eru.rlbot.bot.optimizer.OptimizationResult;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.Iterables;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Relies on other low level tactical units to do the movement but this tactician is responsible for planning a shot on
 * goal.
 */
public class TakeTheShotTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("TakeTheShot");

  TakeTheShotTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public boolean isLocked() {
    return super.isLocked();
  }

  private Path path;

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    // If the tactic location is above 300 uu, move it down since this tactician cannot aerial.
    if (tactic.subject.position.z > 500) {
      Moment subject = tactic.subject.toBuilder()
          .setPosition(tactic.subject.position.addZ(500 - tactic.subject.position.z)) // set Z to 500
          .build();

      tactic = tactic.toBuilder()
          .setSubject(subject)
          .build();
    }

    if (WallHelper.isOnWall(input.car)) {
      bot.botRenderer.setBranchInfo("Get off the wall");

      WallHelper.drive(input, output, input.ball.position);
      return;
    } else if (!input.car.hasWheelContact) {
      delegateTo(new Recover(input.ball.position));
      return;
    } else if ((path == null
        || path.isOffCourse()
        || BallPredictionUtil.get(input.car).wasTouched()
        || pathEndWithoutBall(path))
        && input.car.hasWheelContact)
    // Do not re-plan once we have jumped.
    {
      Optional<CarData> targetOptional = PathPlanner.closestStrike(input.car, tactic.subject);

      if (!targetOptional.isPresent()) {
        bot.botRenderer.setBranchInfo("Target not found");
        return;
      } else {
        bot.botRenderer.setBranchInfo("Target acquired");
      }

      CarData target = targetOptional.get();

      Path newPath;
      if (tactic.object != null) {
        OptimizationResult optimalHit = CarBallOptimizer.xSpeed(tactic.subject, tactic.object, target);
        newPath = PathPlanner.oneTurn(input.car, Moment.from(optimalHit.car));
      } else {
        newPath = PathPlanner.oneTurn(input.car, Moment.from(target));
      }

      if (newPath == null) {
        // Stay on the old path.
      } else if (newPath.lockAndSegment(true)) {
        // Accept the new path.
        path = newPath;
        path.extendThroughBall();
      }
    }

    if (path == null) {
      bot.botRenderer.setBranchInfo("Dumb executor");

      pathExecutor.executeSimplePath(input, output, tactic);
      return;
    }

    bot.botRenderer.renderPath(input, path);
    pathExecutor.executePath(input, output, path);

    if (output.getThrottle() < 0 && !output.holdBoost() && input.ball.velocity.magnitude() < .1) {
      BallData relativeBall = RelativeUtils.noseRelativeBall(input);
      logger.info("Slowing down! throttle: {} ballSpeed: {} ballDistance: {}", output.getThrottle(), input.ball.velocity.magnitude(), relativeBall.position);
    }
  }

  private boolean pathEndWithoutBall(Path path) {
    double time = path.getEndTime();
    Vector3 endLocation = Iterables.getLast(path.allTerseNodes()).end;
    return BallPredictionUtil.get(path.getSource().serialNumber).getPredictions().stream()
        .filter(ballPrediction -> ballPrediction.ball.time > time)
        .findFirst()
        .map(ballPrediction -> ballPrediction.ball.position.distance(endLocation) > 200) // Ball is more than 200 units from end point.
        .orElse(false);
  }

  @Override
  public boolean allowDelegate() {
    return true;
  }
}
