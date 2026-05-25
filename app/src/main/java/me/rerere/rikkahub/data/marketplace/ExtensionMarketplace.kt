package me.rerere.rikkahub.data.marketplace

import me.rerere.rikkahub.data.ai.mcp.McpCommonOptions
import me.rerere.rikkahub.data.ai.mcp.McpServerConfig

private const val TERMUX_HOME = "/data/data/com.termux/files/home"

data class ExtensionMarketPortal(
    val name: String,
    val description: String,
    val url: String,
)

data class SkillMarketItem(
    val name: String,
    val description: String,
    val source: String,
    val downloadUrl: String,
    val homepageUrl: String,
)

data class McpMarketItem(
    val name: String,
    val description: String,
    val source: String,
    val config: McpServerConfig,
    val homepageUrl: String,
    val setupHint: String? = null,
)

object ExtensionMarketplace {
    val skillPortals = listOf(
        ExtensionMarketPortal(
            name = "Smithery",
            description = "Marketplace for MCP tools and agent skills.",
            url = "https://smithery.ai/",
        ),
        ExtensionMarketPortal(
            name = "LLM Skills",
            description = "Community directory focused on reusable LLM skills.",
            url = "https://llmskills.dev/",
        ),
        ExtensionMarketPortal(
            name = "GitHub Skill Search",
            description = "Search public repositories that publish SKILL.md packages.",
            url = "https://github.com/search?q=SKILL.md+agent+skill&type=repositories",
        ),
    )

    val skillItems = listOf(
        SkillMarketItem(
            name = "Smithery CLI",
            description = "Discover, connect, and use MCP tools and skills through the Smithery CLI.",
            source = "Smithery",
            downloadUrl = "https://smithery.ai/skills/smithery-ai/cli/.well-known/skills/smithery-ai-cli/SKILL.md",
            homepageUrl = "https://smithery.ai/",
        ),
    )

    val mcpPortals = listOf(
        ExtensionMarketPortal(
            name = "Official MCP Registry",
            description = "Official registry for discoverable Model Context Protocol servers.",
            url = "https://registry.modelcontextprotocol.io/",
        ),
        ExtensionMarketPortal(
            name = "Glama MCP",
            description = "Large searchable MCP server directory with metadata and categories.",
            url = "https://glama.ai/mcp/servers",
        ),
        ExtensionMarketPortal(
            name = "Smithery",
            description = "Hosted marketplace for MCP tools, skills, and managed connections.",
            url = "https://smithery.ai/",
        ),
        ExtensionMarketPortal(
            name = "PulseMCP",
            description = "MCP discovery site with server listings, news, and ecosystem updates.",
            url = "https://www.pulsemcp.com/servers",
        ),
        ExtensionMarketPortal(
            name = "mcp.so",
            description = "Community MCP directory with multilingual browsing.",
            url = "https://mcp.so/",
        ),
        ExtensionMarketPortal(
            name = "Docker MCP Catalog",
            description = "Docker-hosted MCP catalog for containerized server installs.",
            url = "https://hub.docker.com/mcp",
        ),
    )

    val mcpItems = listOf(
        McpMarketItem(
            name = "Context7",
            description = "Fetch current library documentation and examples through a hosted MCP endpoint.",
            source = "Context7",
            config = McpServerConfig.StreamableHTTPServer(
                commonOptions = McpCommonOptions(name = "context7"),
                url = "https://mcp.context7.com/mcp",
            ),
            homepageUrl = "https://context7.com/",
        ),
        McpMarketItem(
            name = "Filesystem",
            description = "Read and write files inside the Termux home directory.",
            source = "Official MCP",
            config = npxServer(
                name = "filesystem",
                packageName = "@modelcontextprotocol/server-filesystem",
                extraArgs = listOf(TERMUX_HOME),
            ),
            homepageUrl = "https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem",
            setupHint = "Requires node and npx in Termux. Edit the path if you want a different sandbox.",
        ),
        McpMarketItem(
            name = "Memory",
            description = "Store and retrieve long-lived graph memory for assistants.",
            source = "Official MCP",
            config = npxServer(
                name = "memory",
                packageName = "@modelcontextprotocol/server-memory",
            ),
            homepageUrl = "https://github.com/modelcontextprotocol/servers/tree/main/src/memory",
            setupHint = "Requires node and npx in Termux.",
        ),
        McpMarketItem(
            name = "Sequential Thinking",
            description = "Break down reasoning into explicit, revisable thinking steps.",
            source = "Official MCP",
            config = npxServer(
                name = "sequential-thinking",
                packageName = "@modelcontextprotocol/server-sequential-thinking",
            ),
            homepageUrl = "https://github.com/modelcontextprotocol/servers/tree/main/src/sequentialthinking",
            setupHint = "Requires node and npx in Termux.",
        ),
        McpMarketItem(
            name = "Everything",
            description = "Reference server useful for testing MCP client capabilities.",
            source = "Official MCP",
            config = npxServer(
                name = "everything",
                packageName = "@modelcontextprotocol/server-everything",
            ),
            homepageUrl = "https://github.com/modelcontextprotocol/servers/tree/main/src/everything",
            setupHint = "Requires node and npx in Termux.",
        ),
        McpMarketItem(
            name = "GitHub",
            description = "Work with repositories, issues, pull requests, and code search.",
            source = "Official MCP",
            config = npxServer(
                name = "github",
                packageName = "@modelcontextprotocol/server-github",
                env = listOf("GITHUB_PERSONAL_ACCESS_TOKEN" to ""),
            ),
            homepageUrl = "https://github.com/modelcontextprotocol/servers/tree/main/src/github",
            setupHint = "Requires node/npx and a GitHub token in the environment field.",
        ),
        McpMarketItem(
            name = "Brave Search",
            description = "Search the web through Brave Search API.",
            source = "Official MCP",
            config = npxServer(
                name = "brave-search",
                packageName = "@modelcontextprotocol/server-brave-search",
                env = listOf("BRAVE_API_KEY" to ""),
            ),
            homepageUrl = "https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search",
            setupHint = "Requires node/npx and a Brave Search API key.",
        ),
        McpMarketItem(
            name = "Firecrawl",
            description = "Crawl websites and extract structured web content.",
            source = "Firecrawl",
            config = npxServer(
                name = "firecrawl",
                packageName = "firecrawl-mcp",
                env = listOf("FIRECRAWL_API_KEY" to ""),
            ),
            homepageUrl = "https://github.com/mendableai/firecrawl-mcp-server",
            setupHint = "Requires node/npx and a Firecrawl API key.",
        ),
    )

    private fun npxServer(
        name: String,
        packageName: String,
        extraArgs: List<String> = emptyList(),
        env: List<Pair<String, String>> = emptyList(),
    ): McpServerConfig.StdioServer {
        return McpServerConfig.StdioServer(
            commonOptions = McpCommonOptions(name = name),
            command = "npx",
            args = listOf("-y", packageName) + extraArgs,
            env = env,
            workdir = TERMUX_HOME,
        )
    }
}
