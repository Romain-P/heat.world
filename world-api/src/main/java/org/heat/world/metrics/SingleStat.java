package org.heat.world.metrics;

import lombok.Data;
import org.heat.shared.Arithmetics;

@Data
public final class SingleStat implements GameStat {
    final GameStats<LimitStat> id;

    short current;

    @Override
    public short getTotal() {
        return current;
    }

    public void plus(short current) {
        this.current = Arithmetics.addShorts(this.current, current);
    }
}
