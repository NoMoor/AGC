package com.eru.rlbot.bot.main;

import rlbot.Bot;
import rlbot.manager.BotManager;
import rlbot.pyinterop.DefaultPythonInterface;

/** Factor for creating {@link Acg}s. */
public final class BotFactory extends DefaultPythonInterface {

  BotFactory(BotManager botManager) {
    super(botManager);
  }

  protected Bot initBot(int playerIndex, String botType, int team) {
    return new Acg(playerIndex, team);
  }
}