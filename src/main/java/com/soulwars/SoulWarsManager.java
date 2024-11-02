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
import java.util.Optional;

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
    private boolean inSoulWarsGame = false;

    private final EnumMap<SoulWarsResource, SoulWarsInfoBox> resourceToInfoBox = new EnumMap<>(SoulWarsResource.class);
    private final EnumMap<SoulWarsResource, Integer> resourceToTrackedNumber = new EnumMap<>(SoulWarsResource.class);

    // 2167, 2893 | 2157, 2893 | 2167, 2903 | 2157, 2903 blue graveyard
    // 2248, 2930 | 2258, 2930 | 2248, 2920 | 2258, 2920 red graveyard

    // 2199 - 2214 - 2919 - 2904 Soul obelisk

    private WorldArea west_graveyard = new WorldArea(2157, 2893, 10, 10, 0);
    private WorldArea east_graveyard = new WorldArea(2248, 2920, 10, 10, 0);
    private WorldArea obelisk = new WorldArea(2199, 2904, 16, 16, 0);

    void init()
    {
        inSoulWarsGame = true;
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
        inSoulWarsGame = false;
        resourceToTrackedNumber.clear();
        infoBoxManager.removeIf(SoulWarsInfoBox.class::isInstance);
    }

//2024-11-02 15:03:53 EDT [Client] INFO  com.soulwars.SoulWarsPlugin - <col=ff3232>The red team has taken control of the eastern graveyard!</col>
//            2024-11-02 15:03:53 EDT [Client] INFO  com.soulwars.SoulWarsManager - <col=ff3232>The red team has taken control of the eastern graveyard!</col>
//            2024-11-02 15:03:56 EDT [Client] INFO  com.soulwars.SoulWarsPlugin - <col=3366ff>The blue team has taken control of the Soul Obelisk!</col>
//            2024-11-02 15:03:56 EDT [Client] INFO  com.soulwars.SoulWarsManager - <col=3366ff>The blue team has taken control of the Soul Obelisk!</col>
//            2024-11-02 15:04:01 EDT [Client] INFO  com.soulwars.SoulWarsPlugin - <col=3366ff>The blue team has taken control of the western graveyard!</col>
//            2024-11-02 15:04:01 EDT [Client] INFO  com.soulwars.SoulWarsManager - <col=3366ff>The blue team has taken contr
//2024-11-02 15:07:01 EDT [Client] INFO  com.soulwars.SoulWarsPlugin - You charge the Soul Obelisk with soul fragments and weaken the enemy avatar.
//2024-11-02 15:07:01 EDT [Client] INFO  com.soulwars.SoulWarsManager - You charge the Soul Obelisk
    // red east
    // blue west
    void parseChatMessage(final String chatMessage, final Optional<WorldPoint> location, final SoulWarsTeam team)
    {
        if (!inSoulWarsGame)
        {
            return;
        }

        if (chatMessage.startsWith("You charge the Soul Obelisk with soul fragments"))
        {
            updateInfoBox(SoulWarsResource.FRAGMENTS_SACRIFICED, 24);
        } else if (chatMessage.startsWith(team.getPrefix())) {
            if (location.isEmpty()) {
                return;
            }

            if (chatMessage.contains("eastern graveyard") && location.get().isInArea(east_graveyard)) {
                increaseCaptures();
            } else if (chatMessage.contains("Soul Obelisk") && location.get().isInArea(obelisk)) {
                increaseCaptures();
            } else if (chatMessage.contains("western graveyard") && location.get().isInArea(west_graveyard)) {
                increaseCaptures();
            }
        }
    }

    private void increaseCaptures() {
        int capturesSoFar = resourceToTrackedNumber.getOrDefault(SoulWarsResource.CAPTURES, 0);
        int totalCaptures = capturesSoFar + 1;
        resourceToTrackedNumber.put(SoulWarsResource.CAPTURES, totalCaptures);
        updateInfoBox(SoulWarsResource.CAPTURES, totalCaptures);
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
