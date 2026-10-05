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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.GameObject;
import net.runelite.client.Notifier;

/**
 * Notifies once when a plant enters the warning window before it dies for lack of water — for the moment a
 * child wakes up or the phone rings mid-run. Each plant warns at most once per growth stage: a plot's object
 * hash changes with every stage, so the hash alone is the de-duplication key. RuneLite's own notification
 * settings decide whether it also fires while the client is focused.
 */
@Singleton
class DeathWarner
{
	/** Warned hashes kept before the set is pruned; far more than one farm's worth of plant stages. */
	private static final int MAX_REMEMBERED = 512;

	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;
	private final TitheRun run;
	private final Notifier notifier;
	private final Set<Long> warned = new HashSet<>();

	@Inject
	DeathWarner(TitheFarmConfig config, TithePlotTracker plotTracker, TitheRun run, Notifier notifier)
	{
		this.config = config;
		this.plotTracker = plotTracker;
		this.run = run;
		this.notifier = notifier;
	}

	/** Checks every plant once per tick and notifies for any newly inside the warning window. */
	void onTick()
	{
		if (!config.notifyBeforeDeath() || !plotTracker.inTitheFarm())
			return;

		RunSnapshot snapshot = run.snapshot();
		List<GameObject> route = snapshot.getRoute();
		List<PlotInfo> plots = snapshot.getPlots();
		int window = TitheTime.ticks(config.deathWarnSeconds());
		int dying = 0;
		int soonest = Integer.MAX_VALUE;
		for (int i = 0; i < route.size(); i++)
		{
			int ticks = plots.get(i).ticksUntilDeath();
			if (ticks < 0 || ticks > window || !warned.add(route.get(i).getHash()))
				continue;

			dying++;
			soonest = Math.min(soonest, ticks);
		}

		if (dying > 0)
		{
			String plants = dying == 1 ? "A plant dies" : dying + " plants die";
			notifier.notify("Tithe Farm: " + plants + " in " + TitheTime.seconds(soonest) + "s. Water now!");
		}

		if (warned.size() > MAX_REMEMBERED)
			warned.clear();
	}

	/** Forgets which plants were warned about, for a scene reload. */
	void reset()
	{
		warned.clear();
	}
}
