package com.mindvault.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindvault.app.R
import com.mindvault.app.ui.navigation.Screen
import com.mindvault.app.ui.screens.home.HomeFilter

@Composable
fun AppDrawer(
    currentRoute: String?,
    drawerState: DrawerState,
    onNavigate: (String) -> Unit,
    onFilterFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF26215C))
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "MindVault logo",
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "MindVault",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        NavigationDrawerItem(
            label = { Text("All Notes") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        NavigationDrawerItem(
            label = { Text("Favorites") },
            icon = { Icon(Icons.Default.Star, contentDescription = null) },
            selected = false,
            onClick = {
                onFilterFavorites()
                onNavigate(Screen.Home.route)
            },
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("Archive") },
            icon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
            selected = currentRoute == Screen.Archive.route,
            onClick = { onNavigate(Screen.Archive.route) },
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        NavigationDrawerItem(
            label = { Text("Trash") },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            selected = currentRoute == Screen.Trash.route,
            onClick = { onNavigate(Screen.Trash.route) },
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("Categories") },
            icon = { Icon(Icons.Default.Category, contentDescription = null) },
            selected = currentRoute == Screen.Categories.route,
            onClick = { onNavigate(Screen.Categories.route) },
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("Settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
