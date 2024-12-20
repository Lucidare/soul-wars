package com.soulwars;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;

import com.soulwars.SoulWarsConfig.TrackingMode;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.stream.Collectors;

import static net.runelite.api.NpcID.AVATAR_OF_CREATION_10531;
import static net.runelite.api.NpcID.AVATAR_OF_DESTRUCTION_10532;

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
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private Notifier notifier;

    private final EnumMap<SoulWarsResource, SoulWarsInfoBox> resourceToInfoBox = new EnumMap<>(SoulWarsResource.class);
    private final EnumMap<SoulWarsResource, Integer> resourceToTrackedNumber = new EnumMap<>(SoulWarsResource.class);

    private final WorldArea blue_spawn = new WorldArea(2136, 2900, 8, 11, 0);
    private final WorldArea west_graveyard = new WorldArea(2157, 2893, 11, 11, 0);
    private final WorldArea obelisk = new WorldArea(2199, 2904, 16, 16, 0);
    private final WorldArea east_graveyard = new WorldArea(2248, 2920, 11, 11, 0);
    private final WorldArea red_spawn = new WorldArea(2271, 2914, 8, 11, 0);
    static final ImmutableSet<Integer> SOUL_WARS_ARENA_REGIONS = ImmutableSet.of(
            SoulWarsRegion.WEST_REGION.regionId, SoulWarsRegion.OBELISK_REGION.regionId, SoulWarsRegion.EAST_REGION.regionId
    );
    static final ImmutableSet<Integer> AVATARS_IDS = ImmutableSet.of(
            AVATAR_OF_CREATION_10531, AVATAR_OF_DESTRUCTION_10532
    );
    static final int VARBIT_SOUL_WARS = 3815;
    static final int VARBIT_SOUL_WARS_ACTIVITY = 9794;
    private static final double MAX_ACTIVITY = 800.0;

    private SoulWarsTeam team = SoulWarsTeam.NONE;
    private SoulWarsTeam west_graveyard_control = SoulWarsTeam.NONE;
    private SoulWarsTeam obelisk_control = SoulWarsTeam.NONE;
    private SoulWarsTeam east_graveyard_control = SoulWarsTeam.NONE;
    private int inventoryFragments;
    private boolean currentIsActive = true;
    EnumMap<SoulWarsRegion, ArrayList<CaptureAreaTile>> regionToCaptureAreaTiles = new EnumMap<>(SoulWarsRegion.class);

    void init(SoulWarsTeam soulWarsTeam)
    {
        team = soulWarsTeam;
        currentIsActive = true;
        inventoryFragments = 0;
        regionToCaptureAreaTiles.clear();
        createInfoBoxesFromConfig();
    }

    private void createInfoBox(final SoulWarsResource resource, final int goal)
    {
        final boolean isDecrement = config.trackingMode() == TrackingMode.DECREMENT;

        // use team cape for captures
        int itemId = resource == SoulWarsResource.CAPTURES ? team.itemId : resource.itemId;

        final SoulWarsInfoBox infoBox = new SoulWarsInfoBox(
                itemManager.getImage(itemId),
                plugin,
                goal,
                isDecrement,
                resource.name
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

    public void highlightCaptureAreas(int[] loadedRegionIds, WorldView worldView) {
        Color westColor, obeliskColor, eastColor;

        if (west_graveyard_control.color.equals(Color.RED)) {
            westColor = config.redTeamColor();
        } else if (west_graveyard_control.color.equals(Color.BLUE)) {
            westColor = config.blueTeamColor();
        } else {
            westColor = west_graveyard_control.color;
        }
        if (obelisk_control.color.equals(Color.RED)) {
            obeliskColor = config.redTeamColor();
        } else if (obelisk_control.color.equals(Color.BLUE)) {
            obeliskColor = config.blueTeamColor();
        } else {
            obeliskColor = obelisk_control.color;
        }
        if (east_graveyard_control.color.equals(Color.RED)) {
            eastColor = config.redTeamColor();
        } else if (east_graveyard_control.color.equals(Color.BLUE)) {
            eastColor = config.blueTeamColor();
        } else {
            eastColor = east_graveyard_control.color;
        }

        if (ArrayUtils.contains(loadedRegionIds, SoulWarsRegion.WEST_REGION.regionId)) {
            regionToCaptureAreaTiles.put(SoulWarsRegion.WEST_REGION, (ArrayList<CaptureAreaTile>) west_graveyard.toWorldPointList().stream().map(point -> new CaptureAreaTile(SoulWarsRegion.WEST_REGION, point, westColor, worldView)).collect(Collectors.toList()));
        }
        if (ArrayUtils.contains(loadedRegionIds, SoulWarsRegion.OBELISK_REGION.regionId)) {
            regionToCaptureAreaTiles.put(SoulWarsRegion.OBELISK_REGION, (ArrayList<CaptureAreaTile>) obelisk.toWorldPointList().stream().map(point -> new CaptureAreaTile(SoulWarsRegion.OBELISK_REGION, point, obeliskColor, worldView)).collect(Collectors.toList()));
        }
        if (ArrayUtils.contains(loadedRegionIds, SoulWarsRegion.EAST_REGION.regionId)) {
            regionToCaptureAreaTiles.put(SoulWarsRegion.EAST_REGION, (ArrayList<CaptureAreaTile>) east_graveyard.toWorldPointList().stream().map(point -> new CaptureAreaTile(SoulWarsRegion.EAST_REGION, point, eastColor, worldView)).collect(Collectors.toList()));
        }
    }

    void updateCaptureAreas(SoulWarsRegion region, Color color) {
        ArrayList<CaptureAreaTile> currentTiles = regionToCaptureAreaTiles.get(region);
        for (CaptureAreaTile tile:currentTiles) {
            tile.updateColor(color);
        }
        regionToCaptureAreaTiles.put(region, currentTiles);
    }

    public void reset()
    {
        team = SoulWarsTeam.NONE;
        currentIsActive = true;
        west_graveyard_control = SoulWarsTeam.NONE;
        obelisk_control = SoulWarsTeam.NONE;
        east_graveyard_control = SoulWarsTeam.NONE;
        regionToCaptureAreaTiles.clear();
        inventoryFragments = 0;
        resourceToTrackedNumber.clear();
        infoBoxManager.removeIf(SoulWarsInfoBox.class::isInstance);
    }

    void parseChatMessage(final String chatMessage, final WorldPoint location)
    {
        if (team == SoulWarsTeam.NONE || location == null)
        {
            return;
        }

        if (chatMessage.contains("You charge the Soul Obelisk with soul fragments"))
        {
            increaseFragmentsSacrificed(inventoryFragments);
        } else if (chatMessage.contains("You bury the bones")) {
            increaseBonesBuried();
        } else if (chatMessage.contains(SoulWarsTeam.RED.chatIdentifier)) {
            if (chatMessage.contains("eastern graveyard")) {
                east_graveyard_control = SoulWarsTeam.RED;
                updateCaptureAreas(SoulWarsRegion.EAST_REGION, config.redTeamColor());
                if (team == SoulWarsTeam.RED && location.isInArea(east_graveyard)) {
                    increaseCaptures();
                }
            } else if (chatMessage.contains("Soul Obelisk")) {
                obelisk_control = SoulWarsTeam.RED;
                updateCaptureAreas(SoulWarsRegion.OBELISK_REGION, config.redTeamColor());
                if (team == SoulWarsTeam.RED && location.isInArea(obelisk)) {
                    increaseCaptures();
                }
            } else if (chatMessage.contains("western graveyard")) {
                west_graveyard_control = SoulWarsTeam.RED;
                updateCaptureAreas(SoulWarsRegion.WEST_REGION, config.redTeamColor());
                if (team == SoulWarsTeam.RED && location.isInArea(west_graveyard)) {
                    increaseCaptures();
                }
            }
        } else if (chatMessage.contains(SoulWarsTeam.BLUE.chatIdentifier)) {
            if (chatMessage.contains("eastern graveyard")) {
                east_graveyard_control = SoulWarsTeam.BLUE;
                updateCaptureAreas(SoulWarsRegion.EAST_REGION, config.blueTeamColor());
                if (team == SoulWarsTeam.BLUE && location.isInArea(east_graveyard)) {
                    increaseCaptures();
                }
            } else if (chatMessage.contains("Soul Obelisk")) {
                obelisk_control = SoulWarsTeam.BLUE;
                updateCaptureAreas(SoulWarsRegion.OBELISK_REGION, config.blueTeamColor());
                if (team == SoulWarsTeam.BLUE && location.isInArea(obelisk)) {
                    increaseCaptures();
                }
            } else if (chatMessage.contains("western graveyard")) {
                west_graveyard_control = SoulWarsTeam.BLUE;
                updateCaptureAreas(SoulWarsRegion.WEST_REGION, config.blueTeamColor());
                if (team == SoulWarsTeam.BLUE && location.isInArea(west_graveyard)) {
                    increaseCaptures();
                }
            }
        }
    }

    public boolean shouldSacrificeObelisk() {
        return obelisk_control == team;
    }

    public boolean shouldBuryBone(final WorldPoint location) {
        if (location == null) {
            return false;
        }
        if (location.isInArea(west_graveyard)) {
            return west_graveyard_control == team;
        } else if (location.isInArea(east_graveyard)) {
            return east_graveyard_control == team;
        } else {
            return location.isInArea(red_spawn) || location.isInArea(blue_spawn);
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

    public void updateActivityBar(final int activityValue)
    {
        int threshold = config.activityNotifThreshold();
        boolean isActive = activityValue/MAX_ACTIVITY > threshold/100.0;
        if (currentIsActive != isActive) {
            if (config.shouldNotifyActivity() && !isActive) {
                notifier.notify("Soul Wars activity bar dropping below " + threshold + "%");
            }
            currentIsActive = isActive;
        }
    }

    public void notifyEnterGame() {
        if (config.shouldNotifyEnterGame()) {
            notifier.notify("Entered Soul Wars");
        }
    }

    public void notifyLeaveGame() {
        if (config.shouldNotifyLeaveGame()) {
            notifier.notify("Left Soul Wars");
        }
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
