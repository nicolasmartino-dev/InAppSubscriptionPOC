
const { Client } = require("@modelcontextprotocol/sdk/client");
const { StdioClientTransport } = require("@modelcontextprotocol/sdk/client/stdio.js");
const { generateText } = require("ai");
const { google } = require("@ai-sdk/google");
const path = require("path");
const fs = require("fs");
require("dotenv").config();

async function main() {
    const startTime = Date.now();
    console.log("🚀 Starting Generative Mobile MCP E2E Test Driver...");

    if (!process.env.GOOGLE_GENERATIVE_AI_API_KEY) {
        console.error("❌ Error: GOOGLE_GENERATIVE_AI_API_KEY is not set in .env file.");
        process.exit(1);
    }

    // 1. Start the MCP Server
    const serverPath = path.resolve(__dirname, "node_modules", "mobile-mcp", "dist", "index.js");
    const transport = new StdioClientTransport({
        command: "node",
        args: [serverPath],
    });

    const client = new Client({
        name: "generative-e2e-client",
        version: "1.0.0",
    }, {
        capabilities: {}
    });

    try {
        await client.connect(transport);
        console.log("🔗 Connected to Mobile MCP Server.");

        await client.callTool({ name: "mobile_init", arguments: {} });
        console.log("📱 Device initialized.");

        console.log("🚀 Launching App...");
        await client.callTool({
            name: "mobile_open_app",
            arguments: { packageName: "com.subscription.poc" }
        });

        // Wait for stability
        await new Promise(resolve => setTimeout(resolve, 15000));

        console.log("🔍 Dumping UI for Generative Analysis...");
        const uiResult = await client.callTool({
            name: "mobile_dump_ui",
            arguments: {}
        });

        const rawUiContent = uiResult.content[0].text;

        // Simplify XML even more aggressively
        // Handles single or double quotes, and matches any whitespace/newlines
        const uiContent = rawUiContent
            .replace(/\s+bounds=['"][^'"]*['"]/g, "")
            .replace(/\s+package=['"][^'"]*['"]/g, "")
            .replace(/\s+index=['"][^'"]*['"]/g, "")
            .replace(/\s+class=['"][^'"]*['"]/g, "")
            .replace(/\s+(focusable|clickable|enabled|focused|scrollable|long-clickable|password|selected|checkable|checked)=['"][^'"]*['"]/g, "")
            .replace(/\s+naf=['"][^'"]*['"]/g, "")
            .replace(/\s+/g, " ")
            .trim();

        console.log(`🤖 UI Dump simplified: ${rawUiContent.length} chars -> ${uiContent.length} chars`);
        if (uiContent.length === rawUiContent.length && rawUiContent.length > 200) {
            console.warn("⚠️ Warning: XML simplification did not reduce character count.");
            console.log("🔍 Sample of raw UI content (First 300 chars):", rawUiContent.substring(0, 300));
        }

        console.log("🤖 Asking LLM (gemini-flash-latest) to verify screen content...");
        const llmStartTime = Date.now();

        const { text } = await generateText({
            model: google('gemini-flash-latest'),
            prompt: `
            Analyze the following simplified Android UI dump from a subscription screen.
            Verify if:
            1. The screen title contains "Choose Your Plan".
            2. There are at least 3 subscription plans mentioned.
            3. One of the plans is "First Subscription" (Mock).
            
            Respond ONLY with a JSON object:
            {
              "passed": boolean,
              "reason": "summary of findings"
            }
            
            UI DUMP:
            ${uiContent}
            `
        });

        const llmEndTime = Date.now();
        const llmDuration = (llmEndTime - llmStartTime) / 1000;
        console.log(`🤖 LLM Response received in ${llmDuration}s.`);

        let result;
        try {
            result = JSON.parse(text.replace(/```json|```/g, "").trim());
        } catch (e) {
            console.error("❌ Failed to parse LLM Response:", text);
            result = { passed: false, reason: "Invalid LLM output format: " + text.substring(0, 100) };
        }

        if (result.passed) {
            console.log("✅ SUCCESS:", result.reason);
        } else {
            console.error("❌ FAILURE:", result.reason);
        }

        const endTime = Date.now();
        const totalDuration = (endTime - startTime) / 1000;
        console.log(`\n📊 Generative Test Completed in ${totalDuration}s.`);

        process.exit(result.passed ? 0 : 1);

    } catch (error) {
        console.error("💥 Test Failed with Error:", error);
        process.exit(1);
    }
}

main();
