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
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

/**
 * Guards against starting or growing a run the player cannot finish watering. When enabled, the menu contains a
 * plant action on an empty plot, and the water carried would not cover what the crops already in the ground
 * still need plus three for every seed the run still has room for, {@code "Cancel"} is moved to the top so a
 * stray left-click cancels rather than sinks a seed. The need is read from the plots, so watering mid-run never
 * trips it falsely.
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

	/** Applies the Cancel-to-top guard when water is short and a plant action is present. */
	void onPostMenuSort()
	{
		if (!config.blockPlantWhenShort() || !plotTracker.inTitheFarm())
			return;

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		if (entries.length < 2 || !hasPlantOnEmptyPlot(entries) || run.snapshot().canAffordPlant())
			return;

		MenuEntry cancel = firstOfType(entries, MenuAction.CANCEL);
		if (cancel != null)
			menu.setMenuEntries(promoteToTop(entries, cancel));
	}

	/** Whether the menu contains an interaction with an empty plot — the click that would plant a seed. */
	private static boolean hasPlantOnEmptyPlot(MenuEntry[] entries)
	{
		for (MenuEntry entry : entries)
		{
			if (isPlantEntry(entry.getType(), entry.getIdentifier()))
				return true;
		}

		return false;
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
		if (identifier != TitheFarmIds.PLOT_EMPTY)
			return false;

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
