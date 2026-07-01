package com.citruschat.citrusmobile.ui.shared.component

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults.windowInsets
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.navigation.Routes

@Composable
fun NavigationComponent(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    homeUnreadCount: Int = 0,
) {
    val selectedColor = MaterialTheme.colorScheme.primary // WhatsApp green
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    NavigationBar(
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.background,
        windowInsets = windowInsets.only(sides = WindowInsetsSides.Horizontal),
        modifier = Modifier.height(dimensionResource(R.dimen.navbar_height)),
    ) {
        NavigationBarItem(
            selected = currentRoute == Routes.Home,
            onClick = onHomeClick,
            icon = {
                BadgedBox(
                    badge = {
                        if (homeUnreadCount > 0) {
                            Badge {
                                Text(text = homeUnreadCount.badgeText())
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                    )
                }
            },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor,
                    indicatorColor = selectedColor.copy(alpha = 0.18f),
                ),
        )

        NavigationBarItem(
            selected = currentRoute == Routes.Profile,
            onClick = onProfileClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                )
            },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor,
                    indicatorColor = selectedColor.copy(alpha = 0.18f),
                ),
        )
    }
}

private fun Int.badgeText(): String = if (this > MAX_BADGE_COUNT) MAX_BADGE_TEXT else toString()

private const val MAX_BADGE_COUNT = 99
private const val MAX_BADGE_TEXT = "99+"
