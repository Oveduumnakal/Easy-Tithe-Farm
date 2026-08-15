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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Verifies the plot-state decode against the real Tithe Farm object ids from RuneLite's plant tables. */
public class TithePlotStateTest
{
	@Test
	public void emptyPlotDecodes()
	{
		assertEquals(TithePlotState.EMPTY, TithePlotState.fromObjectId(27383));
	}

	@Test
	public void golovanovaStagesDecode()
	{
		assertEquals(TithePlotState.UNWATERED, TithePlotState.fromObjectId(27384));
		assertEquals(TithePlotState.WATERED, TithePlotState.fromObjectId(27385));
		assertEquals(TithePlotState.DEAD, TithePlotState.fromObjectId(27386));
		assertEquals(TithePlotState.UNWATERED, TithePlotState.fromObjectId(27387));
		assertEquals(TithePlotState.UNWATERED, TithePlotState.fromObjectId(27390));
		assertEquals(TithePlotState.GROWN, TithePlotState.fromObjectId(27393));
		assertEquals(TithePlotState.DEAD, TithePlotState.fromObjectId(27394));
	}

	@Test
	public void bologanoAndLogavanoBoundariesDecode()
	{
		assertEquals(TithePlotState.UNWATERED, TithePlotState.fromObjectId(27395));
		assertEquals(TithePlotState.GROWN, TithePlotState.fromObjectId(27404));
		assertEquals(TithePlotState.DEAD, TithePlotState.fromObjectId(27405));
		assertEquals(TithePlotState.UNWATERED, TithePlotState.fromObjectId(27406));
		assertEquals(TithePlotState.GROWN, TithePlotState.fromObjectId(27415));
		assertEquals(TithePlotState.DEAD, TithePlotState.fromObjectId(27416));
	}

	@Test
	public void nonPlotIdsAreRejected()
	{
		assertEquals(TithePlotState.NOT_A_PLOT, TithePlotState.fromObjectId(27382));
		assertEquals(TithePlotState.NOT_A_PLOT, TithePlotState.fromObjectId(27417));
		assertEquals(TithePlotState.NOT_A_PLOT, TithePlotState.fromObjectId(0));
	}

	@Test
	public void stageDecodes()
	{
		assertEquals(1, TithePlotState.stageOf(27384));
		assertEquals(2, TithePlotState.stageOf(27387));
		assertEquals(3, TithePlotState.stageOf(27390));
		assertEquals(0, TithePlotState.stageOf(27393));
		assertEquals(0, TithePlotState.stageOf(27383));
	}

	@Test
	public void watersRemainingCountsFutureStages()
	{
		assertEquals(3, TithePlotState.watersRemaining(27384));
		assertEquals(2, TithePlotState.watersRemaining(27387));
		assertEquals(1, TithePlotState.watersRemaining(27390));
		assertEquals(2, TithePlotState.watersRemaining(27385));
		assertEquals(1, TithePlotState.watersRemaining(27388));
		assertEquals(0, TithePlotState.watersRemaining(27391));
		assertEquals(0, TithePlotState.watersRemaining(27383));
		assertEquals(0, TithePlotState.watersRemaining(27393));
	}

	@Test
	public void regularCanChargesDecode()
	{
		assertEquals(0, TitheFarmIds.regularCanCharges(5331));
		assertEquals(1, TitheFarmIds.regularCanCharges(5333));
		assertEquals(8, TitheFarmIds.regularCanCharges(5340));
		assertEquals(-1, TitheFarmIds.regularCanCharges(5332));
		assertEquals(-1, TitheFarmIds.regularCanCharges(995));
	}
}
