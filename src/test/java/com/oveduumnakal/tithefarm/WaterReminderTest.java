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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.GameObject;
import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the low-water notification fires once per shortfall of the plants in the ground, through the configured
 * notification, and stays quiet at harvest and between runs.
 */
public class WaterReminderTest
{
	/** A seed planted moments ago: unwatered at stage 1, owed three waters. */
	private static final int FRESH_SEED = TitheFarmIds.PLOT_GROWTH_FIRST;

	private TitheFarmConfig config;
	private TitheRun run;
	private Notifier notifier;
	private WaterReminder reminder;

	@Before
	public void setUp()
	{
		config = TestRuns.defaultConfig();
		TithePlotTracker tracker = mock(TithePlotTracker.class);
		run = mock(TitheRun.class);
		notifier = mock(Notifier.class);
		when(tracker.inTitheFarm()).thenReturn(true);
		reminder = new WaterReminder(config, tracker, run, notifier);
	}

	/** Sets the run to the given water carried over plots of the given ids, the whole run needing 60. */
	private void water(int carried, int... plotIds)
	{
		List<GameObject> route = new ArrayList<>();
		List<PlotInfo> plots = new ArrayList<>();
		for (int i = 0; i < plotIds.length; i++)
		{
			route.add(TestRuns.plot(plotIds[i], i + 1L));
			plots.add(PlotInfo.of(plotIds[i], 0));
		}

		RunSnapshot snapshot = new RunSnapshot(route, route.size(), plots, carried, 60, true,
			new ActionAdvisor.Advice(NextAction.WAIT, -1), false, 0, RunStatus.NEUTRAL, Collections.emptyList());
		when(run.snapshot()).thenReturn(snapshot);
	}

	/** Sets the run to the given water carried over a number of fresh seeds, each owed three waters. */
	private void seeds(int carried, int count)
	{
		int[] ids = new int[count];
		Arrays.fill(ids, FRESH_SEED);
		water(carried, ids);
	}

	@Test
	public void notifiesOncePerShortfall()
	{
		seeds(10, 4);
		reminder.onTick();
		reminder.onTick();
		verify(notifier, times(1)).notify(any(Notification.class), anyString());
		seeds(12, 4);
		reminder.onTick();
		seeds(10, 4);
		reminder.onTick();
		verify(notifier, times(2)).notify(any(Notification.class), anyString());
	}

	@Test
	public void quietWhenThereIsEnough()
	{
		seeds(64, 20);
		reminder.onTick();
		verify(notifier, never()).notify(any(Notification.class), anyString());
	}

	@Test
	public void quietWhenHarvestEmptiesPlots()
	{
		water(4, TitheFarmIds.PLOT_EMPTY, TitheFarmIds.PLOT_EMPTY, TitheFarmIds.PLOT_A_GROWN,
			TitheFarmIds.PLOT_A_GROWN);
		reminder.onTick();
		verify(notifier, never()).notify(any(Notification.class), anyString());
	}

	@Test
	public void quietBetweenRuns()
	{
		water(0, TitheFarmIds.PLOT_EMPTY, TitheFarmIds.PLOT_EMPTY);
		reminder.onTick();
		verify(notifier, never()).notify(any(Notification.class), anyString());
	}

	@Test
	public void onByDefault()
	{
		assertEquals(Notification.ON, config.notifyWhenLow());
	}

	@Test
	public void usesTheConfiguredNotification()
	{
		when(config.notifyWhenLow()).thenReturn(Notification.OFF);
		seeds(2, 1);
		reminder.onTick();
		verify(notifier).notify(same(Notification.OFF), anyString());
	}
}
