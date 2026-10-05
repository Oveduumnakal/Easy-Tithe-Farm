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
import java.awt.Shape;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Lights up what to click in the scene, colored by what to do there, so the player can follow the run without
 * reading: the plot to click now and the four after it from {@link ActionForecast}, brightest first and fading
 * to 10%; the water barrels when a refill is due; the sacks once 100 or more fruit is carried; and the seed table
 * or sacks for the other errands. Every highlight pulses together at the configured glow speed. Minimal view
 * drops the trail after the current plot. The matching backpack items are lit by {@link TitheInventoryOverlay},
 * since this overlay draws under the interfaces.
 */
class TitheHighlightOverlay extends Overlay
{
	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;
	private final TitheRun run;

	@Inject
	TitheHighlightOverlay(TitheFarmConfig config, TithePlotTracker plotTracker, TitheRun run)
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
		if (!plotTracker.inTitheFarm())
			return null;

		RunSnapshot snapshot = run.snapshot();
		double glow = HighlightStyle.pulse(System.currentTimeMillis(), config.glowSpeed());
		if (refillDue(config, snapshot))
			highlightObjects(graphics, plotTracker.getWaterBarrels(), config.waterColor(), glow);

		boolean bonusDeposit = bonusDeposit(snapshot);
		if (bonusDeposit)
			highlightObjects(graphics, plotTracker.getSacks(), config.depositColor(), glow);

		if (config.highlightNextAction())
		{
			highlightTrail(graphics, snapshot, glow);
			highlightErrand(graphics, snapshot.getAdvice().getAction(), bonusDeposit, glow);
		}

		return null;
	}

	/**
	 * Whether the cans and water barrels should glow: refilling is the next action, or it is between runs and a
	 * can is not full while the refill reminder is on.
	 */
	static boolean refillDue(TitheFarmConfig config, RunSnapshot snapshot)
	{
		boolean refillNext = config.highlightNextAction()
			&& snapshot.getAdvice().getAction() == NextAction.REFILL_WATER;
		boolean topUp = config.waterRefillWarning() && snapshot.isBetweenRuns()
			&& !snapshot.getStatus().isCansFull();
		return refillNext || topUp;
	}

	/** Whether the fruit and the sacks should glow: 100 or more fruit is carried. */
	static boolean bonusDeposit(RunSnapshot snapshot)
	{
		return snapshot.getStatus().getCarried() >= ActionAdvisor.BONUS_BATCH;
	}

	/**
	 * Draws the current plot and the predicted ones after it, fading step by step, each in its action's color. The
	 * plot's whole patch is outlined, whatever grows on it, so every step looks the same shape. Only the current
	 * plot is filled; the ones after it are outlines.
	 */
	private void highlightTrail(Graphics2D graphics, RunSnapshot snapshot, double glow)
	{
		List<ActionAdvisor.Advice> trail = snapshot.getTrail();
		for (int step = trail.size() - 1; step >= 0; step--)
		{
			ActionAdvisor.Advice advice = trail.get(step);
			GameObject plot = snapshot.plotAt(advice.getPlotIndex());
			Color color = colorOf(advice.getAction());
			if (plot == null || color == null)
				continue;

			double strength = HighlightStyle.fade(step) * glow;
			HighlightStyle.draw(graphics, plot.getCanvasTilePoly(), color, strength, step == 0);
		}
	}

	/** Draws the scene object that goes with an errand action: the seed table, or the sacks. */
	private void highlightErrand(Graphics2D graphics, NextAction action, boolean bonusDeposit, double glow)
	{
		switch (action)
		{
			case GET_SEEDS:
				GameObject table = plotTracker.getSeedTable();
				if (table != null)
					highlightObjects(graphics, Collections.singletonList(table), config.plantColor(), glow);

				break;
			case DEPOSIT_FRUIT:
				if (!bonusDeposit)
					highlightObjects(graphics, plotTracker.getSacks(), config.nextActionColor(), glow);

				break;
			default:
				break;
		}
	}

	/** The highlight color for a plot action, or {@code null} for an action that is not drawn on a plot. */
	private Color colorOf(NextAction action)
	{
		switch (action)
		{
			case PLANT_SEED:
				return config.plantColor();
			case WATER_PLANT:
				return config.waterColor();
			case HARVEST:
				return config.harvestColor();
			case CLEAR_DEAD:
				return config.clearColor();
			default:
				return null;
		}
	}

	/** Draws each of the given objects' outlines. */
	private void highlightObjects(Graphics2D graphics, Collection<GameObject> objects, Color color, double strength)
	{
		for (GameObject object : objects)
			HighlightStyle.draw(graphics, outline(object), color, strength);
	}

	/** An object's clickbox, falling back to its convex hull when the clickbox is not available. */
	private static Shape outline(GameObject object)
	{
		Shape clickbox = object.getClickbox();
		return clickbox != null ? clickbox : object.getConvexHull();
	}
}
