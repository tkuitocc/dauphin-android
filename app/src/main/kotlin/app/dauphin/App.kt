package app.dauphin

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.dauphin.views.screens.ClassScheduleScreen
import app.dauphin.views.screens.OtherScreen
import app.dauphin.views.screens.SettingsScreen

enum class AppDestinations(
    @StringRes val label: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    @StringRes val contentDescription: Int
) {
    CLASS_SCHEDULE(
        label = R.string.class_schedule,
        filledIcon = Icons.AutoMirrored.Filled.EventNote,
        outlinedIcon = Icons.AutoMirrored.Outlined.EventNote,
        contentDescription = R.string.class_schedule
    ),
    OTHER(
        label = R.string.other,
        filledIcon = Icons.AutoMirrored.Filled.Assignment,
        outlinedIcon = Icons.AutoMirrored.Outlined.Assignment,
        contentDescription = R.string.other
    ),
    SETTINGS(
        label = R.string.settings,
        filledIcon = Icons.Filled.Settings,
        outlinedIcon = Icons.Outlined.Settings,
        contentDescription = R.string.settings
    ),
}

@Composable
fun App() {
    var currentDestination by remember { mutableStateOf(value = AppDestinations.CLASS_SCHEDULE) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = if (currentDestination == it) it.filledIcon else it.outlinedIcon,
                            contentDescription = stringResource(id = it.contentDescription)
                        )
                    },
                    label = { Text(text = stringResource(id = it.label)) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.CLASS_SCHEDULE -> ClassScheduleScreen()
            AppDestinations.OTHER -> OtherScreen()
            AppDestinations.SETTINGS -> SettingsScreen {
                currentDestination = AppDestinations.CLASS_SCHEDULE
            }
        }
    }
}
