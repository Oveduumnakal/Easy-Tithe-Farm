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

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.client.Notifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifies the goal notification fires once per crossing and never on the first goal seen. */
public class GoalTrackerTest
{
	private TitheFarmConfig config;
	private GoalTracker tracker;

	@Before
	public void setUp()
	{
		config = TestRuns.defaultConfig();
		when(config.trackStrawhat()).thenReturn(true);
		tracker = new GoalTracker(mock(Client.class), config, mock(Notifier.class));
	}

	@Test
	public void firesOnceWhenPointsReachTheTotal()
	{
		assertFalse(tracker.shouldNotify(Goal.of(config, 70)));
		assertTrue(tracker.shouldNotify(Goal.of(config, 75)));
		assertFalse(tracker.shouldNotify(Goal.of(config, 78)));
	}

	@Test
	public void doesNotFireOnTheFirstGoalSeen()
	{
		assertFalse(tracker.shouldNotify(Goal.of(config, 500)));
		assertFalse(tracker.shouldNotify(Goal.of(config, 500)));
	}

	@Test
	public void spendingBelowTheTotalRearms()
	{
		tracker.shouldNotify(Goal.of(config, 70));
		assertTrue(tracker.shouldNotify(Goal.of(config, 80)));
		assertFalse(tracker.shouldNotify(Goal.of(config, 5)));
		assertTrue(tracker.shouldNotify(Goal.of(config, 75)));
	}

	@Test
	public void tickingAnotherRewardRearms()
	{
		tracker.shouldNotify(Goal.of(config, 70));
		assertTrue(tracker.shouldNotify(Goal.of(config, 100)));
		when(config.trackHerbSack()).thenReturn(true);
		assertFalse(tracker.shouldNotify(Goal.of(config, 100)));
		assertTrue(tracker.shouldNotify(Goal.of(config, 325)));
	}

	@Test
	public void emptyGoalAndResetClearTheBaseline()
	{
		tracker.shouldNotify(Goal.of(config, 70));
		when(config.trackStrawhat()).thenReturn(false);
		assertFalse(tracker.shouldNotify(Goal.of(config, 70)));
		when(config.trackStrawhat()).thenReturn(true);
		assertFalse(tracker.shouldNotify(Goal.of(config, 100)));
		tracker.reset();
		assertFalse(tracker.shouldNotify(Goal.of(config, 100)));
	}
}
