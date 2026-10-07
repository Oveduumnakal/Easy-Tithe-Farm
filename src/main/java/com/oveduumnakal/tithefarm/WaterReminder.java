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

import net.runelite.client.Notifier;

/**
 * Fires a one-shot notification when the plants already in the ground need more water than is carried (see
 * {@link RunSnapshot#isLow}), and resets once the player refills above it, so a single low-water run raises at
 * most one alert rather than one per tick. Plots emptied at harvest and seeds still to plant never count.
 * Whether and how it shows is up to the "Notify when low" notification setting. The on-panel warning is drawn
 * separately by {@link TitheWaterOverlay}; this only handles the notification.
 */
@Singleton
class WaterReminder
{
	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;
	private final TitheRun run;
	private final Notifier notifier;

	private boolean notified;

	@Inject
	WaterReminder(TitheFarmConfig config, TithePlotTracker plotTracker, TitheRun run, Notifier notifier)
	{
		this.config = config;
		this.plotTracker = plotTracker;
		this.run = run;
		this.notifier = notifier;
	}

	/** Checks water against the planted plants' need once per tick, notifying on the first crossing below it. */
	void onTick()
	{
		if (!plotTracker.inTitheFarm())
		{
			notified = false;
			return;
		}

		if (!run.snapshot().isLow())
		{
			notified = false;
			return;
		}

		if (!notified)
		{
			notifier.notify(config.notifyWhenLow(),
				"Tithe Farm: your plants need more water than you carry. Refill at the barrel.");
			notified = true;
		}
	}

	/** Clears the notified flag, for plugin start or a scene reload. */
	void reset()
	{
		notified = false;
	}
}
