package com.eru.rlbot.bot.common;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.eru.rlbot.common.Matrix3;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link Angles3}.
 */
@RunWith(JUnit4.class)
public class Angles3Test {

  @Test
  public void orientation() {
    Orientation o = Orientation.convert(0, 0, 0);
    assertThat((double) o.getRightVector().y, is(closeTo(-1, .0001f)));
  }

  @Test
  public void test_xAxis() {
    CarData.Builder cBuilder = new CarData.Builder();
    cBuilder.setTime(0.0f);
    cBuilder.setPosition(Vector3.of(0, 0, 0));
    cBuilder.setVelocity(Vector3.of(0, 0, 0));
    cBuilder.setAngularVelocity(Vector3.of(0, 0, 0));
    cBuilder.setOrientation(Orientation.convert(1, 0, 0));
    CarData car = cBuilder.build();

    Matrix3 target = Matrix3.of(
        Vector3.of(1, 0, 0),
        Vector3.of(0, 1, 0),
        Vector3.of(0, 0, 1));

    Controls output = Controls.create();

    Angles3.setControlsFor(car, Orientation.fromOrientationMatrix(target), output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(-1.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(0.0, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(0.0, .01)));
  }

  @Test
  public void test_yAxis() {
    CarData.Builder cbuilder = new CarData.Builder();
    cbuilder.setTime(0.0f);
    cbuilder.setPosition(Vector3.of(0, 0, 0));
    cbuilder.setVelocity(Vector3.of(0, 0, 0));
    cbuilder.setAngularVelocity(Vector3.of(0, 0, 0));
    cbuilder.setOrientation(Orientation.convert(0, 1, 0));
    CarData car = cbuilder.build();

    Matrix3 target = Matrix3.of(
        Vector3.of(1, 0, 0),
        Vector3.of(0, 1, 0),
        Vector3.of(0, 0, 1));

    Controls output = Controls.create();

    Angles3.setControlsFor(car, Orientation.fromOrientationMatrix(target), output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(0.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(0.0, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(-1.0, .01)));
  }

  @Test
  public void test_zAxis() {
    CarData.Builder cbuilder = new CarData.Builder();
    cbuilder.setTime(0.0f);
    cbuilder.setPosition(Vector3.of(0, 0, 500));
    cbuilder.setVelocity(Vector3.of(0, 0, 0));
    cbuilder.setAngularVelocity(Vector3.of(0, 0, 0));
    cbuilder.setOrientation(Orientation.convert(0, 0, 1.65)); // ???
    CarData car = cbuilder.build();

    Matrix3 target = Matrix3.of(
        Vector3.of(1, 0, 0),
        Vector3.of(0, 1, 0),
        Vector3.of(0, 0, 1));

    Controls output = Controls.create();

    Angles3.setControlsFor(car, Orientation.fromOrientationMatrix(target), output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(0.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(-.96, .01))); //???
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(0.0, .01)));
  }

  @Test
  public void test_xAngularVelocity() {
    CarData.Builder cbuilder = new CarData.Builder();
    cbuilder.setTime(0.0f);
    cbuilder.setPosition(Vector3.of(0, 0, 0));
    cbuilder.setVelocity(Vector3.of(0, 0, 0));
    cbuilder.setAngularVelocity(Vector3.of(15, 0, 0));
    cbuilder.setOrientation(Orientation.convert(0, 0, 0));
    CarData car = cbuilder.build();

    Matrix3 target = Matrix3.of(
        Vector3.of(1, 0, 0),
        Vector3.of(0, -1, 0),
        Vector3.of(0, 0, 1));

    Controls output = Controls.create();

    Angles3.setControlsFor(car, Orientation.fromOrientationMatrix(target), output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(0.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(1, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(0.0, .01)));
  }

  @Test
  public void test_targetPosition() {
    CarData.Builder cBuilder = new CarData.Builder();

    cBuilder
        .setTime(0.0f)
        .setPosition(Vector3.of(0, 0, 0))
        .setVelocity(Vector3.of(0, 0, 0))
        .setAngularVelocity(Vector3.of(0, 0, 0))
        .setOrientation(Orientation.convert(0, 0, 0));
    CarData car = cBuilder.build();

    Matrix3 target = Orientation.convert(0, 1, 0).getOrientationMatrix();

    Controls output = Controls.create();

    Angles3.setControlsFor(car, Orientation.fromOrientationMatrix(target), output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(0.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(0.0, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(1.0, .01)));
  }

  @Test
  public void test_targetAllPositions() {
    CarData.Builder cBuilder = new CarData.Builder();

    cBuilder
        .setTime(0.0f)
        .setPosition(Vector3.of(0, 0, 0))
        .setVelocity(Vector3.of(0, 0, 0))
        .setAngularVelocity(Vector3.of(0, 0, 0))
        .setOrientation(Orientation.convert(0, 0, 0));
    CarData car = cBuilder.build();

    Matrix3 target = Orientation.convert(1, 1, 1).getOrientationMatrix();

    Controls output = Controls.create();

    Angles3.setControlsFor(car, Orientation.fromOrientationMatrix(target), output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(1.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(.3, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(.42, .01)));
  }

  public static void print(String s, Object... args) {
    System.out.println(String.format(s, args));
  }
}