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

/** Verifies tick-to-time conversion and countdown formatting. */
public class TitheTimeTest
{
	@Test
	public void secondsRoundUp()
	{
		assertEquals(0, TitheTime.seconds(0));
		assertEquals(1, TitheTime.seconds(1));
		assertEquals(60, TitheTime.seconds(100));
	}

	@Test
	public void ticksRoundDown()
	{
		assertEquals(25, TitheTime.ticks(15));
		assertEquals(0, TitheTime.ticks(-3));
	}

	@Test
	public void formatsAsMinutesAndSeconds()
	{
		assertEquals("0:18", TitheTime.format(30));
		assertEquals("1:00", TitheTime.format(100));
	}

	@Test
	public void plotCountsDownOnlyWhileWaitingForWater()
	{
		assertEquals(40, PlotInfo.of(27387, 60).ticksUntilDeath());
		assertEquals(0, PlotInfo.of(27387, 140).ticksUntilDeath());
		assertEquals(-1, PlotInfo.of(27388, 60).ticksUntilDeath());
		assertEquals(-1, PlotInfo.of(27387, PlotInfo.AGE_UNKNOWN).ticksUntilDeath());
	}
}
