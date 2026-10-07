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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The wrong-plot plant guard and the out-of-order water guard. */
public class TitheMenuSwapperOrderTest
{
	private static final int EMPTY = 27383;
	private static final int FRESH_C = 27406;
	private static final int STAGE1_WET_C = 27407;

	private TitheFarmConfig config;
	private TitheRun run;
	private Menu menu;
	private MenuEntry cancel;
	private TitheMenuSwapper swapper;

	@Before
	public void setUp()
	{
		Client client = mock(Client.class);
		config = TestRuns.defaultConfig();
		TithePlotTracker tracker = mock(TithePlotTracker.class);
		run = mock(TitheRun.class);
		menu = mock(Menu.class);
		cancel = entry(MenuAction.CANCEL, 0, 0);
		when(client.getMenu()).thenReturn(menu);
		when(tracker.inTitheFarm()).thenReturn(true);
		swapper = new TitheMenuSwapper(client, config, tracker, run);
	}

	/** A menu entry on the plot whose footprint starts at scene x {@code plotX}. */
	private static MenuEntry entry(MenuAction type, int identifier, int plotX)
	{
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getType()).thenReturn(type);
		when(entry.getIdentifier()).thenReturn(identifier);
		when(entry.getParam0()).thenReturn(plotX + 1);
		when(entry.getParam1()).thenReturn(11);
		return entry;
	}

	/** Route plots side by side, three tiles wide, with the given object ids. */
	private void route(ActionAdvisor.Advice advice, int... ids)
	{
		List<GameObject> plots = new ArrayList<>();
		List<PlotInfo> infos = new ArrayList<>();
		for (int i = 0; i < ids.length; i++)
		{
			GameObject plot = TestRuns.plot(ids[i], i + 1);
			when(plot.getSceneMinLocation()).thenReturn(new Point(i * 3, 10));
			when(plot.getSceneMaxLocation()).thenReturn(new Point(i * 3 + 2, 12));
			plots.add(plot);
			infos.add(PlotInfo.of(ids[i], 0));
		}

		when(run.snapshot()).thenReturn(TestRuns.snapshot(plots, infos, advice));
	}

	private void menuWith(MenuEntry entry)
	{
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{cancel, entry});
	}

	@Test
	public void plantingTheWrongPlotIsAllowedByDefault()
	{
		route(new ActionAdvisor.Advice(NextAction.PLANT_SEED, 1), STAGE1_WET_C, EMPTY, EMPTY);
		menuWith(entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, EMPTY, 6));
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void plantingTheWrongPlotPromotesCancelWhenEnabled()
	{
		when(config.blockWrongPlant()).thenReturn(true);
		route(new ActionAdvisor.Advice(NextAction.PLANT_SEED, 1), STAGE1_WET_C, EMPTY, EMPTY);
		MenuEntry plant = entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, EMPTY, 6);
		menuWith(plant);
		swapper.onPostMenuSort();
		verify(menu).setMenuEntries(new MenuEntry[]{plant, cancel});
	}

	@Test
	public void plantingTheNextRoutePlotIsAllowedWhenEnabled()
	{
		when(config.blockWrongPlant()).thenReturn(true);
		route(new ActionAdvisor.Advice(NextAction.WATER_PLANT, 0), FRESH_C, EMPTY, EMPTY);
		menuWith(entry(MenuAction.GAME_OBJECT_FIRST_OPTION, EMPTY, 3));
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void wateringAnotherPlantPromotesCancelByDefault()
	{
		route(new ActionAdvisor.Advice(NextAction.WATER_PLANT, 0), FRESH_C, FRESH_C);
		MenuEntry water = entry(MenuAction.GAME_OBJECT_FIRST_OPTION, FRESH_C, 3);
		menuWith(water);
		swapper.onPostMenuSort();
		verify(menu).setMenuEntries(new MenuEntry[]{water, cancel});
	}

	@Test
	public void wateringTheTargetPlantIsAllowed()
	{
		route(new ActionAdvisor.Advice(NextAction.WATER_PLANT, 0), FRESH_C, FRESH_C);
		menuWith(entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, FRESH_C, 0));
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void wateringTheTargetIsAllowedWithAnotherPlantUnderTheCursor()
	{
		route(new ActionAdvisor.Advice(NextAction.WATER_PLANT, 0), FRESH_C, FRESH_C);
		MenuEntry other = entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, FRESH_C, 3);
		MenuEntry target = entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, FRESH_C, 0);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{cancel, other, target});
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void wateringAnotherPlantOnTopPromotesCancelWithTheTargetBelow()
	{
		route(new ActionAdvisor.Advice(NextAction.WATER_PLANT, 0), FRESH_C, FRESH_C);
		MenuEntry target = entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, FRESH_C, 0);
		MenuEntry other = entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, FRESH_C, 3);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{cancel, target, other});
		swapper.onPostMenuSort();
		verify(menu).setMenuEntries(new MenuEntry[]{target, other, cancel});
	}

	@Test
	public void plantingTheNextPlotIsAllowedWithAnotherEmptyPlotUnderTheCursor()
	{
		when(config.blockWrongPlant()).thenReturn(true);
		route(new ActionAdvisor.Advice(NextAction.PLANT_SEED, 1), STAGE1_WET_C, EMPTY, EMPTY);
		MenuEntry other = entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, EMPTY, 6);
		MenuEntry next = entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, EMPTY, 3);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{cancel, other, next});
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void wateringIsNotGuardedWhenTheNextActionIsNotWatering()
	{
		route(new ActionAdvisor.Advice(NextAction.PLANT_SEED, 2), FRESH_C, FRESH_C, EMPTY);
		menuWith(entry(MenuAction.GAME_OBJECT_FIRST_OPTION, FRESH_C, 3));
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void wateringAnotherPlantIsAllowedWhenTheGuardIsOff()
	{
		when(config.blockOutOfOrderWater()).thenReturn(false);
		route(new ActionAdvisor.Advice(NextAction.WATER_PLANT, 0), FRESH_C, FRESH_C);
		menuWith(entry(MenuAction.GAME_OBJECT_FIRST_OPTION, FRESH_C, 3));
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}
}
