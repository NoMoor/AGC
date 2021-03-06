from agc_player_config import agc_orange
from kickoff.kickoff_exercises import kickoff_exercises
from stone.testing import orange_testing_locations


def make_default_playlist():
    exercises = []

    exercises += orange_testing_locations

    if False:
        # exercises += bronze_striking

        # exercises += stone_exercises
        # exercises += bronze_exercises
        # exercises += aerial_exercises
        # exercises += dribble_exercises
        exercises += kickoff_exercises
        # exercises += wavedash_exercises
        # exercises += rebound_exercises
        # exercises += common_bronze_striker()
        # exercises += common_bronze_goalie()
        # exercises += common_silver_striker()
        # exercises += common_silver_goalie()

    for exercise in exercises:
        exercise.match_config.player_configs = [agc_orange]
        exercise.match_config.enable_lockstep = False

    return exercises
