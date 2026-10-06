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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/** Verifies the step numbers written on the trail plots and where they sit. */
public class TitheHighlightOverlayTest
{
	@Test
	public void eachDistinctPlotIsNumberedInTrailOrder()
	{
		assertArrayEquals(new String[]{"1", "2", "3", "4", "5"},
			TitheHighlightOverlay.stepLabels(Arrays.asList(7, 8, 9, 10, 11)));
	}

	@Test
	public void aPlotThatComesUpTwiceShowsBothNumbersOnItsFirstStep()
	{
		assertArrayEquals(new String[]{"1,2", null, "3", "4,5", null},
			TitheHighlightOverlay.stepLabels(Arrays.asList(7, 7, 8, 9, 9)));
		assertArrayEquals(new String[]{"1,3", "2", null},
			TitheHighlightOverlay.stepLabels(Arrays.asList(4, 5, 4)));
	}

	@Test
	public void minimalViewTrailIsJustOne()
	{
		assertArrayEquals(new String[]{"1"}, TitheHighlightOverlay.stepLabels(Collections.singletonList(3)));
		assertArrayEquals(new String[0], TitheHighlightOverlay.stepLabels(Collections.emptyList()));
	}

	@Test
	public void northEastTileIsOneTileUpAndRightOfAPlotsCentre()
	{
		assertArrayEquals(new int[]{128, 128}, TitheHighlightOverlay.northEastOffset(3, 3));
		assertArrayEquals(new int[]{0, 0}, TitheHighlightOverlay.northEastOffset(1, 1));
		assertArrayEquals(new int[]{64, 128}, TitheHighlightOverlay.northEastOffset(2, 3));
	}
}
