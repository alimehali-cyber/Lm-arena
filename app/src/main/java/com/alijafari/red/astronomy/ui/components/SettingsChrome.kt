package com.alijafari.red.astronomy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.ui.aether.AetherTheme
import com.alijafari.red.astronomy.ui.spatial.SpatialTheme
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.readableInkOn

internal enum class SettingsPage {
    GENERAL,
    APPEARANCE,
    SKY,
    ABOUT,
}

@Composable
internal fun SettingsCategoryTabs(
    selected: SettingsPage,
    uiStyle: AppUiStyle,
    onSelect: (SettingsPage) -> Unit,
) {
    val accent = when (uiStyle) {
        AppUiStyle.DEFAULT -> RedTheme.colors.accentRed
        AppUiStyle.SPATIAL -> SpatialTheme.palette.accentInk
        AppUiStyle.AETHER -> AetherTheme.palette.crimsonGlow
    }
    val selectedInk = readableInkOn(accent)
    val unselectedInk = when (uiStyle) {
        AppUiStyle.DEFAULT -> RedTheme.colors.textSecondary
        AppUiStyle.SPATIAL -> SpatialTheme.palette.textSecondary
        AppUiStyle.AETHER -> AetherTheme.palette.textSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsPage.entries.forEach { page ->
            val isSelected = page == selected
            val label = stringResource(
                when (page) {
                    SettingsPage.GENERAL -> R.string.settings_group_general
                    SettingsPage.APPEARANCE -> R.string.settings_group_appearance
                    SettingsPage.SKY -> R.string.settings_group_sky
                    SettingsPage.ABOUT -> R.string.settings_group_about
                }
            )
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accent else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) accent else unselectedInk.copy(alpha = 0.24f),
                        shape = CircleShape,
                    )
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(page) })
                    .padding(horizontal = 6.dp, vertical = 11.dp),
                color = if (isSelected) selectedInk else unselectedInk,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun settingsPageTitle(page: SettingsPage, isFa: Boolean): String = when (page) {
    SettingsPage.GENERAL -> if (isFa) "تنظیمات عمومی" else "General"
    SettingsPage.APPEARANCE -> if (isFa) "ظاهر برنامه" else "Appearance"
    SettingsPage.SKY -> if (isFa) "رصد و آسمان" else "Sky & observing"
    SettingsPage.ABOUT -> if (isFa) "دربارهٔ زیگ" else "About ZIG"
}
