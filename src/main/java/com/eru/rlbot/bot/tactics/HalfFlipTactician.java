package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

/** Manages straight and back half flips. */
public class HalfFlipTactician extends Tactician {

  HalfFlipTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void internalExecute(DataPacket input, ControlsOutput output, Tactic nextTactic) {

  }
}
