/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.spatial.samples.startersample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Replace the wordmark with the owner-provided asset when available.
@Composable
internal fun EntryLogoPanel(onSummonTools: () -> Unit) {
  Box(
      modifier = Modifier.fillMaxSize().background(PosterPulse.Background),
      contentAlignment = Alignment.Center,
  ) {
    Canvas(Modifier.fillMaxSize()) {
      drawLine(
          color = PosterPulse.BrandRed.copy(alpha = 0.16f),
          start = Offset(0f, size.height * 0.95f),
          end = Offset(size.width * 0.27f, size.height * 0.54f),
          strokeWidth = 2.dp.toPx(),
      )
      drawLine(
          color = PosterPulse.BrandRed.copy(alpha = 0.16f),
          start = Offset(size.width * 0.73f, size.height * 0.46f),
          end = Offset(size.width, size.height * 0.05f),
          strokeWidth = 2.dp.toPx(),
      )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
          text = stringResource(R.string.entry_wordmark),
          color = PosterPulse.TextPrimary,
          fontSize = 120.sp,
          fontWeight = FontWeight.Black,
          fontStyle = FontStyle.Italic,
          letterSpacing = 3.sp,
      )
      Canvas(Modifier.padding(start = 12.dp).size(width = 66.dp, height = 82.dp)) {
        drawLine(
            color = PosterPulse.BrandRed,
            start = Offset(size.width * 0.07f, size.height),
            end = Offset(size.width * 0.54f, 0f),
            strokeWidth = 6.dp.toPx(),
        )
        drawLine(
            color = PosterPulse.BrandRed,
            start = Offset(size.width * 0.42f, size.height),
            end = Offset(size.width * 0.89f, 0f),
            strokeWidth = 6.dp.toPx(),
        )
      }
    }
    OutlinedButton(
        onClick = onSummonTools,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 38.dp),
        shape = PosterPulse.ControlShape,
        border = BorderStroke(1.dp, PosterPulse.BrandRed.copy(alpha = 0.65f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PosterPulse.TextPrimary),
    ) {
      Text(stringResource(R.string.open_tools), fontSize = 22.sp, letterSpacing = 2.sp)
    }
  }
}
