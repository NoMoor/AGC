from rlbottraining.common_exercises import bronze_goalie as common_bronze_goalie
from rlbottraining.common_exercises import bronze_striker as common_bronze_striker
from rlbottraining.common_exercises import silver_goalie as common_silver_goalie
from rlbottraining.common_exercises import silver_striker as common_silver_striker

from aerials.aerial_exercises import aerial_exercises
from agc_player_config import agc_blue
from bronze.bronze_exercises import bronze_exercises
from dribble.dribble_exercises import dribble_exercises
from kickoff.kickoff_exercises import kickoff_exercises
from rebound.rebound_exercises import rebound_exercises
from stone.stone_exercises import stone_exercises
from wavedash.wavedash_exercises import wavedash_exercises


def make_default_playlist():
    exercises = []

    exercises += wavedash_exercises

    if False:
        exercises += bronze_exercises
        exercises += stone_exercises
        exercises += bronze_exercises
        exercises += aerial_exercises
        exercises += dribble_exercises
        exercises += kickoff_exercises
        exercises += wavedash_exercises
        exercises += rebound_exercises
        exercises += common_bronze_striker.make_default_playlist()
        exercises += common_bronze_goalie.make_default_playlist()
        exercises += common_silver_striker.make_default_playlist()
        exercises += common_silver_goalie.make_default_playlist()

    for exercise in exercises:
        exercise.match_config.player_configs = [agc_blue]
        exercise.match_config.enable_lockstep = True

    return exercises
