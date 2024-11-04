/*
 * Copyright (c) 2018, TheLonelyDev <https://github.com/TheLonelyDev>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2022, Trevor <https://github.com/TrevorMDev>
 * Copyright (c) 2024, Lucidare <https://github.com/Lucidare>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.soulwars;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;

@Slf4j
public class CaptureAreaOverlay extends Overlay {

    @Inject
    private final SoulWarsManager soulWarsManager;
    @Inject
    private final Client client;
    @Inject
    private final SoulWarsConfig config;

    @Inject
    public CaptureAreaOverlay(SoulWarsManager soulWarsManager, Client client, SoulWarsConfig config) {
        this.soulWarsManager = soulWarsManager;
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics2D) {
        if (soulWarsManager.regionToCaptureAreaTiles.isEmpty() || !config.highlightCaptureAreas()) {
            return null;
        }

        Stroke stroke = new BasicStroke((float) 0);
        for (final ArrayList<CaptureAreaTile> captureAreaTiles : soulWarsManager.regionToCaptureAreaTiles.values()) {
            for (final CaptureAreaTile tile: captureAreaTiles) {
                drawTile(graphics2D, tile, stroke);
            }
        }

        return null;
    }

    private void drawTile(Graphics2D graphics2D, CaptureAreaTile captureAreaTile, Stroke borderStroke) {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        WorldPoint playerLocation = WorldPoint.fromLocalInstance(client, player.getLocalLocation());

        if (captureAreaTile.worldPoint.distanceTo(playerLocation) >= config.maxDrawDistance()) {
            return;
        }

        for (LocalPoint lp: captureAreaTile.localPoint) {
            if (lp == null) {
                return;
            }

            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly != null) {
                Color color = captureAreaTile.color;
                Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), config.fillOpacity());
                OverlayUtil.renderPolygon(graphics2D, poly, color, fillColor, borderStroke);
            }
        }
    }
}
