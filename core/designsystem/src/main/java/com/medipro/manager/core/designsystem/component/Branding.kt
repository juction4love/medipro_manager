package com.medipro.manager.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medipro.manager.core.designsystem.R

@Composable
fun MediProLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    compact: Boolean = false
) {
    Image(
        painter = painterResource(
            if (compact) R.drawable.app_logo else R.drawable.logo
        ),
        contentDescription = "MediPro",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun MediProAppBarTitle(
    title: String,
    modifier: Modifier = Modifier,
    showLogo: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            MediProLogo(size = 32.dp, compact = true)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        } else {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        }
    }
}
