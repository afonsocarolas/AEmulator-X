/* Added for AEmulator X, 2026-10-19: Android time machine library view. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aemu.core.GuestImage

/** Android releases by API level: the year it came out and its name. */
object AndroidEras {
    fun year(api: Int): Int = when {
        api <= 1 -> 2008
        api <= 6 -> 2009          // 1.1 Cupcake Donut Eclair 2.0
        api <= 9 -> 2010          // 2.1 2.2 2.3
        api <= 15 -> 2011         // 2.3.3 Honeycomb ICS
        api <= 17 -> 2012
        api <= 19 -> 2013
        api <= 21 -> 2014
        api <= 23 -> 2015
        api <= 25 -> 2016
        api <= 27 -> 2017
        api == 28 -> 2018
        api == 29 -> 2019
        api == 30 -> 2020
        api == 31 -> 2021
        api <= 33 -> 2022
        api == 34 -> 2023
        api == 35 -> 2024
        api == 36 -> 2025
        else -> 2026
    }

    fun name(api: Int): String = when {
        api <= 2 -> "Android 1.0–1.1"
        api == 3 -> "Cupcake"
        api == 4 -> "Donut"
        api <= 7 -> "Eclair"
        api == 8 -> "Froyo"
        api <= 10 -> "Gingerbread"
        api <= 13 -> "Honeycomb"
        api <= 15 -> "Ice Cream Sandwich"
        api <= 18 -> "Jelly Bean"
        api <= 20 -> "KitKat"
        api <= 22 -> "Lollipop"
        api == 23 -> "Marshmallow"
        api <= 25 -> "Nougat"
        api <= 27 -> "Oreo"
        api == 28 -> "Pie"
        api == 29 -> "Android 10"
        api == 30 -> "Android 11"
        api <= 32 -> "Android 12"          // 32 = 12L
        else -> "Android ${api - 20}"      // 33 = 13 … 37 = 17
    }

    /** [imgs] by release year, oldest first; inside a year by API level. */
    fun byYear(imgs: List<GuestImage>): List<Pair<Int, List<GuestImage>>> =
        imgs.groupBy { year(it.api) }.toSortedMap().map { (y, l) -> y to l.sortedWith(compareBy({ it.api }, { it.name.lowercase() })) }
}

/**
 * The library as a time machine: a vertical line from 2008 to today, one stop per release year with its Android
 * names, and the firmwares of that year as square tiles side by side.
 */
@Composable
internal fun TimelineLibrary(
    images: List<GuestImage>,
    header: @Composable () -> Unit,
    tile: @Composable (GuestImage) -> Unit,
) {
    val years = AndroidEras.byYear(images)
    val line = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)) {
        item { header() }
        items(years, key = { it.first }) { (year, list) ->
            Row(Modifier.padding(top = 4.dp)) {
                // the line with a dot for the year
                Box(Modifier.width(28.dp).height(220.dp), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(line))
                    Box(Modifier.padding(top = 6.dp).size(16.dp).clip(CircleShape).background(XBrand.gradient))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.fillMaxSize()) {
                    Text("$year", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text(list.map { AndroidEras.name(it.api) }.distinct().joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(list, key = { it.id }) { img -> Box(Modifier.width(150.dp)) { tile(img) } }
                    }
                }
            }
        }
    }
}
