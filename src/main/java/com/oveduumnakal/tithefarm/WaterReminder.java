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
 * Fires a one-shot notification when the player's water drops below what the run needs, and resets once they
 * refill above it, so a single low-water run raises at most one alert rather than one per tick. The on-panel
 * warning is drawn separately by {@link TitheWaterOverlay}; this only handles the notification.
 */
@Singleton
class WaterReminder
{
	private final TitheFarmConfig config;
	private final WaterTracker waterTracker;
	private final TithePlotTracker plotTracker;
	private final Notifier notifier;

	private boolean notified;

	@Inject
	WaterReminder(TitheFarmConfig config, WaterTracker waterTracker, TithePlotTracker plotTracker,
		Notifier notifier)
	{
		this.config = config;
		this.waterTracker = waterTracker;
		this.plotTracker = plotTracker;
		this.notifier = notifier;
	}

	/** Checks water against the run's need once per tick, notifying on the first crossing below it. */
	void onTick()
	{
		if (!plotTracker.inTitheFarm())
		{
			notified = false;
			return;
		}

		int needed = waterTracker.chargesNeeded(config.cropCount());
		boolean short0 = waterTracker.availableCharges() < needed;
		if (!short0)
		{
			notified = false;
			return;
		}

		if (config.notifyOnRefill() && !notified)
		{
			notifier.notify("Tithe Farm: low on water — refill at the barrel before planting more.");
			notified = true;
		}
	}

	/** Clears the notified flag, for plugin start or a scene reload. */
	void reset()
	{
		notified = false;
	}
}
