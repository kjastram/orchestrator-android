package com.orchestrator.app.ui.finance.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orchestrator.app.ui.finance.MoneyFormat
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

/** One labelled data series for [GroupedBarChart]. */
data class ChartSeries(
    val label: String,
    val color: Color,
    val values: List<Float>
)

/**
 * Grouped column chart — the cash-flow centerpiece (e.g. income vs spending per month).
 * All Vico usage is intentionally contained in this file so the charting dependency is
 * easy to swap or pin.
 */
@Composable
fun GroupedBarChart(
    labels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    if (series.isEmpty() || labels.isEmpty()) return

    val producer = remember(series) {
        ChartEntryModelProducer(
            series.map { s -> s.values.mapIndexed { index, v -> entryOf(index.toFloat(), v) } }
        )
    }

    val columns = series.map { s ->
        lineComponent(
            color = s.color,
            thickness = 10.dp,
        )
    }

    val bottomFormatter = AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Horizontal.Bottom> { value, _ ->
        labels.getOrNull(value.toInt()).orEmpty()
    }
    val startFormatter = AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Vertical.Start> { value, _ ->
        MoneyFormat.compact(value.toDouble())
    }

    Chart(
        chart = columnChart(
            columns = columns,
            mergeMode = ColumnChart.MergeMode.Grouped
        ),
        chartModelProducer = producer,
        startAxis = rememberStartAxis(valueFormatter = startFormatter),
        bottomAxis = rememberBottomAxis(valueFormatter = bottomFormatter),
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
    )
}
