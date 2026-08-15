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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntPredicate;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Highlights the single next thing to click, from {@link ActionAdvisor}: the plot to plant, water, or harvest;
 * the water barrel to refill at; or the seed table to restock from. For a plant or water action the matching
 * inventory item (a seed, or a filled watering can) is boxed too, so the player knows what to click with.
 */
class TitheHighlightOverlay extends Overlay
{
	private final Client client;
	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;
	private final WaterTracker waterTracker;

	@Inject
	TitheHighlightOverlay(Client client, TitheFarmConfig config, TithePlotTracker plotTracker,
		WaterTracker waterTracker)
	{
		this.client = client;
		this.config = config;
		this.plotTracker = plotTracker;
		this.waterTracker = waterTracker;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightNextAction() || !plotTracker.inTitheFarm())
			return null;

		List<GameObject> route = TitheRoutePlots.ordered(plotTracker.getPlots(), config.cropCount());
		List<TithePlotState> states = new ArrayList<>();
		for (GameObject plot : route)
			states.add(plotTracker.stateOf(plot));

		boolean hasSeeds = inventoryHas(TitheFarmIds::isSeed);
		int water = waterTracker.availableCharges();
		ActionAdvisor.Advice advice = ActionAdvisor.decide(states, hasSeeds, water);
		Color color = config.nextActionColor();
		switch (advice.getAction())
		{
			case WATER_PLANT:
				highlightPlot(graphics, route.get(advice.getPlotIndex()), "Water", color);
				highlightInventory(graphics, TitheHighlightOverlay::isFilledCan, color);
				break;
			case PLANT_SEED:
				highlightPlot(graphics, route.get(advice.getPlotIndex()), "Plant", color);
				highlightInventory(graphics, TitheFarmIds::isSeed, color);
				break;
			case HARVEST:
				highlightPlot(graphics, route.get(advice.getPlotIndex()), "Harvest", color);
				break;
			case REFILL_WATER:
				highlightObjects(graphics, plotTracker.getWaterBarrels(), "Refill", config.warningColor());
				break;
			case GET_SEEDS:
				highlightSeedTable(graphics, color);
				break;
			default:
				break;
		}

		return null;
	}

	/** Outlines a plot's tile and labels it with the action word. */
	private void highlightPlot(Graphics2D graphics, GameObject plot, String label, Color color)
	{
		Polygon poly = plot.getCanvasTilePoly();
		if (poly != null)
			OverlayUtil.renderPolygon(graphics, poly, color);

		Point text = plot.getCanvasTextLocation(graphics, label, 0);
		if (text != null)
			OverlayUtil.renderTextLocation(graphics, text, label, color);
	}

	/** Outlines each of the given objects by convex hull and labels the first with the action word. */
	private void highlightObjects(Graphics2D graphics, Collection<GameObject> objects, String label, Color color)
	{
		boolean labelled = false;
		for (GameObject object : objects)
		{
			Shape hull = object.getConvexHull();
			if (hull != null)
				OverlayUtil.renderPolygon(graphics, hull, color);

			if (!labelled)
			{
				Point text = object.getCanvasTextLocation(graphics, label, 0);
				if (text != null)
				{
					OverlayUtil.renderTextLocation(graphics, text, label, color);
					labelled = true;
				}
			}
		}
	}

	/** Highlights the seed table if it is loaded in the scene. */
	private void highlightSeedTable(Graphics2D graphics, Color color)
	{
		GameObject seedTable = plotTracker.getSeedTable();
		if (seedTable == null)
			return;

		Shape hull = seedTable.getConvexHull();
		if (hull != null)
			OverlayUtil.renderPolygon(graphics, hull, color);

		Point text = seedTable.getCanvasTextLocation(graphics, "Seeds", 0);
		if (text != null)
			OverlayUtil.renderTextLocation(graphics, text, "Seeds", color);
	}

	/** Boxes the first inventory slot whose item id the predicate accepts. */
	private void highlightInventory(Graphics2D graphics, IntPredicate wanted, Color color)
	{
		Widget inventory = client.getWidget(InterfaceID.Inventory.ITEMS);
		if (inventory == null)
			return;

		for (Widget item : inventory.getDynamicChildren())
		{
			if (item == null || item.isHidden() || !wanted.test(item.getItemId()))
				continue;

			Rectangle bounds = item.getBounds();
			if (bounds != null)
			{
				graphics.setColor(color);
				graphics.draw(bounds);
			}

			return;
		}
	}

	/** Whether an inventory item id has water to pour: a filled regular can or Gricoller's can. */
	private boolean inventoryHas(IntPredicate wanted)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
			return false;

		for (Item item : inventory.getItems())
		{
			if (item != null && wanted.test(item.getId()))
				return true;
		}

		return false;
	}

	/** Whether an item id is a watering can that can still pour — a filled regular can or Gricoller's can. */
	private static boolean isFilledCan(int itemId)
	{
		return TitheFarmIds.regularCanCharges(itemId) > 0 || itemId == TitheFarmIds.GRICOLLER_CAN;
	}
}
