/*
 * Copyright (c) 2026, Oveduumnakal
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
package com.oveduumnakal.tithefarm;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;

import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws the numbered planting route over the plots: each plot in the run is outlined and labelled with its
 * order in the snake, so the player can plant, then water, in a fixed sequence. The order comes from the pure
 * {@link PlantRoute}; this overlay only maps its ordered tiles back to the scene objects and paints them.
 */
class TitheRouteOverlay extends Overlay
{
	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;

	@Inject
	TitheRouteOverlay(TitheFarmConfig config, TithePlotTracker plotTracker)
	{
		this.config = config;
		this.plotTracker = plotTracker;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showRoute() || !plotTracker.inTitheFarm())
			return null;

		List<GameObject> route = TitheRoutePlots.ordered(plotTracker.getPlots(), config.cropCount());
		for (int i = 0; i < route.size(); i++)
			drawPlot(graphics, route.get(i), Integer.toString(i + 1));

		return null;
	}

	/** Outlines one plot and paints its route number at the tile centre. */
	private void drawPlot(Graphics2D graphics, GameObject plot, String label)
	{
		Polygon poly = plot.getCanvasTilePoly();
		if (poly != null)
			OverlayUtil.renderPolygon(graphics, poly, config.routeColor());

		Point text = plot.getCanvasTextLocation(graphics, label, 0);
		if (text != null)
			OverlayUtil.renderTextLocation(graphics, text, label, config.routeColor());
	}
}
