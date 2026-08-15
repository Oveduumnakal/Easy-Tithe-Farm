<p align="center">
  <img src="banner.png" alt="Tithe Farm Helper banner">
</p>
<h1 align="center">Tithe Farm Helper</h1>

Tithe Farm Helper is a RuneLite plugin that turns the Tithe Farm minigame into a follow-the-lights run:
it draws the most efficient planting route for the number of crops you want to grow, highlights the single
next thing to click, tracks your water, and stops you from planting a run you cannot finish watering.

## Features

### Plan the run

- **Optimal planting route**

  Set how many crops you want to grow per run and the plugin numbers every plot in the order to plant and
  water them — a tight snake through the patch so you never backtrack across a row.

  <img src="docs/img/01-route.gif" alt="Numbered planting route drawn over the plots" width="270">

### Know what to click next

- **Next-action highlight**

  One highlight at a time shows exactly what to do now — the next plot to plant, the plant that needs water,
  the grown plant to harvest, the barrel to refill at, or the table to grab seeds from. The matching
  inventory item (a seed, or a filled watering can) is boxed too.

  <img src="docs/img/02-next-action.gif" alt="Highlight of the next plot to water and the watering can" width="270">

### Never run dry

- **Water tracking**

  A small panel shows how much water you are carrying and how many crops it covers, counting both regular
  watering cans and Gricoller's watering can automatically.

- **Refill reminder**

  When you drop below what the run needs, the panel warns you and the water barrel is highlighted — with an
  optional notification so you catch it even while tabbed out.

- **Plant guard**

  Do not have enough water for the whole run? The plugin moves "Cancel" to the top of the menu so a stray
  click cancels instead of sinking a seed you cannot water through to harvest. It only reorders the menu —
  nothing is ever removed — and you can turn it off.

## Links

- [Report a bug](https://github.com/Oveduumnakal/Tithe-Farm-Helper-Plugin/issues/new?template=bug_report.yml)
- [Request a feature](https://github.com/Oveduumnakal/Tithe-Farm-Helper-Plugin/issues/new?template=feature_request.yml)
- [Buy me a coffee](https://buymeacoffee.com/oveduumnakal)
