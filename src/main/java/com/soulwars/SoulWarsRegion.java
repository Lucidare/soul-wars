package com.soulwars;

import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;

import java.awt.*;

enum SoulWarsRegion {
    WEST_REGION(8493),
    OBELISK_REGION(8749),
    EAST_REGION(9005);

    public final int regionId;

    SoulWarsRegion(final int regionId) {
        this.regionId = regionId;
    }
}

enum SoulWarsTeam {
    NONE("", ItemID.OBELISK, 0, ObjectID.SOUL_OBELISK_40449, Color.WHITE),
    BLUE("blue team", ItemID.BLUE_CAPE_25208, 1, ObjectID.SOUL_OBELISK_40450, Color.BLUE),
    RED("red team", ItemID.RED_CAPE_25207, 2, ObjectID.SOUL_OBELISK_40451, Color.RED);

    final String chatIdentifier;
    final int itemId;
    final int varbitNum;
    final int obeliskId;
    final Color color;


    SoulWarsTeam(final String chatIdentifier, final int itemId, final int varbitNum, final int obeliskId, final Color color) {
        this.chatIdentifier = chatIdentifier;
        this.itemId = itemId;
        this.varbitNum = varbitNum;
        this.obeliskId = obeliskId;
        this.color = color;
    }
}

enum SoulWarsResource {
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
