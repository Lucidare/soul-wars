package com.soulwars;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.ItemID;

public enum SoulWarsResource {
    FRAGMENTS_SACRIFICED("Fragments Sacrificed", ItemID.SOUL_FRAGMENT_25201),
    BONES_BURIED("Bones Buried", ItemID.BONES_25199),
    AVATAR_DAMAGE("Avatar Damage", ItemID.LIL_CREATOR),
    CAPTURES("Number of Captures", ItemID.OBELISK);

    @Getter(AccessLevel.PACKAGE)
    private final String name;
    @Getter(AccessLevel.PACKAGE)
    private final int itemId;

    SoulWarsResource(final String name, final int itemId)
    {
        this.name = name;
        this.itemId = itemId;
    }
}
