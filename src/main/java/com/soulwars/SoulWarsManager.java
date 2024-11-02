package com.soulwars;

import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;

import com.soulwars.SoulWarsConfig.TrackingMode;
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

    void init()
    {
        createInfoBoxesFromConfig();
    }
    public void dealtAvatarDamage(int avatarDamage)
    {
        int damageSoFar = resourceToTrackedNumber.getOrDefault(SoulWarsResource.AVATAR_DAMAGE, 0);
        int totalAvatarDamage = damageSoFar + avatarDamage;
        resourceToTrackedNumber.put(SoulWarsResource.AVATAR_DAMAGE, totalAvatarDamage);
        updateInfoBox(SoulWarsResource.AVATAR_DAMAGE, totalAvatarDamage);
    }

    private void createInfoBox(final SoulWarsResource resource, final int goal)
    {
        final boolean isDecrement = config.trackingMode() == TrackingMode.DECREMENT;
        final SoulWarsInfoBox infoBox = new SoulWarsInfoBox(
                itemManager.getImage(resource.getItemId()),
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
        resourceToTrackedNumber.clear();
        infoBoxManager.removeIf(SoulWarsInfoBox.class::isInstance);
    }

    private static class SoulWarsInfoBox extends InfoBox
    {
        private final int goal;
        private int count = 0;
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

            if (hasReachedGoal())
            {
                color = Color.GRAY;
            }

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
