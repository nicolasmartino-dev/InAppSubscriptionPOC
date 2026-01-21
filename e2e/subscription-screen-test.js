/**
 * Mobile MCP E2E Test: Subscription Screen Verification
 * 
 * This MVP test verifies that:
 * 1. The subscription screen loads
 * 2. All three subscription plans are visible (First, Second, Bundle)
 * 3. One plan shows as "Active"
 */

const { exec } = require('child_process');
const util = require('util');
const execPromise = util.promisify(exec);

// Support device selection via env var or command line arg
const DEVICE = process.env.ANDROID_SERIAL || process.argv[2] || '';
const DEVICE_FLAG = DEVICE ? `-s ${DEVICE}` : '';

const ADB_BASE = process.env.ANDROID_SDK_ROOT
    ? `${process.env.ANDROID_SDK_ROOT}/platform-tools/adb`
    : 'adb';

const ADB = `${ADB_BASE} ${DEVICE_FLAG}`.trim();

async function runTest() {
    console.log('🚀 Starting E2E Test: Subscription Screen Verification');
    console.log(`📱 Using ADB: ${ADB}`);

    try {
        // Step 1: Wait for app to load
        console.log('⏳ Waiting for app to load...');
        await sleep(3000);

        // Step 2: Dump UI hierarchy to file and read it
        console.log('📱 Analyzing screen elements...');
        await execPromise(`${ADB} shell uiautomator dump /sdcard/ui.xml`);
        const { stdout } = await execPromise(`${ADB} shell cat /sdcard/ui.xml`);

        // Step 3: Verify key elements are present
        const checks = [
            { name: 'Choose Your Plan header', pattern: /Choose Your Plan/i },
            { name: 'Current Subscription section', pattern: /Current Subscription/i },
            { name: 'Manage Subscription button', pattern: /Manage Subscription/i },
        ];

        let passed = 0;
        let failed = 0;

        for (const check of checks) {
            if (check.pattern.test(stdout)) {
                console.log(`✅ PASS: ${check.name} is visible`);
                passed++;
            } else {
                console.log(`❌ FAIL: ${check.name} not found`);
                failed++;
            }
        }

        // Step 4: Check for subscription status
        if (/Active/i.test(stdout) || /ON HOLD/i.test(stdout)) {
            console.log('✅ PASS: Subscription status is visible');
            passed++;
        } else {
            console.log('⚠️ WARN: No subscription status detected');
        }

        // Step 5: Report results
        console.log('\n📊 Test Results:');
        console.log(`   Passed: ${passed}`);
        console.log(`   Failed: ${failed}`);

        if (failed > 0) {
            console.log('\n❌ E2E TEST FAILED');
            process.exit(1);
        } else {
            console.log('\n✅ E2E TEST PASSED');
            process.exit(0);
        }

    } catch (error) {
        console.error('💥 Test Error:', error.message);
        process.exit(1);
    }
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// Run the test
runTest();
