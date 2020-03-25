package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Helper for doing flip maneuvers.
 */
public class Flip extends Maneuver {

  private static final double EIGHTH = Math.PI / 4;
  private static final double QUARTER = Math.PI / 2;
  private final double aggressiveness;
  private final Vector3 target;

  // Null if they should be determined by the helper based on the target position.
  private final Double yaw;
  private final Double pitch;

  private boolean initialJump;
  private boolean flipComplete;
  private boolean done;
  private boolean flipAtTarget;

  private boolean isFirstFrame = true;

  public Flip(Builder builder) {
    this.aggressiveness = builder.aggressiveness;
    this.target = builder.target;
    this.pitch = builder.pitch;
    this.yaw = builder.yaw;
    this.flipAtTarget = builder.flipAtTarget;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void execute(DataPacket input, Controls output, Tactic tactic) {
    if (done) {
      // Do nothing.
      return;
    }

    BotRenderer botRenderer = BotRenderer.forCar(input.car);
    JumpManager jumpManager = JumpManager.forCar(input.car);

    double framesToTarget = (input.car.position.distance(target) / input.car.groundSpeed) * Constants.STEP_SIZE_COUNT;
    boolean flipIntoTarget = flipAtTarget && framesToTarget < 13;
    boolean maxHeightJump = jumpManager.hasMaxJumpHeight();
    boolean flipImmediately = !flipAtTarget && input.car.position.z > 50;

    boolean holdJump = !(flipIntoTarget || maxHeightJump || flipImmediately);

    if (flipComplete) {
      if (input.car.hasWheelContact) {
        botRenderer.setBranchInfo("Flip complete");
        done = true;
      } else {
        botRenderer.setBranchInfo("Waiting to land");
        float noseZ = input.car.orientation.getNoseVector().z;
        output
            .withBoost(-.1 < noseZ && noseZ < .2)
            .withThrottle(1.0f);

        if (!JumpManager.forCar(input.car).isFlipping()) {
          // TODO: Replace this with generic landing helper.
          Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(input.car).getOrientationMatrix(), output);
        }
      }
    } else {
      if (!input.car.jumped) {
        botRenderer.setBranchInfo("Initial Jump");
        boolean isRotating = input.car.angularVelocity.flat().magnitude() > .1;
        boolean jumpThisFrame = (!JumpManager.forCar(input.car).jumpPressedLastFrame() || !isFirstFrame) && !isRotating;
        isFirstFrame = false;

        if (!initialJump) {
          initialJump = jumpThisFrame;
        }

        // Jump now
        output
            .withJump(initialJump)
            .withThrottle(1.0)
            .withBoost();
      } else if (holdJump) {
        botRenderer.setBranchInfo("Hold Jump");
        output
            .withThrottle(1.0)
            .withJump()
            .withBoost();
      } else if (!jumpManager.hasReleasedJumpInAir()) {
        botRenderer.setBranchInfo("Quick release");
        output.withBoost();
        // Release Jump
      } else if (jumpManager.canFlip()) {
        if (this.flipAtTarget && !flipIntoTarget) {
          return;
        }

        botRenderer.setBranchInfo("Do flip");

        if (pitch != null && yaw != null) {
          output
              .withJump()
              .withPitch(pitch)
              .withYaw(yaw);
        } else {
          double velocityCorrectionAngle =
              Angles.flatCorrectionAngle(input.car.position, input.car.velocity, getTarget(tactic));
          // TODO: Adjust this correction based on how fast we are going and angle flipping.
          velocityCorrectionAngle *= 2;

          double noseVelocityAngle = input.car.orientation.getNoseVector().flatten()
              .correctionAngle(input.car.velocity.flatten());
          double totalCorrection = velocityCorrectionAngle + noseVelocityAngle;

          double flipYaw, flipPitch;
          if (Math.abs(totalCorrection) < EIGHTH) {
            // Less than 45 degree flip.
            flipPitch = -1;
            flipYaw = totalCorrection / EIGHTH;
          } else {
            flipYaw = Math.signum(totalCorrection);
            flipPitch = -(QUARTER - Math.abs(totalCorrection)) / EIGHTH;
          }

          output
              .withYaw(flipYaw)
              .withJump()
              .withPitch(flipPitch);
        }

        flipComplete = true;
      }
    }
  }

  private Vector3 getTarget(Tactic tactic) {
    return target != null ? target : tactic.subject.position;
  }

  @Override
  public boolean isComplete() {
    return done;
  }

  public static class Builder {
    private boolean flipAtTarget = true;
    private double aggressiveness;
    private Vector3 target;
    private Double yaw;
    private Double pitch;

    private Builder() {
    }

    public Builder setAggressiveness(double aggressiveness) {
      this.aggressiveness = aggressiveness;
      return this;
    }

    public Builder setTarget(Vector3 target) {
      this.target = target;
      return this;
    }

    public Builder withFixedYaw(double yaw) {
      this.yaw = yaw;
      return this;
    }

    public Builder withFixedPitch(double pitch) {
      this.pitch = pitch;
      return this;
    }

    public Builder flipEarly() {
      this.flipAtTarget = false;
      return this;
    }

    public Flip build() {
      return new Flip(this);
    }
  }
}