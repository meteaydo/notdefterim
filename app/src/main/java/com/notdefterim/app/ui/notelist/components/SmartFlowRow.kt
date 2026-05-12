package com.notdefterim.app.ui.notelist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SmartFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hSpacingPx = horizontalSpacing.roundToPx()
        val vSpacingPx = verticalSpacing.roundToPx()

        // Measure all children with unconstrained width
        val childConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(childConstraints) }.toMutableList()

        val rows = mutableListOf<List<Placeable>>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        while (placeables.isNotEmpty()) {
            // Find the first placeable that fits in the remaining width
            // If the row is empty, we must take the first item even if it exceeds maxWidth
            val remainingWidth = constraints.maxWidth - currentRowWidth
            
            val indexToTake = if (currentRow.isEmpty()) {
                0
            } else {
                placeables.indexOfFirst { it.width <= remainingWidth }
            }
            
            if (indexToTake != -1) {
                val p = placeables.removeAt(indexToTake)
                currentRow.add(p)
                currentRowWidth += p.width + hSpacingPx
                currentRowHeight = maxOf(currentRowHeight, p.height)
            } else {
                // Nothing fits in the current row, finish it
                rows.add(currentRow)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }
        }
        
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowHeights.add(currentRowHeight)
        }

        val totalHeight = rowHeights.sum() + maxOf(0, rows.size - 1) * vSpacingPx

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            for ((rowIndex, row) in rows.withIndex()) {
                var x = 0
                for (placeable in row) {
                    placeable.placeRelative(x = x, y = y)
                    x += placeable.width + hSpacingPx
                }
                y += rowHeights[rowIndex] + vSpacingPx
            }
        }
    }
}
