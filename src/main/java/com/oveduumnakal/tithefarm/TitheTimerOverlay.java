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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;

import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws a countdown over every plant waiting for water — the time left before it dies — raised above the plot
 * so it never collides with the route number. Green while there is plenty of time, yellow inside the warning
 * window plus a little, red inside the warning window itself. In minimal view only the targeted plant's timer
 * is drawn.
 */
class TitheTimerOverlay extends Overlay
{
	/** Height above the plot, in local units, where the countdown is drawn. */
	private static final int TEXT_HEIGHT = 160;

	/** Extra seconds before the warning window where the countdown turns yellow. */
	private static final int CAUTION_SECONDS = 15;

	private static final Color SAFE = new Color(120, 255, 120);
	private static final Color CAUTION = Color.YELLOW;

	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;
	private final TitheRun run;

	@Inject
	TitheTimerOverlay(TitheFarmConfig config, TithePlotTracker plotTracker, TitheRun run)
	{
		this.config = config;
		this.plotTracker = plotTracker;
		this.run = run;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showTimers() || !plotTracker.inTitheFarm())
			return null;

		RunSnapshot snapshot = run.snapshot();
		GameObject target = snapshot.getTargetPlot();
		List<GameObject> route = snapshot.getRoute();
		List<PlotInfo> plots = snapshot.getPlots();
		for (int i = 0; i < route.size(); i++)
		{
			int ticks = plots.get(i).ticksUntilDeath();
			if (ticks < 0 || (config.minimalView() && route.get(i) != target))
				continue;

			String text = TitheTime.format(ticks);
			Point location = route.get(i).getCanvasTextLocation(graphics, text, TEXT_HEIGHT);
			if (location != null)
				OverlayUtil.renderTextLocation(graphics, location, text, colorFor(ticks));
		}

		return null;
	}

	/** The countdown color for the ticks left. */
	private Color colorFor(int ticks)
	{
		int seconds = TitheTime.seconds(ticks);
		if (seconds <= config.deathWarnSeconds())
			return config.warningColor();

		return seconds <= config.deathWarnSeconds() + CAUTION_SECONDS ? CAUTION : SAFE;
	}
}
