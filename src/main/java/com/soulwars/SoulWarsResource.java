package com.soulwars;

import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;

import java.awt.*;

public enum SoulWarsResource {
    FRAGMENTS_SACRIFICED("Fragments Sacrificed", ItemID.SOUL_FRAGMENT_25201),
    BONES_BURIED("Bones Buried", ItemID.BONES_25199),
    AVATAR_DAMAGE("Avatar Damage", ItemID.LIL_CREATOR),
    CAPTURES("Areas Captured", ItemID.OBELISK);

    final String name;
    final int itemId;

    SoulWarsResource(final String name, final int itemId) {
        this.name = name;
        this.itemId = itemId;
    }
}

enum SoulWarsTeam {
    NONE("", ItemID.OBELISK, 0, ObjectID.SOUL_OBELISK_40449, Color.GRAY),
    BLUE("<col=3366ff>The blue team", ItemID.BLUE_CAPE_25208, 1, ObjectID.SOUL_OBELISK_40450, Color.BLUE),
    RED("<col=ff3232>The red team", ItemID.RED_CAPE_25207, 2, ObjectID.SOUL_OBELISK_40451, Color.RED);

    final String prefix;
    final int itemId;
    final int varbitNum;
    final int obeliskId;
    final Color color;


    SoulWarsTeam(final String prefix, final int itemId, final int varbitNum, final int obeliskId, final Color color) {
        this.prefix = prefix;
        this.itemId = itemId;
        this.varbitNum = varbitNum;
        this.obeliskId = obeliskId;
        this.color = color;
    }
}