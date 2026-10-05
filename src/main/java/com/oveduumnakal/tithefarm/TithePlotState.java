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
 * The state of a single Tithe Farm plot, decoded from its scene object id.
 *
 * <p>A plot moves EMPTY -> (plant a seed) -> UNWATERED -> (water it) -> WATERED -> (it grows) -> UNWATERED at
 * the next stage, and so on for three stages, ending GROWN (harvestable). Miss a stage's water before the
 * plot advances and it becomes DEAD. The decode mirrors RuneLite's built-in Tithe Farm plugin: each seed
 * tier owns eleven consecutive object ids — three stages of dry/wet/dead, a grown id, then a dead-grown id —
 * and the state is the id's offset below its tier's grown id, taken modulo three.
 */
enum TithePlotState
{
	/** No plant — an empty, plantable plot. */
	EMPTY,

	/** A plant that needs watering now to survive to its next stage. */
	UNWATERED,

	/** A plant watered for its current stage; leave it until it grows. */
	WATERED,

	/** A fully grown plant, ready to harvest. */
	GROWN,

	/** A blighted plant that missed a watering; it yields nothing. */
	DEAD,

	/** The object id is not a Tithe Farm plot. */
	NOT_A_PLOT;

	/**
	 * Decodes a scene object id into a plot state.
	 *
	 * @param objectId the plot object's id
	 * @return the decoded state, or {@link #NOT_A_PLOT} when the id is not a plot
	 */
	static TithePlotState fromObjectId(int objectId)
	{
		if (objectId == TitheFarmIds.PLOT_EMPTY)
			return EMPTY;

		if (objectId < TitheFarmIds.PLOT_GROWTH_FIRST || objectId > TitheFarmIds.PLOT_GROWTH_LAST)
			return NOT_A_PLOT;

		int grownId = grownIdFor(objectId);
		if (objectId == grownId)
			return GROWN;

		int remainder = (grownId - objectId) % TitheFarmIds.WATERS_PER_CROP;
		if (remainder == 0)
			return UNWATERED;

		if (remainder == 2)
			return WATERED;

		return DEAD;
	}

	/**
	 * The growth stage of a plot, 1 to 3, or 0 when the id has no meaningful stage (empty, grown, dead-grown,
	 * or not a plot). Each stage spans a dry/wet/dead triple of ids within the tier.
	 *
	 * @param objectId the plot object's id
	 * @return the stage 1 to 3, or 0
	 */
	static int stageOf(int objectId)
	{
		if (objectId < TitheFarmIds.PLOT_GROWTH_FIRST || objectId > TitheFarmIds.PLOT_GROWTH_LAST)
			return 0;

		int tierIndex = (objectId - TitheFarmIds.PLOT_GROWTH_FIRST) / TitheFarmIds.TIER_SPAN;
		int local = objectId - (TitheFarmIds.PLOT_GROWTH_FIRST + tierIndex * TitheFarmIds.TIER_SPAN);
		if (local > 8)
			return 0;

		return local / TitheFarmIds.WATERS_PER_CROP + 1;
	}

	/**
	 * How many more waters a plot needs before it is grown, given its object id. A fresh seed needs three; an
	 * unwatered plant at stage {@code s} needs {@code 4 - s}; a watered plant at stage {@code s} needs
	 * {@code 3 - s}. Empty, grown, dead, and non-plot ids need none.
	 *
	 * @param objectId the plot object's id
	 * @return the waters still required, 0 to 3
	 */
	static int watersRemaining(int objectId)
	{
		TithePlotState state = fromObjectId(objectId);
		int stage = stageOf(objectId);
		if (state == UNWATERED)
			return TitheFarmIds.WATERS_PER_CROP + 1 - stage;

		if (state == WATERED)
			return TitheFarmIds.WATERS_PER_CROP - stage;

		return 0;
	}

	/**
	 * Whether an object id is a seed that was planted moments ago: unwatered at growth stage 1. A plot that
	 * changes from {@link #EMPTY} to this id has just been planted.
	 *
	 * @param objectId the plot object's id
	 * @return true for a stage-1 unwatered plant
	 */
	static boolean isFreshSeed(int objectId)
	{
		return fromObjectId(objectId) == UNWATERED && stageOf(objectId) == 1;
	}

	/** The grown (harvestable) object id of the tier that owns the given growth object id. */
	private static int grownIdFor(int objectId)
	{
		int tierIndex = (objectId - TitheFarmIds.PLOT_GROWTH_FIRST) / TitheFarmIds.TIER_SPAN;
		return TitheFarmIds.PLOT_A_GROWN + tierIndex * TitheFarmIds.TIER_SPAN;
	}
}
