package com.nolan.gaminghub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.navigation.compose.*

enum class Dest(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Games("Games", Icons.Filled.SportsEsports),
    Consoles("Consoles", Icons.Filled.Memory),
    Pad("Pad", Icons.Filled.Gamepad),
    Perf("Perf", Icons.Filled.Speed),
    Settings("Settings", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HubTheme { HubApp() } }
    }
}

@Composable
fun HubApp() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    Scaffold(
        containerColor = Navy,
        bottomBar = {
            NavigationBar(containerColor = NavyCard) {
                Dest.values().forEach { d ->
                    NavigationBarItem(
                        selected = current?.destination?.route == d.name,
                        onClick = {
                            nav.navigate(d.name) {
                                popUpTo(Dest.Home.name); launchSingleTop = true
                            }
                        },
                        icon = { Icon(d.icon, d.label) },
                        label = { Text(d.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White, indicatorColor = ElectricBlue,
                            selectedTextColor = ElectricBlue
                        )
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = Dest.Home.name, modifier = Modifier.padding(pad)) {
            composable(Dest.Home.name) { HomeScreen() }
            composable(Dest.Perf.name) { PerformanceScreen() }
            composable(Dest.Games.name) { Soon("My Games", "Stage 2", "Game library, importing, covers, search, favorites.") }
            composable(Dest.Consoles.name) { Soon("Console Center", "Stages 4-5", "PSP (PPSSPP) and PS2 emulator integration.") }
            composable(Dest.Pad.name) { Soon("Virtual Pad", "Stage 3", "Customizable on-screen gamepad and profiles.") }
            composable(Dest.Settings.name) { Soon("Settings", "Later stages", "Settings appear once there are features to configure.") }
        }
    }
}
