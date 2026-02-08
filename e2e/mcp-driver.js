
const { Client } = require("@modelcontextprotocol/sdk/client");
const { StdioClientTransport } = require("@modelcontextprotocol/sdk/client/stdio.js");
const { spawn } = require("child_process");
const path = require("path");
const fs = require("fs");

async function main() {
    console.log("Starting Mobile MCP E2E Test Driver...");

    // 1. Start the MCP Server
    const serverPath = path.resolve(__dirname, "node_modules", "mobile-mcp", "dist", "index.js");
    console.log(`Spawning server from: ${serverPath}`);

    const transport = new StdioClientTransport({
        command: "node",
        args: [serverPath],
    });

    const client = new Client({
        name: "e2e-client",
        version: "1.0.0",
    }, {
        capabilities: {}
    });

    try {
        await client.connect(transport);
        console.log("Connected to Mobile MCP Server via Stdio.");

        // 2. Initialize Device
        console.log("Initializing device connection...");
        await client.callTool({
            name: "mobile_init",
            arguments: {}
        });

        // 3. Launch App
        // Note: App installation is handled by the CI pipeline via ADB before this script runs.
        console.log("Launching App...");
        await client.callTool({
            name: "mobile_open_app",
            arguments: {
                packageName: "com.subscription.poc"
            }
        });

        // Wait for launch
        // Wait for launch - increased to 30s for slow CI environments to handle "System UI not responding"
        console.log("Waiting for app launch and system stabilization...");
        await new Promise(resolve => setTimeout(resolve, 30000));

        // 4. Verify UI State (Semantic Check)
        console.log("Checking UI elements...");

        const maxRetries = 5;
        let uiContent = "";
        let found = false;
        const requiredText = ["Subscribe", "First Subscription"]; // Adjust based on expected UI

        for (let i = 0; i < maxRetries; i++) {
            console.log(`Attempt ${i + 1}/${maxRetries} to read UI...`);
            const uiResult = await client.callTool({
                name: "mobile_dump_ui",
                arguments: {}
            });

            uiContent = uiResult.content[0].text;

            // Check for potential ANR or System UI dialog
            if (uiContent.includes("isn't responding") || uiContent.includes("Close app")) {
                console.log("WARNING: Detected ANR dialog. Waiting 5s...");
                await new Promise(resolve => setTimeout(resolve, 5000));
                continue;
            }

            found = requiredText.some(text => uiContent.includes(text));
            if (found) break;

            // SELF-HEALING: If we are not on the app (e.g. Home Screen), try to re-launch it
            if (uiContent.includes("Home") || uiContent.includes("Chrome") || uiContent.includes("Phone")) {
                console.log("Detecting Home screen or other app. Proactively Re-launching target app...");
                await client.callTool({
                    name: "mobile_open_app",
                    arguments: { packageName: "com.subscription.poc" }
                });
                await new Promise(resolve => setTimeout(resolve, 5000));
            } else {
                console.log("Expected text not found yet, retrying in 3s...");
                await new Promise(resolve => setTimeout(resolve, 3000));
            }
        }

        if (found) {
            console.log("SUCCESS: Found subscription elements on screen.");
        } else {
            console.log("WARNING: Did not find expected text. Full dump:");
            console.log(uiContent);
            // We can choose to fail here, but let's take a screenshot first.
        }

        // 5. Take Screenshot
        console.log("Taking validation screenshot...");
        // mobile_screenshot likely returns base64 data since it has no 'saveTo' argument in schema?
        // Wait, schema was: "properties": {} -> No arguments? 
        // If it returns base64 in content, we need to save it.
        const screenshotResult = await client.callTool({
            name: "mobile_screenshot",
            arguments: {}
        });

        // Check if content[0] is image or text (base64)
        if (screenshotResult.content && screenshotResult.content.length > 0) {
            const item = screenshotResult.content[0];
            const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
            const filename = `mcp_screenshot_${timestamp}.png`;
            const filepath = path.resolve(__dirname, filename);

            if (item.type === "image" && item.data) {
                // It's already base64 data
                fs.writeFileSync(filepath, item.data, 'base64');
                console.log(`Screenshot saved to ${filepath}`);
            } else if (item.type === "text") {
                // Maybe it returns base64 string in text?
                // Let's assume standard MCP image return, but fallback to dumping if needed.
                console.log("Received text content from screenshot tool, length:", item.text.length);
                // Try to save as likely base64
                try {
                    fs.writeFileSync(filepath, item.text, 'base64');
                    console.log(`Screenshot saved from text content to ${filepath}`);
                } catch (e) {
                    console.error("Failed to save screenshot", e);
                }
            }
        }

        if (!found) {
            console.error("Test Failed: specific UI elements not found.");
            process.exit(1);
        }

        console.log("Test Passed!");
        process.exit(0);

    } catch (error) {
        console.error("Test Failed:", error);
        process.exit(1);
    }
}

main();
