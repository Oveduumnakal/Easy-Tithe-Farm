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
 * Tracks this session's Farming experience and time spent in the farm, and reads the spendable points, for the
 * run panel. The reward goal is tracked separately by {@link GoalTracker}.
 *
 * <p>The session starts at the first logged-in tick after the plugin starts, and restarts if the plugin is
 * restarted or {@link #reset()} is called, which the plugin also does when a different account logs in. Time
 * only accrues while the player is in the farm.
 * {@link #compactXp(int)} is static so it can be unit-tested.
 */
@Singleton
class SessionTracker
{
	private final Client client;
	private final TithePlotTracker plotTracker;

	private boolean started;
	private int startXp;
	private int ticksInFarm;

	@Inject
	SessionTracker(Client client, TithePlotTracker plotTracker)
	{
		this.client = client;
		this.plotTracker = plotTracker;
	}

	/** Records the experience baseline on its first tick, then accrues time in the farm each tick. */
	void onTick()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		if (!started)
		{
			started = true;
			startXp = client.getSkillExperience(Skill.FARMING);
		}

		if (plotTracker.inTitheFarm())
			ticksInFarm++;
	}

	/** Starts a fresh session on the next tick. */
	void reset()
	{
		started = false;
		ticksInFarm = 0;
	}

	/** The spendable points now. */
	int points()
	{
		return client.getVarbitValue(TitheFarmIds.POINTS_VARBIT);
	}

	/** Farming experience gained this session. */
	int xpTonight()
	{
		return started ? Math.max(0, client.getSkillExperience(Skill.FARMING) - startXp) : 0;
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
