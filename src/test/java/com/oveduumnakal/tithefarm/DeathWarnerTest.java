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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.GameObject;
import net.runelite.client.Notifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the about-to-die notification fires once each time a plant enters the window and respects its setting. */
public class DeathWarnerTest
{
	private static final int STAGE2_DRY = 27387;

	private TitheFarmConfig config;
	private TithePlotTracker tracker;
	private TitheRun run;
	private Notifier notifier;
	private DeathWarner warner;

	@Before
	public void setUp()
	{
		config = TestRuns.defaultConfig();
		tracker = mock(TithePlotTracker.class);
		run = mock(TitheRun.class);
		notifier = mock(Notifier.class);
		when(tracker.inTitheFarm()).thenReturn(true);
		warner = new DeathWarner(config, tracker, run, notifier);
	}

	private void plantAged(int ageTicks, long hash)
	{
		GameObject plot = TestRuns.plot(STAGE2_DRY, hash);
		when(run.snapshot()).thenReturn(TestRuns.snapshot(TestRuns.listOf(plot),
			TestRuns.listOf(PlotInfo.of(STAGE2_DRY, ageTicks)), new ActionAdvisor.Advice(NextAction.WAIT, -1)));
	}

	@Test
	public void quietWhileThePlantHasTime()
	{
		plantAged(10, 1L);
		warner.onTick();
		verify(notifier, never()).notify(anyString());
	}

	@Test
	public void notifiesOnceInsideTheWindow()
	{
		plantAged(90, 1L);
		warner.onTick();
		warner.onTick();
		verify(notifier, times(1)).notify(contains("dies in 6s"));
	}

	@Test
	public void notifiesAgainForTheNextStage()
	{
		plantAged(90, 1L);
		warner.onTick();
		plantAged(90, 2L);
		warner.onTick();
		verify(notifier, times(2)).notify(anyString());
	}

	@Test
	public void notifiesAgainWhenTheSamePlantReentersTheWindow()
	{
		plantAged(90, 1L);
		warner.onTick();
		plantAged(10, 1L);
		warner.onTick();
		plantAged(90, 1L);
		warner.onTick();
		verify(notifier, times(2)).notify(contains("dies in 6s"));
	}

	@Test
	public void countsEveryPlantEnteringTheWindowTogether()
	{
		GameObject a = TestRuns.plot(STAGE2_DRY, 1L);
		GameObject b = TestRuns.plot(STAGE2_DRY, 2L);
		when(run.snapshot()).thenReturn(TestRuns.snapshot(Arrays.asList(a, b),
			Arrays.asList(PlotInfo.of(STAGE2_DRY, 95), PlotInfo.of(STAGE2_DRY, 80)),
			new ActionAdvisor.Advice(NextAction.WAIT, -1)));
		warner.onTick();
		verify(notifier).notify(contains("2 plants die in 3s"));
	}

	@Test
	public void silentWhenTurnedOff()
	{
		when(config.notifyBeforeDeath()).thenReturn(false);
		plantAged(95, 1L);
		warner.onTick();
		verify(notifier, never()).notify(anyString());
	}
}
