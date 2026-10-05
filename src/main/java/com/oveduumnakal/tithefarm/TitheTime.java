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

/** Converts game ticks into the seconds and {@code m:ss} countdowns shown to the player. Pure and static. */
final class TitheTime
{
	/** Milliseconds in one game tick. */
	static final int TICK_MILLIS = 600;

	private TitheTime()
	{
	}

	/**
	 * Whole seconds in a tick count, rounded up so a countdown never shows zero while time is left.
	 *
	 * @param ticks the tick count
	 * @return the seconds, never negative
	 */
	static int seconds(int ticks)
	{
		return Math.max(0, (ticks * TICK_MILLIS + 999) / 1000);
	}

	/**
	 * Ticks in a number of seconds, rounded down.
	 *
	 * @param seconds the seconds
	 * @return the ticks, never negative
	 */
	static int ticks(int seconds)
	{
		return Math.max(0, seconds * 1000 / TICK_MILLIS);
	}

	/**
	 * A tick count as {@code m:ss}, e.g. {@code 0:18}.
	 *
	 * @param ticks the tick count
	 * @return the formatted countdown
	 */
	static String format(int ticks)
	{
		int seconds = seconds(ticks);
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
