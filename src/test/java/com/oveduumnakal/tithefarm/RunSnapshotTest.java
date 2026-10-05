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
import java.util.List;

import org.junit.Test;

import net.runelite.api.GameObject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies when a run counts as between runs, the only time the water barrel is outlined. */
public class RunSnapshotTest
{
	private static final int EMPTY = 27383;
	private static final int DEAD = 27386;
	private static final int STAGE1_WET = 27385;
	private static final int GROWN = 27393;

	private static RunSnapshot snapshotOf(int... ids)
	{
		List<GameObject> route = new ArrayList<>();
		List<PlotInfo> plots = new ArrayList<>();
		for (int i = 0; i < ids.length; i++)
		{
			route.add(TestRuns.plot(ids[i], i));
			plots.add(PlotInfo.of(ids[i], 0));
		}

		return TestRuns.snapshot(route, plots, new ActionAdvisor.Advice(NextAction.WAIT, -1));
	}

	@Test
	public void emptyFarmIsBetweenRuns()
	{
		assertTrue(snapshotOf(EMPTY, EMPTY).isBetweenRuns());
		assertTrue(snapshotOf(EMPTY, DEAD).isBetweenRuns());
		assertTrue(TestRuns.snapshot(new ArrayList<>(), Arrays.asList(), null).isBetweenRuns());
	}

	@Test
	public void anyPlantInTheGroundMeansARunIsOn()
	{
		assertFalse(snapshotOf(EMPTY, STAGE1_WET).isBetweenRuns());
		assertFalse(snapshotOf(EMPTY, GROWN).isBetweenRuns());
	}
}
