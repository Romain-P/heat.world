package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.RequiredArgsConstructor;
import org.heat.StdDataModule;
import org.heat.data.Datacenter;
import org.heat.shared.IntPair;
import org.heat.world.players.metrics.PlayerSpell;
import org.junit.Before;
import org.junit.Test;
import org.rocket.ImmutableServiceContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.heat.shared.tests.CollectionMatchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PlayersTest {

    @Inject Datacenter datacenter;

    @Before
    public void setUp() throws Exception {
        File configFile = new File("world.conf").getAbsoluteFile();

        if (!configFile.exists()) {
            throw new FileNotFoundException(configFile.toString());
        }

        Config config = ConfigFactory.parseFileAnySyntax(configFile);

        Injector injector = Guice.createInjector(
                new StdDataModule(),
                binder -> {
                    binder.bind(Config.class).toInstance(config);
                    binder.bind(ExecutorService.class).toInstance(MoreExecutors.sameThreadExecutor());
                    binder.bind(ByteBufAllocator.class).toInstance(UnpooledByteBufAllocator.DEFAULT);
                }
        );
        injector.injectMembers(this);

        datacenter.start(ImmutableServiceContext.of(config, ClassLoader.getSystemClassLoader(), injector));
    }

    @Test
    public void testGetCostOneUpgrade() throws Exception {
        // given
        final long[][] stats = new long[][] {
                { 0, 1 },
                { 100, 2 },
                { 200, 3 },
                { 300, 4 },
                { 400, 5 },
        };

        final int[][] expectations = new int[][] {
        // actual, cost
            {0, 1},
            {99, 1},
            {100, 2},
            {199, 2},
            {200, 3},
            {299, 3},
            {300, 4},
            {399, 4},
            {400, 5},
            {499, 5},
            {1000, 5},
        };

        // when
        // then
        for (int[] expectation : expectations) {
            assertEquals("cost of one upgrade when actual=" + expectation[0],
                    expectation[1],
                    Players.getCostOneUpgrade(stats, expectation[0])
            );
        }
    }

    @RequiredArgsConstructor(staticName = "$")
    static final class expectation {
        final int actual;
        final int points;
        final IntPair expected;
        final long[][] stats;
    }

    @Test
    public void testUpgrade() throws Exception {
        // given
        final long[][] stats = new long[][] {
                { 0, 1 },
                { 100, 2 },
                { 200, 3 },
                { 300, 4 },
                { 400, 5 },
        };

        final expectation[] expectations = new expectation[]{
                expectation.$(0, 200, IntPair.of(150, 200), stats),
                expectation.$(100, 100, IntPair.of(50, 100), stats),
                expectation.$(200, 100, IntPair.of(33, 99), stats),
                expectation.$(0, 100, IntPair.of(100, 100), stats),
                expectation.$(0, 101, IntPair.of(100, 100), stats),
                expectation.$(0, 102, IntPair.of(101, 102), stats),
                expectation.$(100, 1, IntPair.of(0, 0), stats),
                expectation.$(100, 2, IntPair.of(1, 2), stats),
                expectation.$(200, 2, IntPair.of(0, 0), stats),
                expectation.$(0, 100, IntPair.of(50, 100), new long[][] {
                        { 0, 2 },
                        { 50, 3 },
                        { 100, 4 },
                        { 150, 5 },
                }),
        };

        // when
        // then
        for (expectation expectation : expectations) {
            assertEquals(
                    "upgrade(actual=" + expectation.actual + ", points=" + expectation.points + ")",
                    expectation.expected,
                    Players.upgrade(expectation.stats, expectation.actual, expectation.points)
            );
        }
    }

    @Test
    public void testBuildDefaultBreedSpells() throws Exception {
        // given
        Breed breed = datacenter.find(Breed.class, 1).get();
        int actualLevel = 1;

        // when
        List<PlayerSpell> spells = Players.buildDefaultBreedSpells(datacenter, breed, actualLevel);

        // then
        assertThat("spells", spells, hasSize(21));
        assertThat("spells with position", spells.stream().filter(PlayerSpell::hasPosition).count(), equalTo(3L));
    }
}