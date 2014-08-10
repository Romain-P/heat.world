package org.heat.world.players;

import org.heat.world.metrics.GameStatBook;
import org.heat.world.metrics.GameStats;
import org.heat.world.metrics.RegularStat;

public interface PlayerStatBook extends GameStatBook {
    int getStatsPoints();
    void plusStatsPoints(int statsPoints);

    int getSpellsPoints();
    void plusSpellsPoints(int spellsPoints);

    /**
     * Upgrade a stat by a given amount of points. This also remove <i>statsPoints</i> from this book
     * @param id a non-null stat
     * @param points a positive integer
     * @return amount of points added
     */
    int upgradeStat(GameStats<RegularStat> id, int points);
}
