import subprocess
import sys
import datetime
import os

ADB_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "tools", "platform-tools", "adb.exe"))
DEVICE_IP = "10.0.0.51:5555"
DISABLED_LOG_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "disabled_packages.txt"))

STRICT_EXCLUDE = {
    "com.shadeed.neo4kpro",  # CRITICAL: Neo 4K Pro (Parents login)
    "com.amazon.tv.launcher", # Stock launcher
    "com.amazon.tv.settings.v2",
    "com.amazon.tv.settings.core",
    "com.amazon.tv.quicksettings.ui",
    "com.amazon.device.bluetoothkeymaplib",
    "com.amazon.device.bluetoothpa",
    "com.amazon.device.blepa",
    "com.amazon.fireinputdevices",
    "com.amazon.autopairservice",
    "com.android.bluetooth",
    "com.amazon.connectivitycontroller",
    "com.amazon.connectivitydiag",
    "com.amazon.wifilocker",
    "com.android.networkstack",
    "com.android.wifi.resources",
    "com.amazon.venezia",
    "com.android.packageinstaller",
    "com.amazon.android.marketplace",
    "com.amazon.webview.chromium",
    "com.android.systemui",
    "com.amazon.tv.ime"
}

# Group 1: Safe to disable
GROUP_1 = [
    # Telemetry, Diagnostics & Crash Reporting
    "com.amazon.device.crashmanager",
    "com.amazon.device.metrics",
    "com.amazon.device.telemetry.emitter",
    "com.amazon.client.metrics.api",
    "com.amazon.wirelessmetrics.service",
    "com.amazon.dp.logger",
    "com.amazon.logan",
    "com.amazon.ssm",
    "com.amazon.ssmsys",
    "com.amazon.perfc",
    "com.amazon.perfcollection",
    "com.amazon.neo.minerva",
    "com.amazon.neo.minerva.logcat",
    "com.fireos.arcus.proxy",
    "com.fireos.usagestats.proxy",
    "com.android.traceur",

    # Ads, Recommendations & Commercial Bloat
    "com.amazon.hybridadidservice",
    "com.amazon.media.recommendations",
    "com.amazon.ftvads.deeplinking",
    "com.amazon.shoptv.client",
    "com.amazon.shoptv.firetv.client",
    "com.amazon.gamehub",
    "com.amazon.minitv.android.app",
    "com.amazon.bueller.photos",
    "com.amazon.bueller.music",

    # Tutorials, Dummies & Setup Prompts
    "com.amazon.tmm.tutorial",
    "com.amazon.storm.lightning.tutorial",
    "com.amazon.dummy.alarmclock",
    "com.amazon.dummy.calendar",
    "com.amazon.dummy.contacts",
    "com.amazon.dummy.gallery",
    "com.amazon.dummy.music",
    "com.amazon.dummy.settings",
    "com.amazon.dummy.soundpicker",
    "com.android.protips",
    "com.amazon.tv.releasenotes",
    "com.amazon.tv.earlyaccess",
    "com.amazon.tv.easyupgrade"
]

# Group 2: Optional / Unsure approved by user
GROUP_2 = [
    # Alexa & Voice Ecosystem
    "com.amazon.aca",
    "com.amazon.alexa.datastore.app",
    "com.amazon.alexa.externalmediaplayer.fireos",
    "com.amazon.alexadirectivebrokerservice",
    "com.amazon.alexamediaplayer.runtime.ftv",
    "com.amazon.tv.alexaalerts",
    "com.amazon.tv.alexanotifications",
    "com.amazon.aria",
    "com.amazon.vizzini",

    # Screen Saver & Ambient Display
    "com.amazon.ftv.screensaver",
    "com.amazon.tv.ftvambient",

    # Casting & Secondary Screen
    "com.amazon.whisperlink.core.android",
    "com.amazon.whisperplay.contracts",
    "com.amazon.whisperplay.service.install",
    "com.amazon.awvflingreceiver",
    "com.amazon.cast.sink",
    "com.amazon.dialservice",

    # Updates (OTA)
    "com.amazon.device.software.ota",
    "com.amazon.device.software.ota.override",
    "com.amazon.tv.forcedotaupdater.v2",

    # UX Notifications & Smart Home
    "com.amazon.uxnotification",
    "com.amazon.tv.notificationcenter",
    "com.amazon.smarthomemapviewapp",
    "com.amazon.tv.matter",
    "com.amazon.tv.mattercohost",

    # Content & Live TV Guides
    "com.amazon.tv.livetv",
    "com.amazon.tifobserver"
]

def run_adb(args):
    cmd = [ADB_PATH, "-s", DEVICE_IP] + args
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.stdout.strip(), res.stderr.strip(), res.returncode

def ensure_connected():
    subprocess.run([ADB_PATH, "connect", DEVICE_IP], capture_output=True, text=True)

def log_disabled(pkg, cmd_run):
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    entry = f"[{now}] ACTION: {cmd_run}\nUNDO: pm enable {pkg}\n\n"
    with open(DISABLED_LOG_PATH, "a", encoding="utf-8") as f:
        f.write(entry)

def main():
    ensure_connected()
    
    # Get currently installed packages
    stdout, stderr, code = run_adb(["shell", "pm", "list", "packages"])
    installed_pkgs = set()
    for line in stdout.splitlines():
        if line.startswith("package:"):
            installed_pkgs.add(line.replace("package:", "").strip())
            
    print(f"Total installed packages on device: {len(installed_pkgs)}")

    # Target packages: Group 1 followed by Group 2
    targets = [(pkg, "Group 1 (Safe to disable)") for pkg in GROUP_1] + \
              [(pkg, "Group 2 (Optional/Unsure approved)") for pkg in GROUP_2]

    disabled_success = []
    disabled_failed = []
    skipped = []

    print("\n--- Starting Debloat (pm disable-user --user 0) ---")
    for pkg, group_name in targets:
        if pkg in STRICT_EXCLUDE:
            print(f"[SKIP - PROTECTED] {pkg}")
            skipped.append((pkg, "Protected system/user package"))
            continue

        if pkg not in installed_pkgs:
            # Package not installed
            skipped.append((pkg, "Not installed on this device"))
            continue

        cmd_run = f"pm disable-user --user 0 {pkg}"
        out, err, ret = run_adb(["shell", "pm", "disable-user", "--user", "0", pkg])
        
        result_str = (out + " " + err).strip()
        if "disabled-user" in result_str or "new state: disabled" in result_str:
            print(f"[SUCCESS] {pkg} ({group_name}) -> {result_str}")
            disabled_success.append(pkg)
            log_disabled(pkg, cmd_run)
        else:
            print(f"[FAILED/BLOCKED] {pkg} ({group_name}) -> {result_str}")
            disabled_failed.append((pkg, result_str))

    print("\n================== DEBLOAT SUMMARY ==================")
    print(f"Successfully disabled: {len(disabled_success)}")
    print(f"Failed / Blocked by OS: {len(disabled_failed)}")
    print(f"Skipped (not present or protected): {len(skipped)}")
    
    if disabled_failed:
        print("\nBlocked Packages:")
        for p, reason in disabled_failed:
            print(f" - {p}: {reason}")

if __name__ == "__main__":
    main()

