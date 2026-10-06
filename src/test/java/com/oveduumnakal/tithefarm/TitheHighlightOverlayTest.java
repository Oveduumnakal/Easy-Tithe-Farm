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

import static org.junit.Assert.assertArrayEquals;

/** Verifies where the step markers sit on a trail plot, and which model vertices are the plant. */
public class TitheHighlightOverlayTest
{
	@Test
	public void onlyVerticesAboveTheSoilAreThePlant()
	{
		float[] heights = {0, -4, -8, -9, -80, -347};
		assertArrayEquals(new int[]{3, 4, 5}, TitheHighlightOverlay.plantVertices(heights, heights.length));
	}

	@Test
	public void aBarePatchHasNoPlantVertices()
	{
		float[] heights = {0, 0, 0, 0, -4};
		assertArrayEquals(new int[0], TitheHighlightOverlay.plantVertices(heights, heights.length));
	}

	@Test
	public void northEastTileIsOneTileUpAndRightOfAPlotsCentre()
	{
		assertArrayEquals(new int[]{128, 128}, TitheHighlightOverlay.northEastOffset(3, 3));
		assertArrayEquals(new int[]{0, 0}, TitheHighlightOverlay.northEastOffset(1, 1));
		assertArrayEquals(new int[]{64, 128}, TitheHighlightOverlay.northEastOffset(2, 3));
	}
}
