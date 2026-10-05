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
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies the saved-route encoding round-trips and tolerates damaged input. */
public class RouteRecorderTest
{
	@Test
	public void encodesAndDecodesInOrder()
	{
		List<int[]> tiles = Arrays.asList(new int[]{1810, 3501}, new int[]{1814, 3498});
		String encoded = RouteRecorder.encode(tiles);
		assertEquals("1810,3501;1814,3498", encoded);
		List<int[]> decoded = RouteRecorder.decode(encoded);
		assertEquals(2, decoded.size());
		assertArrayEquals(tiles.get(0), decoded.get(0));
		assertArrayEquals(tiles.get(1), decoded.get(1));
	}

	@Test
	public void emptyOrNullDecodesToNothing()
	{
		assertTrue(RouteRecorder.decode("").isEmpty());
		assertTrue(RouteRecorder.decode(null).isEmpty());
	}

	@Test
	public void skipsMalformedEntries()
	{
		List<int[]> decoded = RouteRecorder.decode("1,2;oops;3;4,x; 5 , 6 ");
		assertEquals(2, decoded.size());
		assertArrayEquals(new int[]{1, 2}, decoded.get(0));
		assertArrayEquals(new int[]{5, 6}, decoded.get(1));
	}
}
