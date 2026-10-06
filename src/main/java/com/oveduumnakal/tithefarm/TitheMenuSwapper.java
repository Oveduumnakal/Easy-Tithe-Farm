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

import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;

/**
 * Guards against clicks that would break the run, by moving {@code "Cancel"} to the top of the menu so a stray
 * left-click cancels instead. Three guards, each with its own setting:
 * <ul>
 * <li>Low water: a plant action on an empty plot while the water carried would not cover what the crops already
 * in the ground still need plus three for every seed the run still has room for. The need is read from the
 * plots, so watering mid-run never trips it falsely.</li>
 * <li>Wrong plot: a plant action on any plot but the route's next one, so seeds go in route order.</li>
 * <li>Out-of-order water: while the next action is watering, a click on any other unwatered plant.</li>
 * </ul>
 * It only ever reorders — no entry is removed — mirroring the Cancel-to-top guard in the Goat Pit Indicators
 * plugin. Runs every frame on {@code PostMenuSort} so it fixes both the left-click default and the right-click
 * ordering.
 */
@Singleton
class TitheMenuSwapper
{
	private final Client client;
	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;
	private final TitheRun run;

	@Inject
	TitheMenuSwapper(Client client, TitheFarmConfig config, TithePlotTracker plotTracker, TitheRun run)
	{
		this.client = client;
		this.config = config;
		this.plotTracker = plotTracker;
		this.run = run;
	}

	/** Applies the Cancel-to-top guard when the menu holds a plant or water click a guard forbids. */
	void onPostMenuSort()
	{
		boolean anyGuard = config.blockPlantWhenShort() || config.blockWrongPlant() || config.blockOutOfOrderWater();
		if (!anyGuard || !plotTracker.inTitheFarm())
			return;

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		if (entries.length < 2 || !hasBlockedEntry(entries))
			return;

		MenuEntry cancel = firstOfType(entries, MenuAction.CANCEL);
		if (cancel != null)
			menu.setMenuEntries(promoteToTop(entries, cancel));
	}

	/** Whether any entry is a plant or water click that an enabled guard forbids. */
	private boolean hasBlockedEntry(MenuEntry[] entries)
	{
		for (MenuEntry entry : entries)
		{
			if (isPlantEntry(entry.getType(), entry.getIdentifier()))
			{
				if (blocksPlant(entry, run.snapshot()))
					return true;
			}
			else if (isWaterEntry(entry.getType(), entry.getIdentifier()))
			{
				if (blocksWater(entry, run.snapshot()))
					return true;
			}
		}

		return false;
	}

	/** Whether a plant click is forbidden: water is short, or the plot is not the route's next one. */
	private boolean blocksPlant(MenuEntry entry, RunSnapshot snapshot)
	{
		if (config.blockPlantWhenShort() && !snapshot.canAffordPlant())
			return true;

		return config.blockWrongPlant() && !snapshot.isRecording() && !isOn(entry, nextPlantPlot(snapshot));
	}

	/** Whether a water click is forbidden: the next action waters a different plant. */
	private boolean blocksWater(MenuEntry entry, RunSnapshot snapshot)
	{
		if (!config.blockOutOfOrderWater() || snapshot.getAdvice().getAction() != NextAction.WATER_PLANT)
			return false;

		return !isOn(entry, snapshot.getTargetPlot());
	}

	/**
	 * The route plot the next seed belongs in: the earliest route plot still needing one.
	 *
	 * @param snapshot the current run
	 * @return that plot, or {@code null} when every route plot is planted
	 */
	static GameObject nextPlantPlot(RunSnapshot snapshot)
	{
		for (int i = 0; i < snapshot.getRouteLength(); i++)
		{
			if (snapshot.getPlots().get(i).needsSeed())
				return snapshot.plotAt(i);
		}

		return null;
	}

	/**
	 * Whether a menu entry targets the given plot. Object entries carry the scene tile clicked in their params,
	 * which lies within the plot's footprint.
	 *
	 * @param entry the menu entry
	 * @param plot  the plot, or {@code null}
	 * @return true when the entry's tile is on the plot
	 */
	static boolean isOn(MenuEntry entry, GameObject plot)
	{
		if (plot == null)
			return false;

		Point min = plot.getSceneMinLocation();
		Point max = plot.getSceneMaxLocation();
		int x = entry.getParam0();
		int y = entry.getParam1();
		return x >= min.getX() && x <= max.getX() && y >= min.getY() && y <= max.getY();
	}

	/**
	 * Whether a menu entry's type and identifier mark a plant action on an empty plot: either a game-object
	 * option or a "use item on object" aimed at the empty-plot object id.
	 *
	 * @param type       the entry's menu action
	 * @param identifier the entry's identifier, which for object entries is the object id
	 * @return true when the entry would plant a seed
	 */
	static boolean isPlantEntry(MenuAction type, int identifier)
	{
		return identifier == TitheFarmIds.PLOT_EMPTY && isObjectClick(type);
	}

	/**
	 * Whether a menu entry's type and identifier mark a click on an unwatered plant: its own option or a "use
	 * item on object" aimed at it, which is how a watering can is used.
	 *
	 * @param type       the entry's menu action
	 * @param identifier the entry's identifier, which for object entries is the object id
	 * @return true when the entry would water a plant
	 */
	static boolean isWaterEntry(MenuAction type, int identifier)
	{
		return TithePlotState.fromObjectId(identifier) == TithePlotState.UNWATERED && isObjectClick(type);
	}

	/** Whether a menu action is a game-object option or an item used on a game object. */
	private static boolean isObjectClick(MenuAction type)
	{
		switch (type)
		{
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			case WIDGET_TARGET_ON_GAME_OBJECT:
				return true;
			default:
				return false;
		}
	}

	/** The first entry of the given action in menu order, or {@code null} when the menu has none. */
	private static MenuEntry firstOfType(MenuEntry[] entries, MenuAction type)
	{
		for (MenuEntry entry : entries)
		{
			if (entry.getType() == type)
				return entry;
		}

		return null;
	}

	/**
	 * Returns a copy of the menu with {@code promote} moved to the last slot — the top of the visible menu and
	 * the left-click default — leaving every other entry in its existing order.
	 *
	 * @param entries the current menu entries
	 * @param promote the entry to move to the top
	 * @return a reordered copy
	 */
	static MenuEntry[] promoteToTop(MenuEntry[] entries, MenuEntry promote)
	{
		MenuEntry[] reordered = new MenuEntry[entries.length];
		int index = 0;
		for (MenuEntry entry : entries)
		{
			if (entry != promote)
				reordered[index++] = entry;
		}

		reordered[index] = promote;
		return reordered;
	}
}
