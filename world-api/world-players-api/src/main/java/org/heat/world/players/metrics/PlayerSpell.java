package org.heat.world.players.metrics;

import com.ankamagames.dofus.datacenter.spells.Spell;
import com.ankamagames.dofus.network.types.game.data.items.SpellItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.heat.world.players.Players;

import java.util.OptionalInt;

import static org.heat.world.players.Players.MIN_SPELL_LEVEL;
import static org.heat.world.players.Players.checkValidSpellLevel;

@Getter
@AllArgsConstructor(staticName = "create")
public final class PlayerSpell {

    final Spell data;
    final int minPlayerLevel;
    byte level;
    @Setter OptionalInt position;

    public static PlayerSpell create(Spell data, int minPlayerLevel, OptionalInt position) {
        return create(data, minPlayerLevel, MIN_SPELL_LEVEL, position);
    }

    public int getId() {
        return data.getId();
    }

    public int getMinPlayerLevel() {
        if (level == Players.MAX_SPELL_LEVEL - 1) {
            return 100 + minPlayerLevel;
        }
        return minPlayerLevel;
    }

    public boolean hasPosition() {
        return this.position.isPresent();
    }

    public boolean hasPosition(int position) {
        return this.position.isPresent() && this.position.getAsInt() == position;
    }

    public void resetLevel() {
        this.level = MIN_SPELL_LEVEL;
    }

    public void setLevel(byte level) {
        this.level = checkValidSpellLevel(level);
    }

    public void setLevel(int level) {
        setLevel((byte) level);
    }

    public void plusLevel(byte level) {
        setLevel(this.level + level);
    }

    public SpellItem toSpellItem() {
        return new SpellItem(
                (short) 63, // TODO(world/players): spell position
                data.getId(),
                level
        );
    }

    @Override
    public int hashCode() {
        return data.getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != PlayerSpell.class) return false;
        PlayerSpell other = (PlayerSpell) obj;
        return other.data.getId() == this.data.getId();
    }

    @Override
    public String toString() {
        return "PlayerSpell(" +
                "id=" + data.getId() +
                ", position=" + position +
                ", level=" + level +
                ')';
    }
}
