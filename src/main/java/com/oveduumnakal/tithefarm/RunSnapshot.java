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

import java.util.Collections;
import java.util.List;

import net.runelite.api.GameObject;

/**
 * One frame's view of the run, built by {@link TitheRun}: the route plots in order with their snapshots, the
 * water carried and still needed, the advised next action, and the trail of plot actions predicted after it.
 */
final class RunSnapshot
{
	private final List<GameObject> route;
	private final int routeLength;
	private final List<PlotInfo> plots;
	private final int water;
	private final int runNeed;
	private final boolean canAffordPlant;
	private final ActionAdvisor.Advice advice;
	private final boolean recording;
	private final int recordedCount;
	private final RunStatus status;
	private final List<ActionAdvisor.Advice> trail;

	/**
	 * Creates a snapshot.
	 *
	 * @param route          the route plot objects in planting order, then planted plots off the route
	 * @param routeLength    how many leading entries of {@code route} are the numbered route
	 * @param plots          the matching plot snapshots, index for index
	 * @param water          the water charges carried
	 * @param runNeed        the water needed to finish the run
	 * @param canAffordPlant whether a seed can be planted with water enough for the whole run
	 * @param advice         the advised next action
	 * @param recording      whether a route recording is in progress
	 * @param recordedCount  how many plots the recording holds so far
	 * @param status         the run's state beyond its plots
	 * @param trail          the current plot action and the predicted ones after it, one per plot
	 */
	RunSnapshot(List<GameObject> route, int routeLength, List<PlotInfo> plots, int water, int runNeed,
		boolean canAffordPlant, ActionAdvisor.Advice advice, boolean recording, int recordedCount, RunStatus status,
		List<ActionAdvisor.Advice> trail)
	{
		this.route = Collections.unmodifiableList(route);
		this.routeLength = routeLength;
		this.plots = Collections.unmodifiableList(plots);
		this.water = water;
		this.runNeed = runNeed;
		this.canAffordPlant = canAffordPlant;
		this.advice = advice;
		this.recording = recording;
		this.recordedCount = recordedCount;
		this.status = status;
		this.trail = Collections.unmodifiableList(trail);
	}

	/** The run's state beyond its plots: sack, tools, energy, fruit carried. */
	RunStatus getStatus()
	{
		return status;
	}

	/** The route plot objects in planting order, followed by any planted plots off the route. */
	List<GameObject> getRoute()
	{
		return route;
	}

	/** How many leading entries of {@link #getRoute()} are the numbered route. */
	int getRouteLength()
	{
		return routeLength;
	}

	/** The targeted plot's route number, or 0 when the advice targets no plot or a plot off the route. */
	int getTargetNumber()
	{
		int index = advice.getPlotIndex();
		return index >= 0 && index < routeLength ? index + 1 : 0;
	}

	/** The plot snapshots, index for index with {@link #getRoute()}. */
	List<PlotInfo> getPlots()
	{
		return plots;
	}

	/** The water charges carried. */
	int getWater()
	{
		return water;
	}

	/** The water needed to finish the run. */
	int getRunNeed()
	{
		return runNeed;
	}

	/** Whether the water carried falls short of what the run needs. */
	boolean isShort()
	{
		return water < runNeed;
	}

	/** Whether a seed can be planted with water enough for the whole run. */
	boolean canAffordPlant()
	{
		return canAffordPlant;
	}

	/** Whether nothing is growing: the run has not started yet, or every plant has been harvested. */
	boolean isBetweenRuns()
	{
		return nothingGrowing(plots);
	}

	/**
	 * Whether none of the given plots holds a living plant.
	 *
	 * @param plots the plots to check
	 * @return true when every plot is empty or dead
	 */
	static boolean nothingGrowing(List<PlotInfo> plots)
	{
		for (PlotInfo plot : plots)
		{
			if (!plot.needsSeed())
				return false;
		}

		return true;
	}

	/** The advised next action. */
	ActionAdvisor.Advice getAdvice()
	{
		return advice;
	}

	/**
	 * The current plot action followed by the predicted ones after it, each on a different plot, from
	 * {@link ActionForecast}. Empty when the advice is not a plot action.
	 */
	List<ActionAdvisor.Advice> getTrail()
	{
		return trail;
	}

	/** The plot object at a route index, or {@code null} when the index is out of range. */
	GameObject plotAt(int index)
	{
		return index >= 0 && index < route.size() ? route.get(index) : null;
	}

	/** The plot object the advice targets, or {@code null} for a non-plot action. */
	GameObject getTargetPlot()
	{
		return plotAt(advice.getPlotIndex());
	}

	/** Whether a route recording is in progress. */
	boolean isRecording()
	{
		return recording;
	}

	/** How many plots the recording holds so far. */
	int getRecordedCount()
	{
		return recordedCount;
	}
}
