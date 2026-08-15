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

import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/** Verifies the plant-entry detection and the Cancel-to-top reorder. */
public class TitheMenuSwapperTest
{
	@Test
	public void detectsPlantEntriesOnEmptyPlot()
	{
		assertTrue(TitheMenuSwapper.isPlantEntry(MenuAction.GAME_OBJECT_FIRST_OPTION, 27383));
		assertTrue(TitheMenuSwapper.isPlantEntry(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, 27383));
	}

	@Test
	public void rejectsNonPlantEntries()
	{
		assertFalse(TitheMenuSwapper.isPlantEntry(MenuAction.GAME_OBJECT_FIRST_OPTION, 27384));
		assertFalse(TitheMenuSwapper.isPlantEntry(MenuAction.WALK, 27383));
		assertFalse(TitheMenuSwapper.isPlantEntry(MenuAction.CANCEL, 27383));
	}

	@Test
	public void promoteToTopMovesEntryToLastSlot()
	{
		MenuEntry a = mock(MenuEntry.class);
		MenuEntry b = mock(MenuEntry.class);
		MenuEntry c = mock(MenuEntry.class);
		MenuEntry[] result = TitheMenuSwapper.promoteToTop(new MenuEntry[]{a, b, c}, b);
		assertArrayEquals(new MenuEntry[]{a, c, b}, result);
	}
}
