package org.heat.world.players;

import com.ankamagames.dofus.datacenter.spells.Spell;
import com.ankamagames.dofus.network.types.game.data.items.SpellItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.OptionalInt;

import static org.heat.world.players.Players.MIN_SPELL_LEVEL;
import static org.heat.world.players.Players.checkValidSpellLevel;

@Getter
@AllArgsConstructor(staticName = "create")
public final class PlayerSpell {

    final Spell data;
    byte level;
    @Setter OptionalInt position;

    public static PlayerSpell create(Spell data) {
        return create(data, MIN_SPELL_LEVEL, OptionalInt.empty());
    }

    public int getId() {
        return data.getId();
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
                (short) position.orElse(-1),
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
