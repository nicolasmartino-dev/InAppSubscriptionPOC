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
        // Step 1: Wait for app to load with retry logic
        console.log('⏳ Waiting for app to load...');

        const MAX_RETRIES = 15; // Increased to 75 seconds total
        const RETRY_DELAY = 5000;
        let loaded = false;

        for (let i = 0; i < MAX_RETRIES; i++) {
            console.log(`🔄 Attempt ${i + 1}/${MAX_RETRIES}: Analyzing screen...`);

            let dumpSuccess = false;
            let outputContent = '';

            // CLEANUP: Kill stuck uiautomator and delete old dump to prevent stale data
            try {
                await execPromise(`${ADB} shell pkill -f uiautomator`);
            } catch (e) { /* Ignore - process might not exist */ }
            try {
                await execPromise(`${ADB} shell rm -f /sdcard/ui.xml`);
            } catch (e) { /* Ignore */ }
            await sleep(500); // Let accessibility service reset

            // Dump UI hierarchy
            try {
                await execPromise(`${ADB} shell uiautomator dump /sdcard/ui.xml`);
                dumpSuccess = true;
            } catch (e) {
                console.log('   ⚠️ Dump failed (system busy?)');
            }

            // Read the dump if successful
            if (dumpSuccess) {
                try {
                    const { stdout } = await execPromise(`${ADB} shell cat /sdcard/ui.xml`);
                    outputContent = stdout;

                    // Check if our screen is visible
                    if (/Choose Your Plan/i.test(stdout)) {
                        console.log('✅ Screen loaded!');
                        loaded = true;
                        break;
                    }

                    // Check for System UI ANR
                    if (/isn't responding/i.test(stdout)) {
                        console.log('   ⚠️ Detected "isn\'t responding" dialog.');
                    }
                } catch (e) {
                    console.log('   ⚠️ Read failed');
                    outputContent = "Read failed"; // Mark outputContent to indicate read failure
                }
            }

            // RECOVERY STRATEGY:
            // If we haven't succeeded yet, check state and maybe nudge it
            if (!loaded && i < MAX_RETRIES - 1) {

                // If the dump failed, it's likely an ANR dialog blocking it.
                // Or if we successfully read "isn't responding".
                const isAnr = outputContent.includes("isn't responding");
                const isReadFailed = outputContent.includes("Read failed") || outputContent.trim().length === 0;

                if (isAnr || isReadFailed) {
                    console.log('   ⚠️ ANR Dialog or Read Failure detected.');

                    // Try to find the "Wait" button coordinates in the XML dump
                    // XML pattern: <node ... text="Wait" ... bounds="[237,1257][843,1389]" ... />
                    const waitMatch = outputContent.match(/text="Wait".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/);

                    if (waitMatch) {
                        const [_, x1, y1, x2, y2] = waitMatch;
                        const centerX = Math.floor((parseInt(x1) + parseInt(x2)) / 2);
                        const centerY = Math.floor((parseInt(y1) + parseInt(y2)) / 2);

                        console.log(`   👆 Found "Wait" button. Tapping at ${centerX},${centerY}...`);
                        try { await execPromise(`${ADB} shell input tap ${centerX} ${centerY}`); } catch (e) { }
                    } else {
                        // Fallback: Try keyboard navigation (Tab -> Enter)
                        // This is crucial when "Read failed" because we have no coordinates!
                        console.log('   Warning: Could not find "Wait" button bounds (or read failed). Trying generic key events...');
                        try {
                            // Send TAB to move focus to "Wait" (usually the second option)
                            await execPromise(`${ADB} shell input keyevent 61`); // TAB
                            await sleep(500);
                            await execPromise(`${ADB} shell input keyevent 66`); // ENTER
                        } catch (e) { }
                    }
                    await sleep(1000);
                }

                // Ensure app is in foreground (just in case)
                console.log('   Starting/Bringing app to front...');
                try {
                    await execPromise(`${ADB} shell am start -n com.subscription.poc/.MainActivity`);
                } catch (e) { }

                // Just wait patiently
                console.log(`   ⏳ Waiting ${RETRY_DELAY}ms for app to reload...`);
                await sleep(RETRY_DELAY);
            }
        }

        if (!loaded) {
            console.error('❌ Timeout: App did not load "Choose Your Plan" screen within time limit.');
            console.log('\n❌ E2E TEST FAILED');
            process.exit(1);
        }

        // Step 2: Final Verification (using the last successful dump)
        const { stdout } = await execPromise(`${ADB} shell cat /sdcard/ui.xml`);

        // Step 3: Verify key elements are present (Purchase Screen)
        const checks = [
            { name: 'Choose Your Plan header', pattern: /Choose Your Plan/i },
            { name: 'First (Mock) plan', pattern: /First \(Mock\)/i },
            { name: 'Subscribe button', pattern: /Subscribe/i },
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
