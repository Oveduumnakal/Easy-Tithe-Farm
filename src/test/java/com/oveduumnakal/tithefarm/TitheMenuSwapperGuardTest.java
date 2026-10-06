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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Drives the plant guard through a mocked client menu. */
public class TitheMenuSwapperGuardTest
{
	private TitheFarmConfig config;
	private TitheRun run;
	private Menu menu;
	private MenuEntry cancel;
	private MenuEntry plant;
	private TitheMenuSwapper swapper;

	@Before
	public void setUp()
	{
		Client client = mock(Client.class);
		config = TestRuns.defaultConfig();
		TithePlotTracker tracker = mock(TithePlotTracker.class);
		run = mock(TitheRun.class);
		menu = mock(Menu.class);
		cancel = entry(MenuAction.CANCEL, 0);
		plant = entry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, TitheFarmIds.PLOT_EMPTY);
		when(client.getMenu()).thenReturn(menu);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{cancel, plant});
		when(tracker.inTitheFarm()).thenReturn(true);
		swapper = new TitheMenuSwapper(client, config, tracker, run);
	}

	private static MenuEntry entry(MenuAction type, int identifier)
	{
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getType()).thenReturn(type);
		when(entry.getIdentifier()).thenReturn(identifier);
		return entry;
	}

	private void affordable(boolean canAfford)
	{
		RunSnapshot snapshot = new RunSnapshot(TestRuns.listOf(TestRuns.plot(27383, 1L)), 1,
			TestRuns.listOf(PlotInfo.of(27383, 0)), 2, 60, canAfford,
			new ActionAdvisor.Advice(NextAction.REFILL_WATER, -1), false, 0, RunStatus.NEUTRAL,
			Collections.emptyList());
		when(run.snapshot()).thenReturn(snapshot);
	}

	@Test
	public void promotesCancelWhenTheSeedCannotBeWatered()
	{
		affordable(false);
		swapper.onPostMenuSort();
		verify(menu).setMenuEntries(new MenuEntry[]{plant, cancel});
	}

	@Test
	public void leavesTheMenuAloneWhenWaterCovers()
	{
		affordable(true);
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void leavesTheMenuAloneWhenTheGuardIsOff()
	{
		when(config.blockPlantWhenShort()).thenReturn(false);
		affordable(false);
		swapper.onPostMenuSort();
		verify(menu, never()).setMenuEntries(any());
	}
}
