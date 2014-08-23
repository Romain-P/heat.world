package org.heat.world.trading.impl.player;

import com.github.blackrush.acara.EventBus;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemWallet;
import org.heat.world.items.WorldItemWallets;
import org.heat.world.trading.WorldTradeSide;
import org.heat.world.trading.impl.player.events.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.heat.world.trading.WorldTradeSide.FIRST;
import static org.heat.world.trading.WorldTradeSide.SECOND;

final class PlayerTradeImpl implements PlayerTrade {
    @Getter final EventBus eventBus;

    final ImmutableMap<WorldTradeSide, SideState> states;

    Optional<? extends Result> result = Optional.empty();

    PlayerTradeImpl(EventBus eventBus, PlayerTrader first, PlayerTrader second) {
        this.eventBus = eventBus;
        this.states = ImmutableMap.of(
            FIRST,  new SideState(first),
            SECOND, new SideState(second)
        );
    }

    @Synchronized
    @Override
    public Optional<? extends Result> conclude() {
        if (!result.isPresent()) {
            SideState first = state(FIRST),
                    second = state(SECOND);

            if (!first.check || !second.check) {
                return Optional.empty();
            }

            result = Optional.of(new ConcludedResult(first.createWallet(), second.createWallet()));
        }

        return result;
    }

    @Override
    public void cancel(WorldTradeSide side) {
        if (result.isPresent()) {
            return;
        }

        SideState state = state(side);
        PlayerTrader canceller = state.trader;

        result = Optional.of(new CancelledResult(canceller));
        eventBus.publish(new PlayerTradeCancelEvent(this, canceller));
    }

    @Override
    public boolean isConcluded() {
        return result.isPresent() && result.get() instanceof ConcludedResult;
    }

    @Override
    public boolean isCancelled() {
        return result.isPresent() && result.get() instanceof CancelledResult;
    }

    SideState state(WorldTradeSide side) {
        if (result.isPresent()) {
            throw new IllegalStateException("trade has been " + result.get());
        }
        return states.get(side);
    }

    //<editor-fold desc="Results">

    @RequiredArgsConstructor
    @Getter
    class ConcludedResult implements Result {
        final WorldItemWallet first, second;

        @Override
        public String toString() {
            return "concluded";
        }
    }

    @RequiredArgsConstructor
    class CancelledResult implements Result {
        final PlayerTrader canceller;

        @Override
        public WorldItemWallet getFirst() {
            throw new UnsupportedOperationException("trade has been cancelled by " + canceller);
        }

        @Override
        public WorldItemWallet getSecond() {
            throw new UnsupportedOperationException("trade has been cancelled by " + canceller);
        }

        @Override
        public String toString() {
            return "cancelled by " + canceller;
        }
    }

    //</editor-fold>
    //<editor-fold desc="SideState">

    @RequiredArgsConstructor
    class SideState {
        final PlayerTrader trader;

        boolean check;
        List<WorldItem> items = new ArrayList<>();
        int kamas;

        void addItem(WorldItem item) {
            if (items.contains(item)) return;
            items.add(item);
            eventBus.publish(new PlayerTradeAddItemEvent(PlayerTradeImpl.this, trader, item));
        }

        Optional<WorldItem> tryRemoveItem(int itemUid) {
            Optional<WorldItem> option = items.stream().filter(x -> x.getUid() == itemUid).findAny();
            option.ifPresent(item -> {
                items.remove(item);
                eventBus.publish(new PlayerTradeRemoveItemEvent(PlayerTradeImpl.this, trader, item));
            });
            return option;
        }

        void removeItem(WorldItem item) {
            if (items.remove(item)) {
                eventBus.publish(new PlayerTradeRemoveItemEvent(PlayerTradeImpl.this, trader, item));
            }
        }

        void addKamas(int kamas) {
            if (kamas < 0) {
                throw new IllegalArgumentException("you cannot add a negative amount of kamas");
            }
            if (kamas == 0) {
                return;
            }
            this.kamas += kamas;
            eventBus.publish(new PlayerTradeAddKamasEvent(PlayerTradeImpl.this, trader, kamas));
        }

        void removeKamas(int kamas) {
            if (kamas < 0) {
                throw new IllegalArgumentException("you cannot remove a negative amount of kamas");
            }
            if (kamas == 0) {
                return;
            }
            this.kamas -= kamas;
            eventBus.publish(new PlayerTradeRemoveKamasEvent(PlayerTradeImpl.this, trader, kamas));
        }

        void check() {
            if (check) {
                return;
            }

            this.check = true;
            eventBus.publish(new PlayerTradeCheckEvent(PlayerTradeImpl.this, trader, true));
        }

        void uncheck() {
            if (!check) {
                return;
            }

            this.check = false;
            eventBus.publish(new PlayerTradeCheckEvent(PlayerTradeImpl.this, trader, false));
        }

        WorldItemWallet createWallet() {
            return WorldItemWallets.unmodifiable(items, kamas);
        }
    }
    //</editor-fold>
    //<editor-fold desc="Delegates to SideState">

    @Synchronized
    @Override
    public void addItem(WorldTradeSide side, WorldItem item) {
        state(side).addItem(item);
    }

    @Synchronized
    @Override
    public void removeItem(WorldTradeSide side, WorldItem item) {
        state(side).removeItem(item);
    }

    @Synchronized
    @Override
    public Optional<WorldItem> tryRemoveItem(WorldTradeSide side, int itemUid) {
        return state(side).tryRemoveItem(itemUid);
    }

    @Synchronized
    @Override
    public void addKamas(WorldTradeSide side, int kamas) {
        state(side).addKamas(kamas);
    }

    @Synchronized
    @Override
    public void removeKamas(WorldTradeSide side, int kamas) {
        state(side).removeKamas(kamas);
    }

    @Synchronized
    @Override
    public void check(WorldTradeSide side) {
        state(side).check();
    }

    @Synchronized
    @Override
    public void uncheck(WorldTradeSide side) {
        state(side).uncheck();
    }

    //</editor-fold>
}
