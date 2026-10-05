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
import net.runelite.api.GameState;
import net.runelite.api.Skill;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifies tonight's progress tracking against a mocked client. */
public class SessionTrackerTest
{
	private Client client;
	private TithePlotTracker plots;
	private SessionTracker session;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		plots = mock(TithePlotTracker.class);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(plots.inTitheFarm()).thenReturn(true);
		state(100, 0, 50_000);
		session = new SessionTracker(client, plots);
	}

	private void state(int points, int score, int xp)
	{
		when(client.getVarbitValue(TitheFarmIds.POINTS_VARBIT)).thenReturn(points);
		when(client.getVarbitValue(TitheFarmIds.SCORE_VARBIT)).thenReturn(score);
		when(client.getSkillExperience(Skill.FARMING)).thenReturn(xp);
	}

	@Test
	public void countsPointsExperienceAndTimeFromTheFirstTick()
	{
		session.onTick();
		state(118, 54, 62_300);
		session.onTick();
		assertEquals(18, session.pointsTonight());
		assertEquals(12_300, session.xpTonight());
		assertEquals(54, session.fruitTonight());
		assertEquals(2, session.ticksInFarm());
	}

	@Test
	public void keepsTonightsFruitWhenTheGameScoreResets()
	{
		session.onTick();
		state(118, 60, 50_000);
		session.onTick();
		state(118, 0, 50_000);
		session.onTick();
		state(125, 20, 50_000);
		session.onTick();
		assertEquals(80, session.fruitTonight());
	}

	@Test
	public void spendingPointsDoesNotGoNegative()
	{
		session.onTick();
		state(25, 0, 50_000);
		assertEquals(0, session.pointsTonight());
	}

	@Test
	public void ignoresTicksWhileLoggedOutAndOutsideTheFarm()
	{
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		session.onTick();
		assertEquals(0, session.ticksInFarm());
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(plots.inTitheFarm()).thenReturn(false);
		session.onTick();
		assertEquals(0, session.ticksInFarm());
	}

	@Test
	public void formatsExperienceCompactly()
	{
		assertEquals("950", SessionTracker.compactXp(950));
		assertEquals("12.3k", SessionTracker.compactXp(12_300));
		assertEquals("1.2m", SessionTracker.compactXp(1_234_567));
	}
}
