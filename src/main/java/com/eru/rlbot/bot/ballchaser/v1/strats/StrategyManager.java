package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.Bot;

import java.awt.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.eru.rlbot.bot.common.Constants.*;

public class StrategyManager {

  // Delay ~1 frame after a jump to allow prediction propagation.
  private static final float RESET_DELAY = .1f;

  private static final Tactic LEFT_WALL_TACTIC = new Tactic(LEFT_SIDE_WALL, Tactic.Type.HIT_BALL);
  private static final Tactic RIGHT_WALL_TACTIC = new Tactic(RIGHT_SIDE_WALL, Tactic.Type.HIT_BALL);

  private static boolean grabBoostStrat = false;
  private static boolean wallRideStrat = false;
  private static boolean hitBallStrat = true;
  private final BotRenderer botRenderer;

  // DO NOT MODIFY
  private Map<Strategy.Type, Strategist> strategists = new HashMap<>();

  private Strategist active;

  private Bot bot;

  private Vector3 lastBallPosition;
  private Vector3 lastCarPosition;
  private float resetTime;

  public StrategyManager(Bot bot) {
    this.bot = bot;
    this.botRenderer = BotRenderer.forBot(bot);
    for(Strategy.Type type : Strategy.Type.values()) {
      strategists.put(type, Strategy.stratigistForBot(type, bot));
    }
  }

  // TODO this only periodically.
  public void updateStrategy(DataPacket input) {
    if (checkPause(input)) {
      return;
    }

    if (active == null || active.getType() != Strategy.Type.ATTACK) {
      active = strategists.get(Strategy.Type.ATTACK);
      if (!active.assign(input)) {
        active = null;
      }
    }

    // TODO: Find a place for this code.
//    if (grabBoostStrat && input.car.boost < 20) {
//      // TODO(ahatfield): Put this as an available tactic that can just be added.
//      // Lets get some boost.
//      // TODO(ahatfield): Prioritize one that is in front of me.
//      // TODO(ahatfield): Prioritize one that is closer to my goal.
//      BoostManager.allBoosts().stream()
//          .filter(BoostPad::isActive)
//          .min(selectBoost(input))
//          .ifPresent(boost -> TacticManager.setTactic(new Tactic(boost.getLocation(), Tactic.Type.GRAB_BOOST)));
//    } else if (wallRideStrat) {
//      if (input.car.position.distance(TacticManager.getNextTarget()) < 100) {
//        Tactic lastTactic = TacticManager.tacticFulfilled();
//
//        if (lastTactic.target.position == LEFT_SIDE_WALL) {
//          TacticManager.setTactic(RIGHT_WALL_TACTIC);
//        } else {
//          TacticManager.setTactic(LEFT_WALL_TACTIC);
//        }
//      }
//    }
  }

  private boolean checkPause(DataPacket input) {
    if (active != null && ballHasJumped(input) && carHasJumped(input)) {
      active.abort();
      active = null;
      resetTime = input.car.elapsedSeconds;
    }

    if (resetTime != 0 && resetTime + RESET_DELAY > input.car.elapsedSeconds) {
      botRenderer.addText("RESET: Wait...", Color.red);
      return true;
    }

    resetTime = 0;
    return false;
  }

  private boolean carHasJumped(DataPacket input) {
    if (lastCarPosition == null) {
      return true;
    }

    return input.car.position.distance(lastCarPosition) > NORMAL_EXPECTED;
  }

  private boolean ballHasJumped(DataPacket input) {
    if (lastBallPosition == null) {
      return true;
    }

    return input.ball.position.distance(lastBallPosition) > NORMAL_EXPECTED;
  }

  private void updateCarAndBallTracking(DataPacket input) {
    lastCarPosition = input.car.position;
    lastBallPosition = input.ball.position;
  }

  private static Comparator<? super BoostPad> selectBoost(DataPacket input) {
    Vector2 noseVector = input.car.orientation.noseVector.flatten();
    Vector2 flatPosition = input.car.position.flatten();

    return (a, b) -> {
      // Angle diff in radians
      int angleValue = (int) (Math.abs(noseVector.correctionAngle(a.getLocation().flatten()))
                                  - Math.abs(noseVector.correctionAngle(b.getLocation().flatten())));
      // 750 units is worth a u-turn.
      int distanceValue = (int) (flatPosition.distance(a.getLocation().flatten())
                                     - flatPosition.distance(b.getLocation().flatten())) / 2000;
      return angleValue + distanceValue;
    };
  }

  public ControlsOutput executeStrategy(DataPacket input) {
    ControlsOutput output;
    if (active == null) {
      // Park this frame.
      output = new ControlsOutput();
    } else {
      output = active.execute(input);
    }

    updateCarAndBallTracking(input);
    return output;
  }
}
