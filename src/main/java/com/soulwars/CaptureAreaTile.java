package com.soulwars;

import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.Color;
import java.util.Collection;
import java.util.stream.Collectors;

class CaptureAreaTile {
    SoulWarsRegion region;
    Collection<LocalPoint> localPoint;
    WorldPoint worldPoint;
    Color color;

    CaptureAreaTile(SoulWarsRegion region, WorldPoint point, Color color, WorldView worldView) {
        this.region = region;
        this.worldPoint = point;
        this.localPoint = WorldPoint.toLocalInstance(worldView, point).stream().map(wp -> LocalPoint.fromWorld(worldView, wp)).collect(Collectors.toList());
        this.color = color;
    }

    void updateColor(Color color) {
        this.color = color;
    }
}