package app.dauphin

import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.dauphin.views.screens.ClassScheduleScreen
import app.dauphin.views.screens.SettingsScreen
import app.dauphin.views.screens.other.BarcodeScreen
import app.dauphin.views.screens.other.OtherScreen
import kotlinx.serialization.Serializable

data class TopNavKeyMetadata(
    @field:StringRes val label: Int,
    @field:StringRes val labelWithLineWrap: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    @field:StringRes val contentDescription: Int
)

@Serializable
enum class TopNavKey : NavKey { // ✅ 讓 enum 實作 NavKey，所有列舉常數就會自動成為 NavKey 的實例
    CLASS_SCHEDULE_SCREEN_NAV_KEY,
    OTHER_SCREEN_NAV_KEY,
    SETTINGS_SCREEN_NAV_KEY;

    val metadata: TopNavKeyMetadata
        get() = when (this) {
            CLASS_SCHEDULE_SCREEN_NAV_KEY ->
                TopNavKeyMetadata(
                    label = R.string.class_schedule,
                    labelWithLineWrap = R.string.class_schedule_line_wrap,
                    filledIcon = Icons.AutoMirrored.Filled.EventNote,
                    outlinedIcon = Icons.AutoMirrored.Outlined.EventNote,
                    contentDescription = R.string.class_schedule
                )

            OTHER_SCREEN_NAV_KEY ->
                TopNavKeyMetadata(
                    label = R.string.other,
                    labelWithLineWrap = R.string.other,
                    filledIcon = Icons.AutoMirrored.Filled.Assignment,
                    outlinedIcon = Icons.AutoMirrored.Outlined.Assignment,
                    contentDescription = R.string.other
                )

            SETTINGS_SCREEN_NAV_KEY ->
                TopNavKeyMetadata(
                    label = R.string.settings,
                    labelWithLineWrap = R.string.settings,
                    filledIcon = Icons.Filled.Settings,
                    outlinedIcon = Icons.Outlined.Settings,
                    contentDescription = R.string.settings
                )
        }
}

@Serializable
enum class OtherScreenNavKey : NavKey {
    BARCODE_SCREEN_NAV_KEY;
}

@Composable
fun NavDisplay(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> =
        rememberNavBackStack(TopNavKey.CLASS_SCHEDULE_SCREEN_NAV_KEY)
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        popTransitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        predictivePopTransitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        entryProvider = entryProvider {
            entry<TopNavKey> { navKey ->
                when (navKey) {
                    TopNavKey.CLASS_SCHEDULE_SCREEN_NAV_KEY -> {
                        ClassScheduleScreen()
                    }

                    TopNavKey.OTHER_SCREEN_NAV_KEY -> {
                        OtherScreen(
                            onNavigateToLogin = {
                                backStack.retainAll {
                                    it == TopNavKey.CLASS_SCHEDULE_SCREEN_NAV_KEY
                                }
                            },
                            onNavigateToBarcode = {
                                backStack.retainAll(
                                    elements = setOf(
                                        TopNavKey.CLASS_SCHEDULE_SCREEN_NAV_KEY,
                                        TopNavKey.OTHER_SCREEN_NAV_KEY,
                                        OtherScreenNavKey.BARCODE_SCREEN_NAV_KEY,
                                    )
                                )

                                if (
                                    !backStack.contains(
                                        element = TopNavKey.OTHER_SCREEN_NAV_KEY
                                    )
                                ) {
                                    backStack.add(element = TopNavKey.OTHER_SCREEN_NAV_KEY)
                                }

                                if (
                                    !backStack.contains(
                                        element = OtherScreenNavKey.BARCODE_SCREEN_NAV_KEY
                                    )
                                ) {
                                    backStack.add(
                                        element = OtherScreenNavKey.BARCODE_SCREEN_NAV_KEY
                                    )
                                }
                            }
                        )
                    }

                    TopNavKey.SETTINGS_SCREEN_NAV_KEY -> {
                        SettingsScreen(
                            onLoginLogout = {
                                backStack.retainAll {
                                    it == TopNavKey.CLASS_SCHEDULE_SCREEN_NAV_KEY
                                }
                            }
                        )
                    }
                }
            }

            entry<OtherScreenNavKey> { navKey ->
                when (navKey) {
                    OtherScreenNavKey.BARCODE_SCREEN_NAV_KEY -> {
                        BarcodeScreen(
                            onBack = {
                                backStack.retainAll(
                                    elements = setOf(
                                        TopNavKey.CLASS_SCHEDULE_SCREEN_NAV_KEY,
                                        TopNavKey.OTHER_SCREEN_NAV_KEY,
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App() {
    val windowContainerSize = LocalWindowInfo.current.containerSize

    val isHorizontal = windowContainerSize.width > windowContainerSize.height

    val isImeVisible = WindowInsets.isImeVisible

    val backStack = rememberNavBackStack(TopNavKey.CLASS_SCHEDULE_SCREEN_NAV_KEY)

    val backStackTopNavKey = backStack.findLast { it is TopNavKey } as? TopNavKey

    Surface(color = MaterialTheme.colorScheme.surface) {
        Surface(modifier = Modifier.safeDrawingPadding()) {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    TopNavKey.entries.forEach {
                        item(
                            icon = {
                                Icon(
                                    imageVector = when (backStackTopNavKey) {
                                        it -> it.metadata.filledIcon
                                        else -> it.metadata.outlinedIcon
                                    },
                                    contentDescription =
                                        stringResource(id = it.metadata.contentDescription)
                                )
                            },
                            label = {
                                when (isHorizontal) {
                                    true -> Text(
                                        text = stringResource(id = it.metadata.labelWithLineWrap),
                                        textAlign = TextAlign.Center
                                    )

                                    else -> Text(text = stringResource(id = it.metadata.label))
                                }
                            },
                            selected = it == backStackTopNavKey,
                            onClick = {
                                if (backStackTopNavKey != it) {
                                    backStack.retainAll(
                                        elements = setOf(
                                            TopNavKey.CLASS_SCHEDULE_SCREEN_NAV_KEY,
                                            it,
                                        )
                                    )

                                    if (!backStack.contains(element = it)) {
                                        backStack.add(element = it)
                                    }
                                }
                            }
                        )
                    }
                },
                layoutType = when {
                    backStack.lastOrNull() !is TopNavKey ->
                        NavigationSuiteType.None

                    isHorizontal ->
                        NavigationSuiteType.NavigationRail

                    isImeVisible ->
                        NavigationSuiteType.None

                    else ->
                        NavigationSuiteScaffoldDefaults
                            .calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
                }
            ) {
                NavDisplay(
                    backStack = backStack
                )
            }
        }
    }
}
