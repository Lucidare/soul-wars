package com.soulwars;

import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;

import com.soulwars.SoulWarsConfig.TrackingMode;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;

@Slf4j
@Singleton
public class SoulWarsManager {

    @Inject
    private SoulWarsPlugin plugin;
    @Inject
    private SoulWarsConfig config;
    @Inject
    private ItemManager itemManager;
    @Inject
    private InfoBoxManager infoBoxManager;

    private final EnumMap<SoulWarsResource, SoulWarsInfoBox> resourceToInfoBox = new EnumMap<>(SoulWarsResource.class);
    private final EnumMap<SoulWarsResource, Integer> resourceToTrackedNumber = new EnumMap<>(SoulWarsResource.class);

    private final WorldArea west_graveyard = new WorldArea(2157, 2893, 11, 11, 0);
    private final WorldArea east_graveyard = new WorldArea(2248, 2920, 11, 11, 0);
    private final WorldArea obelisk = new WorldArea(2199, 2904, 16, 16, 0);

    private SoulWarsTeam team = SoulWarsTeam.NONE;
    private int inventoryFragments;

    void init(SoulWarsTeam soulWarsTeam)
    {
        team = soulWarsTeam;
        inventoryFragments = 0;
        createInfoBoxesFromConfig();
    }

    private void createInfoBox(final SoulWarsResource resource, final int goal)
    {
        final boolean isDecrement = config.trackingMode() == TrackingMode.DECREMENT;

        int itemId = resource.getItemId();

        // update captures image to be team cape instead of default obe
        if (resource == SoulWarsResource.CAPTURES) {
            if (team == SoulWarsTeam.RED)
            {
                itemId = SoulWarsTeam.RED.getItemId();
            } else if (team == SoulWarsTeam.BLUE) {
                itemId = SoulWarsTeam.BLUE.getItemId();
            }
        }
        final SoulWarsInfoBox infoBox = new SoulWarsInfoBox(
                itemManager.getImage(itemId),
                plugin,
                goal,
                isDecrement,
                resource.getName()
        );

        resourceToInfoBox.put(resource, infoBox);
        infoBoxManager.addInfoBox(infoBox);
        updateInfoBox(resource, resourceToTrackedNumber.getOrDefault(resource,0));
    }

    private void createInfoBoxesFromConfig()
    {
        if (config.showFragmentsSacrificed()) {
            createInfoBox(SoulWarsResource.FRAGMENTS_SACRIFICED, config.fragmentsSacrificed());
        }
        if (config.showAvatarDamage()) {
            createInfoBox(SoulWarsResource.AVATAR_DAMAGE, config.avatarDamage());
        }
        if (config.showBonesBuried()) {
            createInfoBox(SoulWarsResource.BONES_BURIED, config.bonesBuried());
        }
        if (config.showCaptures()) {
            createInfoBox(SoulWarsResource.CAPTURES, config.captures());
        }
    }

    // for config changes without touching game values
    public void updateInfoBoxes()
    {
        infoBoxManager.removeIf(SoulWarsInfoBox.class::isInstance);
        createInfoBoxesFromConfig();
    }
    private void updateInfoBox(final SoulWarsResource resource, final int count)
    {
        final SoulWarsInfoBox infoBox = resourceToInfoBox.get(resource);

        if (infoBox == null)
        {
            return;
        }

        infoBox.updateCount(count);

        if (config.removedWhenGoalReached() && infoBox.hasReachedGoal())
        {
            infoBoxManager.removeInfoBox(infoBox);
            resourceToInfoBox.remove(resource);
        }
    }

    public void reset()
    {
        team = SoulWarsTeam.NONE;
        inventoryFragments = 0;
        resourceToTrackedNumber.clear();
        infoBoxManager.removeIf(SoulWarsInfoBox.class::isInstance);
    }

    void parseChatMessage(final String chatMessage, final WorldPoint location)
    {
        if (team == SoulWarsTeam.NONE)
        {
            return;
        }

        if (chatMessage.startsWith("You charge the Soul Obelisk with soul fragments"))
        {
            increaseFragmentsSacrificed(inventoryFragments);
        } else if (chatMessage.contains("You bury the bones")) {
            increaseBonesBuried();
        } else if (chatMessage.startsWith(team.getPrefix())) {
            if (location == null) {
                return;
            }

            if (chatMessage.contains("eastern graveyard") && location.isInArea(east_graveyard)) {
                increaseCaptures();
            } else if (chatMessage.contains("Soul Obelisk") && location.isInArea(obelisk)) {
                increaseCaptures();
            } else if (chatMessage.contains("western graveyard") && location.isInArea(west_graveyard)) {
                increaseCaptures();
            }
        }
    }

    private void increaseCaptures()
    {
        int capturesSoFar = resourceToTrackedNumber.getOrDefault(SoulWarsResource.CAPTURES, 0);
        int totalCaptures = capturesSoFar + 1;
        resourceToTrackedNumber.put(SoulWarsResource.CAPTURES, totalCaptures);
        updateInfoBox(SoulWarsResource.CAPTURES, totalCaptures);
    }

    private void increaseBonesBuried()
    {
        int bonesBuriedSoFar = resourceToTrackedNumber.getOrDefault(SoulWarsResource.BONES_BURIED, 0);
        int totalBonesBuried = bonesBuriedSoFar + 1;
        resourceToTrackedNumber.put(SoulWarsResource.BONES_BURIED, totalBonesBuried);
        updateInfoBox(SoulWarsResource.BONES_BURIED, totalBonesBuried);
    }

    private void increaseFragmentsSacrificed(final int numFragments)
    {
        int fragmentsSacrificedSoFar = resourceToTrackedNumber.getOrDefault(SoulWarsResource.FRAGMENTS_SACRIFICED, 0);
        int totalFragmentsSacrificed = fragmentsSacrificedSoFar + numFragments;
        resourceToTrackedNumber.put(SoulWarsResource.FRAGMENTS_SACRIFICED, totalFragmentsSacrificed);
        updateInfoBox(SoulWarsResource.FRAGMENTS_SACRIFICED, totalFragmentsSacrificed);
    }

    // needed for when avatar is low strength and can't sacrifice all fragments
    private void decreaseFragmentsSacrificed(final int numFragments)
    {
        int fragmentsSacrificedSoFar = resourceToTrackedNumber.getOrDefault(SoulWarsResource.FRAGMENTS_SACRIFICED, 0);
        int totalFragmentsSacrificed = fragmentsSacrificedSoFar - numFragments;
        resourceToTrackedNumber.put(SoulWarsResource.FRAGMENTS_SACRIFICED, totalFragmentsSacrificed);
        updateInfoBox(SoulWarsResource.FRAGMENTS_SACRIFICED, totalFragmentsSacrificed);
    }

    public void updateFragmentInInventoryCount(final int numFragments)
    {
        // num fragments decrease so sacrificed but potentially some remain due to low avatar strength
        if (inventoryFragments > numFragments) {
            decreaseFragmentsSacrificed(numFragments);
        }
        inventoryFragments = numFragments;
    }

    public void dealtAvatarDamage(int avatarDamage)
    {
        int damageSoFar = resourceToTrackedNumber.getOrDefault(SoulWarsResource.AVATAR_DAMAGE, 0);
        int totalAvatarDamage = damageSoFar + avatarDamage;
        resourceToTrackedNumber.put(SoulWarsResource.AVATAR_DAMAGE, totalAvatarDamage);
        updateInfoBox(SoulWarsResource.AVATAR_DAMAGE, totalAvatarDamage);
    }

    private static class SoulWarsInfoBox extends InfoBox
    {
        private final int goal;
        private int count;
        private Color color = Color.WHITE;
        private final boolean isDecrement;
        private String text;

        private SoulWarsInfoBox(
                final BufferedImage bufferedImage,
                final SoulWarsPlugin plugin,
                final int goal,
                final boolean isDecrement,
                final String tooltip)
        {
            super(bufferedImage, plugin);
            setPriority(InfoBoxPriority.LOW);
            this.count = 0;
            this.goal = goal;
            this.isDecrement = isDecrement;
            this.text = calculateText();
            this.setTooltip(tooltip);
        }

        @Override
        public String getText()
        {
            return text;
        }

        @Override
        public Color getTextColor()
        {
            return color;
        }
        private void updateCount(final int count)
        {
            this.count = count;
            color = hasReachedGoal() ? Color.GRAY : Color.WHITE;
            text = calculateText();
        }

        private String calculateText() {
            return isDecrement ? String.valueOf(this.goal-this.count) : String.valueOf(this.count);
        }

        private boolean hasReachedGoal()
        {
            return count >= goal;
        }
    }
}
