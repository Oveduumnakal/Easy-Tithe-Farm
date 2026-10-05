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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Lights up backpack items to match the scene highlights, drawn above the interfaces so the inventory panel does
 * not cover them: every watering can that is not full while the water barrels glow; the fruit once 100 or more
 * is carried; the seed to plant or the first filled watering can for the current plot action; and any energy or
 * stamina potion when run energy is low. Each item is traced along its own outline, the way Stockpile marks
 * tracked items. All but the energy warning pulse with the scene highlights. Minimal view drops the seed and can
 * outlines.
 */
class TitheInventoryOverlay extends WidgetItemOverlay
{
	private final Client client;
	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;
	private final TitheRun run;
	private final ItemManager itemManager;

	private boolean canBoxed;

	@Inject
	TitheInventoryOverlay(Client client, TitheFarmConfig config, TithePlotTracker plotTracker, TitheRun run,
		ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.plotTracker = plotTracker;
		this.run = run;
		this.itemManager = itemManager;
		showOnInventory();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		canBoxed = false;
		return super.render(graphics);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plotTracker.inTitheFarm())
			return;

		RunSnapshot snapshot = run.snapshot();
		if (snapshot.getStatus().isEnergyLow(config.lowEnergyPercent()) && TitheFarmIds.isRunRestore(itemId))
		{
			drawOutline(graphics, itemId, widgetItem, config.warningColor(), 1);
			return;
		}

		Color color = colorOf(snapshot, itemId);
		if (color != null)
		{
			double glow = HighlightStyle.pulse(System.currentTimeMillis(), config.glowSpeed());
			drawOutline(graphics, itemId, widgetItem, color, glow);
		}
	}

	/**
	 * Traces an item's outline in a color at a strength. The outline image is fetched opaque, so the item
	 * manager's cache holds one image per item and color, and the color's alpha and the strength are applied
	 * while drawing.
	 */
	private void drawOutline(Graphics2D graphics, int itemId, WidgetItem widgetItem, Color color, double strength)
	{
		Rectangle bounds = widgetItem.getCanvasBounds();
		float alpha = (float) (color.getAlpha() / 255.0 * Math.max(0, Math.min(1, strength)));
		if (bounds == null || alpha <= 0)
			return;

		Color opaque = new Color(color.getRed(), color.getGreen(), color.getBlue());
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), opaque);
		Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		graphics.drawImage(outline, bounds.x, bounds.y, null);
		graphics.setComposite(original);
	}

	/** The color to light an item in, or {@code null} to leave it unlit. */
	private Color colorOf(RunSnapshot snapshot, int itemId)
	{
		int gricollerCharges = client.getVarbitValue(TitheFarmIds.GRICOLLER_CHARGES_VARBIT);
		if (TitheHighlightOverlay.refillDue(config, snapshot) && WaterTracker.needsFill(itemId, gricollerCharges))
			return config.waterColor();

		if (TitheHighlightOverlay.bonusDeposit(snapshot) && TitheFarmIds.isFruit(itemId))
			return config.depositColor();

		if (!config.highlightNextAction() || config.minimalView())
			return null;

		NextAction action = snapshot.getAdvice().getAction();
		if (action == NextAction.PLANT_SEED && TitheFarmIds.isSeed(itemId))
			return config.plantColor();

		if (action == NextAction.WATER_PLANT && !canBoxed && isFilledCan(itemId))
		{
			canBoxed = true;
			return config.waterColor();
		}

		return null;
	}

	/** Whether an item id is a watering can that can still pour — a filled regular can or Gricoller's can. */
	private static boolean isFilledCan(int itemId)
	{
		return TitheFarmIds.regularCanCharges(itemId) > 0 || itemId == TitheFarmIds.GRICOLLER_CAN;
	}
}
