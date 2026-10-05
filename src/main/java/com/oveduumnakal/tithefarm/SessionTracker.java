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
import net.runelite.api.GameState;
import net.runelite.api.Skill;

/**
 * fruit deposited, and time spent in the farm. The reward goal is tracked separately by {@link GoalTracker}.
 * fruit deposited, and time spent in the farm, plus how many runs remain to the chosen reward.
 *
 * <p>The session starts at the first logged-in tick after the plugin starts, and restarts if the plugin is
 * restarted. Fruit deposited is summed from rises in the game's score, so the reset when leaving the farm does
 * not lose tonight's count. Time only accrues while the player is in the farm. The arithmetic is static so it can
 * be unit-tested.
 */
@Singleton
class SessionTracker
{
	private final Client client;
	private final TithePlotTracker plotTracker;

	private boolean started;
	private int startPoints;
	private int startXp;
	private int lastScore;
	private int fruitTonight;
	private int ticksInFarm;

	@Inject
	SessionTracker(Client client, TithePlotTracker plotTracker)
	{
		this.client = client;
		this.plotTracker = plotTracker;
	}

	/** Records the session baseline on its first tick, then accrues fruit and time each tick. */
	void onTick()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		int score = client.getVarbitValue(TitheFarmIds.SCORE_VARBIT);
		if (!started)
		{
			started = true;
			startPoints = points();
			startXp = client.getSkillExperience(Skill.FARMING);
			lastScore = score;
		}

		if (score > lastScore)
			fruitTonight += score - lastScore;

		lastScore = score;
		if (plotTracker.inTitheFarm())
			ticksInFarm++;
	}

	/** Starts a fresh session on the next tick. */
	void reset()
	{
		started = false;
		fruitTonight = 0;
		ticksInFarm = 0;
	}

	/** The spendable points now. */
	int points()
	{
		return client.getVarbitValue(TitheFarmIds.POINTS_VARBIT);
	}

	/** Points earned this session, never negative (spending points does not count against it). */
	int pointsTonight()
	{
		return started ? Math.max(0, points() - startPoints) : 0;
	}

	/** Farming experience gained this session. */
	int xpTonight()
	{
		return started ? Math.max(0, client.getSkillExperience(Skill.FARMING) - startXp) : 0;
	}

	/** Fruit deposited this session, across games. */
	int fruitTonight()
	{
		return fruitTonight;
	}

	/** Ticks spent in the farm this session. */
	int ticksInFarm()
	{
		return ticksInFarm;
	}

	/**
	 * Experience as a compact figure, e.g. {@code 950}, {@code 12.3k}, {@code 1.2m}.
	 *
	 * @param xp the experience
	 * @return the compact text
	 */
	static String compactXp(int xp)
	{
		if (xp >= 1_000_000)
			return String.format("%.1fm", xp / 1_000_000.0);

		if (xp >= 1_000)
			return String.format("%.1fk", xp / 1_000.0);

		return String.valueOf(xp);
	}
}
