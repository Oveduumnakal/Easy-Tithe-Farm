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

/**
 * A client-free snapshot of one route plot: its decoded state, seed tier, growth stage, how many waters it still needs,
 * how long it has sat in its current object id, how long it has been in its current growth stage, and how long ago
 * its plant went in. The first two ages differ only for a watered plant: watering changes the object id but not
 * the stage clock, which runs from the moment the plant entered the stage. {@link ActionAdvisor} and the water
 * math work on lists of these so they can be unit-tested without scene objects.
 */
final class PlotInfo
{
	/** Age reported when the plot's current id, or its plant, appeared before tracking began. */
	static final int AGE_UNKNOWN = -1;

	/** Tier reported when the plant's seed tier is not known, such as a predicted planting. */
	static final int TIER_UNKNOWN = -1;

	private final TithePlotState state;
	private final int tier;
	private final int stage;
	private final int watersRemaining;
	private final int ageTicks;
	private final int stageAgeTicks;
	private final int plantAgeTicks;

	private PlotInfo(TithePlotState state, int tier, int stage, int watersRemaining, int ageTicks,
		int stageAgeTicks, int plantAgeTicks)
	{
		this.state = state;
		this.tier = tier;
		this.stage = stage;
		this.watersRemaining = watersRemaining;
		this.ageTicks = ageTicks;
		this.stageAgeTicks = stageAgeTicks;
		this.plantAgeTicks = plantAgeTicks;
	}

	/**
	 * Builds the snapshot for a plot from its current object id.
	 *
	 * @param objectId the plot's current object id
	 * @param ageTicks ticks since the plot changed to this id, or {@link #AGE_UNKNOWN}
	 * @return the plot snapshot
	 */
	static PlotInfo of(int objectId, int ageTicks)
	{
		return of(objectId, ageTicks, ageTicks);
	}

	/**
	 * Builds the snapshot for a plot from its current object id and both of its ages, with its planting unseen.
	 *
	 * @param objectId      the plot's current object id
	 * @param ageTicks      ticks since the plot changed to this id, or {@link #AGE_UNKNOWN}
	 * @param stageAgeTicks ticks since the plant entered its current growth stage, or {@link #AGE_UNKNOWN}
	 * @return the plot snapshot
	 */
	static PlotInfo of(int objectId, int ageTicks, int stageAgeTicks)
	{
		return of(objectId, ageTicks, stageAgeTicks, AGE_UNKNOWN);
	}

	/**
	 * Builds the snapshot for a plot from its current object id, both of its ages, and when its plant went in.
	 *
	 * @param objectId      the plot's current object id
	 * @param ageTicks      ticks since the plot changed to this id, or {@link #AGE_UNKNOWN}
	 * @param stageAgeTicks ticks since the plant entered its current growth stage, or {@link #AGE_UNKNOWN}
	 * @param plantAgeTicks ticks since the plant was seeded, or {@link #AGE_UNKNOWN}
	 * @return the plot snapshot
	 */
	static PlotInfo of(int objectId, int ageTicks, int stageAgeTicks, int plantAgeTicks)
	{
		return new PlotInfo(TithePlotState.fromObjectId(objectId), TithePlotState.tierOf(objectId),
			TithePlotState.stageOf(objectId), TithePlotState.watersRemaining(objectId), ageTicks, stageAgeTicks,
			plantAgeTicks);
	}

	/**
	 * Builds a predicted plot snapshot from a state and stage rather than an object id, for
	 * {@link ActionForecast} playing the run forward.
	 *
	 * @param state         the predicted state
	 * @param tier          the seed tier 0 to 2, or {@link #TIER_UNKNOWN}
	 * @param stage         the growth stage 1 to 3, or 0 when the state has none
	 * @param ageTicks      ticks in this state, or {@link #AGE_UNKNOWN}
	 * @param stageAgeTicks ticks in this growth stage, or {@link #AGE_UNKNOWN}
	 * @param plantAgeTicks ticks since the plant was seeded, or {@link #AGE_UNKNOWN}
	 * @return the plot snapshot
	 */
	static PlotInfo predicted(TithePlotState state, int tier, int stage, int ageTicks, int stageAgeTicks,
		int plantAgeTicks)
	{
		int waters = 0;
		if (state == TithePlotState.UNWATERED)
			waters = TitheFarmIds.WATERS_PER_CROP + 1 - stage;
		else if (state == TithePlotState.WATERED)
			waters = TitheFarmIds.WATERS_PER_CROP - stage;

		return new PlotInfo(state, tier, stage, waters, ageTicks, stageAgeTicks, plantAgeTicks);
	}

	/** The decoded plot state. */
	TithePlotState getState()
	{
		return state;
	}

	/** The plant's seed tier, 0 Golovanova to 2 Logavano, or {@link #TIER_UNKNOWN} for an empty or predicted plot. */
	int getTier()
	{
		return tier;
	}

	/** The growth stage 1 to 3, or 0 when the plot has no stage. */
	int getStage()
	{
		return stage;
	}

	/** Waters still needed before the plant is grown, 0 to 3. */
	int getWatersRemaining()
	{
		return watersRemaining;
	}

	/** Ticks since the plot changed to its current id, or {@link #AGE_UNKNOWN}. */
	int getAgeTicks()
	{
		return ageTicks;
	}

	/** Ticks since the plant entered its current growth stage, or {@link #AGE_UNKNOWN}. */
	int getStageAgeTicks()
	{
		return stageAgeTicks;
	}

	/** Ticks since the plant was seeded, or {@link #AGE_UNKNOWN} when it was not seen going in. */
	int getPlantAgeTicks()
	{
		return plantAgeTicks;
	}

	/**
	 * Ticks left before this plant dies for lack of water, or {@code -1} when it is not waiting for water or its
	 * age is unknown.
	 */
	int ticksUntilDeath()
	{
		if (state != TithePlotState.UNWATERED || ageTicks == AGE_UNKNOWN)
			return -1;

		return Math.max(0, TitheFarmIds.STAGE_TICKS - ageTicks);
	}

	/** Whether this is a seed planted moments ago — unwatered at stage 1. */
	boolean isFreshSeed()
	{
		return state == TithePlotState.UNWATERED && stage == 1;
	}

	/** Whether this plot will need a seed: empty, or dead and waiting to be cleared. */
	boolean needsSeed()
	{
		return state == TithePlotState.EMPTY || state == TithePlotState.DEAD;
	}
}
