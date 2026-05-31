package me.rerere.rikkahub.ui.components.marketplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.FileImport
import me.rerere.hugeicons.stroke.McpServer
import me.rerere.hugeicons.stroke.Puzzle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.marketplace.ExtensionMarketPortal
import me.rerere.rikkahub.data.marketplace.ExtensionMarketplace
import me.rerere.rikkahub.data.marketplace.McpMarketItem
import me.rerere.rikkahub.data.marketplace.SkillMarketItem
import me.rerere.rikkahub.utils.openUrl

@Composable
fun SkillsMarketplaceSheet(
    isInstalling: Boolean,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit,
) {
    var customUrl by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("header") {
                SheetHeader(
                    title = stringResource(R.string.assistant_page_skills_market_title),
                    description = stringResource(R.string.assistant_page_skills_market_desc),
                )
            }

            item("custom-url") {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            singleLine = true,
                            label = { Text(stringResource(R.string.assistant_page_skills_market_url_label)) },
                            placeholder = {
                                Text(stringResource(R.string.assistant_page_skills_market_url_placeholder))
                            },
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = customUrl.isNotBlank() && !isInstalling,
                            onClick = { onInstall(customUrl.trim()) },
                        ) {
                            Icon(HugeIcons.FileImport, contentDescription = null, modifier = Modifier.size(18.dp))
                            Box(modifier = Modifier.width(8.dp))
                            Text(
                                if (isInstalling) {
                                    stringResource(R.string.assistant_page_skills_market_installing)
                                } else {
                                    stringResource(R.string.assistant_page_skills_market_install_url)
                                }
                            )
                        }
                    }
                }
            }

            item("featured-title") {
                SectionTitle(stringResource(R.string.assistant_page_skills_market_featured))
            }
            items(
                items = ExtensionMarketplace.skillItems,
                key = { it.downloadUrl },
            ) { item ->
                SkillMarketItemCard(
                    item = item,
                    isInstalling = isInstalling,
                    onInstall = { onInstall(item.downloadUrl) },
                )
            }

            item("sources-title") {
                SectionTitle(stringResource(R.string.assistant_page_skills_market_sources))
            }
            items(
                items = ExtensionMarketplace.skillPortals,
                key = { it.url },
            ) { portal ->
                MarketPortalCard(portal)
            }
        }
    }
}

@Composable
fun McpMarketplaceSheet(
    installedNames: Set<String>,
    onDismiss: () -> Unit,
    onInstall: (McpMarketItem) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("header") {
                SheetHeader(
                    title = stringResource(R.string.setting_mcp_page_market_title),
                    description = stringResource(R.string.setting_mcp_page_market_desc),
                )
            }

            item("featured-title") {
                SectionTitle(stringResource(R.string.setting_mcp_page_market_featured))
            }
            items(
                items = ExtensionMarketplace.mcpItems,
                key = { it.config.commonOptions.name },
            ) { item ->
                McpMarketItemCard(
                    item = item,
                    installed = item.config.commonOptions.name in installedNames,
                    onInstall = { onInstall(item) },
                )
            }

            item("sources-title") {
                SectionTitle(stringResource(R.string.setting_mcp_page_market_sources))
            }
            items(
                items = ExtensionMarketplace.mcpPortals,
                key = { it.url },
            ) { portal ->
                MarketPortalCard(portal)
            }
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun SkillMarketItemCard(
    item: SkillMarketItem,
    isInstalling: Boolean,
    onInstall: () -> Unit,
) {
    val context = LocalContext.current
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(HugeIcons.Puzzle, contentDescription = null)
                MarketText(
                    title = item.name,
                    source = item.source,
                    description = item.description,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = { context.openUrl(item.homepageUrl) }) {
                    Text(stringResource(R.string.assistant_page_skills_market_open))
                }
                OutlinedButton(
                    enabled = !isInstalling,
                    onClick = onInstall,
                ) {
                    Text(
                        if (isInstalling) {
                            stringResource(R.string.assistant_page_skills_market_installing)
                        } else {
                            stringResource(R.string.assistant_page_skills_market_install)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun McpMarketItemCard(
    item: McpMarketItem,
    installed: Boolean,
    onInstall: () -> Unit,
) {
    val context = LocalContext.current
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(HugeIcons.McpServer, contentDescription = null)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MarketText(
                        title = item.name,
                        source = item.source,
                        description = item.description,
                    )
                    item.setupHint?.let { hint ->
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = { context.openUrl(item.homepageUrl) }) {
                    Text(stringResource(R.string.setting_mcp_page_market_open))
                }
                OutlinedButton(
                    enabled = !installed,
                    onClick = onInstall,
                ) {
                    Text(
                        if (installed) {
                            stringResource(R.string.setting_mcp_page_market_installed)
                        } else {
                            stringResource(R.string.setting_mcp_page_market_install)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketPortalCard(portal: ExtensionMarketPortal) {
    val context = LocalContext.current
    Card(
        onClick = { context.openUrl(portal.url) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(HugeIcons.FileImport, contentDescription = null)
            MarketText(
                title = portal.name,
                source = portal.url,
                description = portal.description,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MarketText(
    title: String,
    source: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
