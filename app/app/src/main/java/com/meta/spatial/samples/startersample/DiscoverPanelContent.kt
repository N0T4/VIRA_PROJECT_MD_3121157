package com.meta.spatial.samples.startersample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.spatial.samples.startersample.catalog.CatalogHome
import com.meta.spatial.samples.startersample.catalog.CatalogTitle

private val PosterAccents = listOf(
    Color(0xFFB03B46),
    Color(0xFF636FAD),
    Color(0xFF657F77),
    Color(0xFF927048),
)

@Composable
internal fun FeaturedDiscover(
    home: CatalogHome,
    onBrowse: () -> Unit,
    onTitleSelected: (CatalogTitle) -> Unit,
) {
  val featured = home.featured
  Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
  ) {
    Box(
        modifier = Modifier
            .weight(1.18f)
            .fillMaxHeight()
            .clip(PosterPulse.PanelShape)
            .border(1.dp, PosterPulse.Outline, PosterPulse.PanelShape),
    ) {
      CatalogArtwork(featured, accentOverride = PosterPulse.BrandRed)
      Box(
          Modifier.fillMaxSize().background(
              Brush.verticalGradient(
                  0f to PosterPulse.Background.copy(alpha = 0.2f),
                  0.33f to Color.Transparent,
                  0.63f to PosterPulse.Background.copy(alpha = 0.83f),
                  1f to PosterPulse.Background,
              ),
          ),
      )
      Column(
          modifier = Modifier.fillMaxSize().padding(PosterPulse.Space16),
          verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(PosterPulse.Space4)) {
          Text(
              "VIRA CATALOG  /  01",
              color = PosterPulse.TextPrimary,
              fontSize = PosterPulse.MetadataSize,
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold,
          )
          Row(
              horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                "DISCOVER",
                color = PosterPulse.TextPrimary,
                fontSize = PosterPulse.DisplaySize,
                lineHeight = PosterPulse.DisplaySize,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            Text("//", color = PosterPulse.FocusEdge, fontSize = 26.sp, fontWeight = FontWeight.Black)
          }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PosterPulse.Space4)) {
          Text(
              "FEATURED  /  01",
              color = PosterPulse.FocusEdge,
              fontSize = PosterPulse.MetadataSize,
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold,
          )
          Text(
              featured.title,
              color = PosterPulse.TextPrimary,
              fontSize = 30.sp,
              lineHeight = 32.sp,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
              fontWeight = FontWeight.Black,
          )
          Text(
              featured.synopsis,
              color = PosterPulse.TextSecondary,
              fontSize = PosterPulse.MetadataSize,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8)) {
            CatalogButton("Open card", { onTitleSelected(featured) }, selected = true)
            CatalogButton("Browse catalog", onBrowse)
          }
        }
      }
    }
    Column(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
    ) {
      Text(
          "FROM THE SHELF",
          color = PosterPulse.TextPrimary,
          fontSize = PosterPulse.MetadataSize,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp,
          modifier = Modifier.padding(start = PosterPulse.Space4),
      )
      for (row in home.tiles.take(4).chunked(2)) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
        ) {
          for (title in row) {
            CatalogTile(
                title,
                Modifier.weight(1f).fillMaxHeight().clickable { onTitleSelected(title) },
                showMetadata = false,
            )
          }
          if (row.size == 1) Box(Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
internal fun CatalogTile(title: CatalogTitle, modifier: Modifier, showMetadata: Boolean) {
  Box(
      modifier = modifier
          .clip(PosterPulse.ControlShape)
          .border(1.dp, PosterPulse.Outline, PosterPulse.ControlShape),
  ) {
    CatalogArtwork(title)
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.42f to PosterPulse.Background.copy(alpha = 0.2f),
                1f to PosterPulse.Background,
            ),
        ),
    )
    if (!title.hasDemoPoster || title.inDemoLibrary) {
      Text(
          if (!title.hasDemoPoster) "NO POSTER" else "DEMO · IN LIBRARY",
          color = PosterPulse.TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier
              .align(Alignment.TopStart)
              .padding(PosterPulse.Space8)
              .background(PosterPulse.Background.copy(alpha = 0.86f), PosterPulse.ControlShape)
              .padding(horizontal = PosterPulse.Space4),
      )
    }
    Column(
        modifier = Modifier.align(Alignment.BottomStart).padding(PosterPulse.Space8),
        verticalArrangement = Arrangement.spacedBy(PosterPulse.Space4),
    ) {
      Text(
          title.title,
          color = PosterPulse.TextPrimary,
          fontSize = PosterPulse.BodySize,
          lineHeight = 22.sp,
          fontWeight = FontWeight.Bold,
          maxLines = if (showMetadata) 3 else 2,
          overflow = TextOverflow.Ellipsis,
      )
      if (showMetadata) {
        Text(
            "${title.genre}  /  ${title.year}  /  ${title.format}",
            color = PosterPulse.TextSecondary,
            fontSize = PosterPulse.MetadataSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

// Graphic placeholders only. The fictional catalog does not ship licensed cover images.
@Composable
internal fun CatalogArtwork(title: CatalogTitle, accentOverride: Color? = null) {
  val accent = accentOverride ?: PosterAccents[(title.id.hashCode().ushr(1)) % PosterAccents.size]
  Canvas(Modifier.fillMaxSize()) {
    if (!title.hasDemoPoster) {
      drawRect(PosterPulse.Surface)
      drawLine(
          PosterPulse.Outline,
          Offset(size.width * 0.54f, size.height),
          Offset(size.width, 0f),
          strokeWidth = 1.dp.toPx(),
      )
      return@Canvas
    }

    val w = size.width
    val h = size.height
    val center = Offset(w * 0.72f, h * 0.38f)
    val radius = size.minDimension * 0.39f
    drawRect(Brush.linearGradient(listOf(PosterPulse.Background, accent.copy(alpha = 0.6f))))
    drawCircle(accent.copy(alpha = 0.72f), radius, center)
    drawCircle(PosterPulse.TextPrimary.copy(alpha = 0.34f), radius * 1.13f, center, style = Stroke(1.dp.toPx()))
    drawPath(
        Path().apply {
          moveTo(w * 0.29f, h)
          lineTo(w * 0.76f, h * 0.1f)
          lineTo(w, h * 0.02f)
          lineTo(w * 0.66f, h)
          close()
        },
        color = PosterPulse.Background.copy(alpha = 0.7f),
    )
    drawLine(accent, Offset(w * 0.43f, h), Offset(w * 0.92f, 0f), 2.dp.toPx())
    drawLine(
        PosterPulse.TextPrimary.copy(alpha = 0.3f),
        Offset(w * 0.58f, h),
        Offset(w, h * 0.22f),
        1.dp.toPx(),
    )
    for (row in 0..3) {
      for (column in 0..6) {
        drawCircle(
            PosterPulse.TextPrimary.copy(alpha = 0.22f),
            radius = 1.dp.toPx(),
            center = Offset(w * (0.65f + column * 0.04f), h * (0.08f + row * 0.045f)),
        )
      }
    }
    drawRect(accent, size = Size(3.dp.toPx(), h * 0.36f))
  }
}
