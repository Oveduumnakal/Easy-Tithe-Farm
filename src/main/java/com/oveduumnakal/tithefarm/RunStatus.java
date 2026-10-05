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
import java.util.List;

/**
 * The run's state beyond its plots, carried on a {@link RunSnapshot}: whether this is the last run and how many
 * seeds it still allows, the fruit deposited this game, missing tools, run energy, the fruit carried and the
 * experience it would earn once deposited, and whether every watering can is full.
 */
final class RunStatus
{
	/** A neutral status: not wrapping up, nothing missing, full energy, no fruit, full cans. */
	static final RunStatus NEUTRAL = new RunStatus(false, Integer.MAX_VALUE, 0, Collections.emptyList(), false, 100,
		0, 0, true);

	private final boolean wrapUp;
	private final int plantLimit;
	private final int deposited;
	private final List<String> missingTools;
	private final boolean fertiliser;
	private final int energyPercent;
	private final int carried;
	private final int pendingXp;
	private final boolean cansFull;

	/**
	 * Creates a status.
	 *
	 * @param wrapUp        whether this is the last run
	 * @param plantLimit    how many more seeds the run allows now
	 * @param deposited     the fruit deposited this game
	 * @param missingTools  the names of required tools not in the backpack
	 * @param fertiliser    whether Gricoller's fertiliser is in the backpack
	 * @param energyPercent the run energy, 0 to 100
	 * @param carried       the fruit in the backpack
	 * @param pendingXp     the experience depositing the carried fruit would earn
	 * @param cansFull      whether every watering can carried is full
	 */
	RunStatus(boolean wrapUp, int plantLimit, int deposited, List<String> missingTools, boolean fertiliser,
		int energyPercent, int carried, int pendingXp, boolean cansFull)
	{
		this.wrapUp = wrapUp;
		this.plantLimit = plantLimit;
		this.deposited = deposited;
		this.missingTools = Collections.unmodifiableList(missingTools);
		this.fertiliser = fertiliser;
		this.energyPercent = energyPercent;
		this.carried = carried;
		this.pendingXp = pendingXp;
		this.cansFull = cansFull;
	}

	/** Whether this is the last run. */
	boolean isWrapUp()
	{
		return wrapUp;
	}

	/** How many more seeds the run allows now. */
	int getPlantLimit()
	{
		return plantLimit;
	}

	/** The fruit deposited this game. */
	int getDeposited()
	{
		return deposited;
	}

	/** The names of required tools not in the backpack. */
	List<String> getMissingTools()
	{
		return missingTools;
	}

	/** Whether Gricoller's fertiliser is in the backpack. */
	boolean hasFertiliser()
	{
		return fertiliser;
	}

	/** The run energy, 0 to 100. */
	int getEnergyPercent()
	{
		return energyPercent;
	}

	/** The fruit in the backpack. */
	int getCarried()
	{
		return carried;
	}

	/** The experience depositing the carried fruit would earn. */
	int getPendingXp()
	{
		return pendingXp;
	}

	/** Whether every watering can carried is full. */
	boolean isCansFull()
	{
		return cansFull;
	}

	/**
	 * Whether run energy is below a warning threshold.
	 *
	 * @param thresholdPercent the threshold, 0 meaning the warning is off
	 * @return true when the warning is on and energy is under it
	 */
	boolean isEnergyLow(int thresholdPercent)
	{
		return thresholdPercent > 0 && energyPercent < thresholdPercent;
	}
}
