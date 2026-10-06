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

/** Verifies the low-water notification fires once per shortfall, through the configured notification. */
public class WaterReminderTest
{
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

	private void water(int carried, int needed)
	{
		RunSnapshot snapshot = new RunSnapshot(TestRuns.listOf(TestRuns.plot(27383, 1L)), 1,
			TestRuns.listOf(PlotInfo.of(27383, 0)), carried, needed, true,
			new ActionAdvisor.Advice(NextAction.WAIT, -1), false, 0, RunStatus.NEUTRAL,
			Collections.emptyList());
		when(run.snapshot()).thenReturn(snapshot);
	}

	@Test
	public void notifiesOncePerShortfall()
	{
		water(10, 60);
		reminder.onTick();
		reminder.onTick();
		verify(notifier, times(1)).notify(any(Notification.class), anyString());
		water(64, 60);
		reminder.onTick();
		water(10, 60);
		reminder.onTick();
		verify(notifier, times(2)).notify(any(Notification.class), anyString());
	}

	@Test
	public void quietWhenThereIsEnough()
	{
		water(64, 60);
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
		water(10, 60);
		reminder.onTick();
		verify(notifier).notify(same(Notification.OFF), anyString());
	}
}
