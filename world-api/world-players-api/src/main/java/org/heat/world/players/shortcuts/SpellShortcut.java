package org.heat.world.players.shortcuts;

import com.ankamagames.dofus.network.types.game.shortcut.Shortcut;
import com.ankamagames.dofus.network.types.game.shortcut.ShortcutSpell;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true, of = "spellId")
public final class SpellShortcut extends PlayerShortcut {
    @Getter private final short spellId;

    public SpellShortcut(int playerId, int slot, short spellId) {
        super(playerId, slot);
        this.spellId = spellId;
    }

    @Override
    public Shortcut toShortcut() {
        return new ShortcutSpell(getSlot(), spellId);
    }

    @Override
    public String toString() {
        return "SpellShortcut(" +
                "player_id=" + getPlayerId() +
                ", slot=" + getSlot() +
                ", spellId=" + spellId +
                ')';
    }
}
