package org.heat.world.items;

public interface WorldWallet {
    int getKamas();
    void setKamas(int kamas);

    default void plusKamas(int kamas) {
        setKamas(getKamas() + kamas);
    }
}
