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

    /**
     * Get max transportable weight. See http://dofuswiki.wikia.com/wiki/Characteristic#Pods_.28Carrying_Capacity.29
     *
     * <p>
     * This statistic determines the number of items you can carry. The base value is 1000.
     * Each profession level of the character gives +5 pods, and each level 100 profession gives an additional +1000 pods.
     * Strength also affects carrying capacity, at the rate of 5 pods per strength point.
     * Pods can also be obtained from Pods equipment.
     *
     * @return an integer
     */
    default int getMaxWeight() {
        return Players.BASE_TRANSPORTABLE_WEIGHT
                + get(GameStats.STRENGTH).getSafeTotal() * 5
                + get(GameStats.PODS).getTotal()
                // TODO(world/players): jobs affect transportable weight
                ;
    }
}
