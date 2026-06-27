package com.ezcanvas.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ezcanvas.demo.ui.EzColors

/** Hi-fi Screen 3 — "What you can build": cards that open a working example of the library. */
@Composable
fun GalleryScreen(
    onOpenExample: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(EzColors.Surface)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "What you can build",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = EzColors.Ink,
                )
                Text(
                    "Different uses of EZCanvas. Tap Take a look to open one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EzColors.Subtle,
                )
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EzColors.ChipBg)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "Close", tint = EzColors.Ink, modifier = Modifier.size(18.dp))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(Examples, key = { it.id }) { info ->
                ExampleCard(info) { onOpenExample(info.id) }
            }
        }
    }
}

@Composable
private fun ExampleCard(info: ExampleInfo, onOpen: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EzColors.Surface)
            .border(1.dp, EzColors.Divider, RoundedCornerShape(20.dp)),
    ) {
        CardPreview(info)
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(info.title, style = MaterialTheme.typography.titleMedium, color = EzColors.Ink)
                    Text(info.subtitle, style = MaterialTheme.typography.bodySmall, color = EzColors.Subtle)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(EzColors.Ink)
                        .clickable { onOpen() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Visibility, null, tint = EzColors.Surface, modifier = Modifier.size(14.dp))
                        Text("Take a look", color = EzColors.Surface, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (info.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    info.tags.forEach { TagChip(it) }
                }
            }
        }
    }
}

@Composable
private fun TagChip(label: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, EzColors.Divider, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, color = EzColors.Muted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CardPreview(info: ExampleInfo) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(info.background),
    ) {
        info.preview(this)
    }
}
