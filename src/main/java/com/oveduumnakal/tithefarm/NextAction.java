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

/** The single next thing the player should do in the run, chosen by {@link ActionAdvisor}. */
enum NextAction
{
	/** Plant a seed in the next empty plot. */
	PLANT_SEED("Plant"),

	/** Water a plant that needs it this stage. */
	WATER_PLANT("Water"),

	/** Harvest a fully grown plant. */
	HARVEST("Harvest"),

	/** Clear a dead plant so the plot can be replanted. */
	CLEAR_DEAD("Clear"),

	/** Refill watering cans at the barrel — not enough water for what comes next. */
	REFILL_WATER("Refill"),

	/** Put the carried fruit in the sack — between runs, or mid-run only when a harvest would not fit. */
	DEPOSIT_FRUIT("Deposit"),

	/** Collect seeds — an empty plot is waiting but the backpack has no seeds. */
	GET_SEEDS("Seeds"),

	/** The last run is done — everything harvested and deposited — so leave through the farm door. */
	LEAVE("Leave"),

	/** Nothing to do this moment; plants are watered and still growing. */
	WAIT("Wait");

	private final String label;

	NextAction(String label)
	{
		this.label = label;
	}

	/** The short word drawn on the highlighted target and shown in the panel. */
	String getLabel()
	{
		return label;
	}
}
