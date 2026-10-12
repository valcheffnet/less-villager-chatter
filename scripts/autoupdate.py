#!/usr/bin/env python3
"""Automatic Minecraft version updates for Less Villager Chatter (run by .github/workflows/autoupdate.yml).

Subcommands (each prints key=value lines meant for $GITHUB_OUTPUT):
  bump           Move gradle.properties to the newest stable Minecraft release if there is one.
  prop KEY       Print one value from gradle.properties.
  needs          Decide whether to build and whether the Claude fix may run.
  check-fix      Undo any edits the fixer made outside the mod's sources.
  set-type TYPE  Record release/beta for the current version in the state file.
  report-failure Open or update the GitHub issue for a failed update.
  keepalive      Commit a heartbeat when the repo has been quiet for 45 days.

Standard library only, so it keeps working without installing anything.
"""
import datetime as dt
import json
import os
import re
import subprocess
import sys
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROPS = os.path.join(ROOT, "gradle.properties")
STATE = os.path.join(ROOT, ".github", "autoupdate-state.json")
CHANGELOG = os.path.join(ROOT, "CHANGELOG.md")
WRAPPER_PROPS = os.path.join(ROOT, "gradle", "wrapper", "gradle-wrapper.properties")
UA = "valcheffnet/less-villager-chatter autoupdate (github.com/valcheffnet/less-villager-chatter)"
EXAMPLE_MOD = "https://raw.githubusercontent.com/FabricMC/fabric-example-mod/HEAD/"
AI_RETRY_DAYS = 14
QUIET_DAYS = 45


def http_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def http_text(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8")


def http_bytes(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read()


def read_props(text=None):
    if text is None:
        with open(PROPS, encoding="utf-8") as f:
            text = f.read()
    props = {}
    for line in text.splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    return props


def write_props(updates):
    with open(PROPS, encoding="utf-8") as f:
        lines = f.read().splitlines()
    for i, line in enumerate(lines):
        if "=" in line and not line.lstrip().startswith("#"):
            k = line.split("=", 1)[0].strip()
            if k in updates:
                lines[i] = f"{k}={updates[k]}"
    with open(PROPS, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


def load_state():
    try:
        with open(STATE, encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return {"published": {}}


def save_state(state):
    with open(STATE, "w", encoding="utf-8", newline="\n") as f:
        json.dump(state, f, indent=2, sort_keys=True)
        f.write("\n")


def out(**kv):
    for k, v in kv.items():
        print(f"{k}={v}")


def modrinth_versions(project, mc):
    q = urllib.parse.urlencode({"game_versions": json.dumps([mc]), "loaders": json.dumps(["fabric"])})
    return http_json(f"https://api.modrinth.com/v2/project/{project}/version?{q}")


def java_for(mc):
    manifest = http_json("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
    for v in manifest["versions"]:
        if v["id"] == mc:
            return str(http_json(v["url"])["javaVersion"]["majorVersion"])
    raise SystemExit(f"Minecraft {mc} not in Mojang's version manifest")


def bump_patch(version):
    parts = version.split(".")
    parts[-1] = str(int(parts[-1]) + 1)
    return ".".join(parts)


def cmd_bump():
    props = read_props()
    if os.environ.get("SIMULATE_FROM"):  # workflow test: behave as if the mod were still on an older release
        write_props({"minecraft_version": os.environ["SIMULATE_FROM"]})
        props = read_props()
    current = props["minecraft_version"]
    latest = next(v["version"] for v in http_json("https://meta.fabricmc.net/v2/versions/game") if v["stable"])
    if latest == current:
        out(result="current", minecraft=current)
        return

    fabric_api = modrinth_versions("fabric-api", latest)
    if not fabric_api:
        # Fabric API (needed by the game test) is not out yet for this release; try again tomorrow.
        out(result="waiting", minecraft=latest)
        return

    updates = {
        "minecraft_version": latest,
        "loader_version": next(v["version"] for v in http_json("https://meta.fabricmc.net/v2/versions/loader") if v["stable"]),
        "fabric_api_version": fabric_api[0]["version_number"],
        "java_version": java_for(latest),
        "version": bump_patch(props["version"]),
    }
    modmenu = modrinth_versions("modmenu", latest)
    if modmenu:
        updates["modmenu_version"] = modmenu[0]["version_number"]

    # Loom and Gradle: take what Fabric's own example mod uses, but only once it targets this release.
    try:
        example = read_props(http_text(EXAMPLE_MOD + "gradle.properties"))
        if example.get("minecraft_version") == latest and example.get("loom_version"):
            updates["loom_version"] = example["loom_version"]
            wrapper = http_text(EXAMPLE_MOD + "gradle/wrapper/gradle-wrapper.properties")
            with open(WRAPPER_PROPS, "w", encoding="utf-8", newline="\n") as f:
                f.write(wrapper)
            jar = http_bytes(EXAMPLE_MOD + "gradle/wrapper/gradle-wrapper.jar")
            with open(os.path.join(ROOT, "gradle", "wrapper", "gradle-wrapper.jar"), "wb") as f:
                f.write(jar)
    except Exception as e:  # keep the current Loom; the build or the fixer will tell if that's not enough
        print(f"note: example mod lookup failed: {e}", file=sys.stderr)

    write_props(updates)
    with open(CHANGELOG, encoding="utf-8") as f:
        log = f.read()
    entry = f"## {updates['version']}\n\n- Updated to Minecraft {latest}.\n\n"
    log = log.replace("# Changelog\n\n", "# Changelog\n\n" + entry, 1)
    with open(CHANGELOG, "w", encoding="utf-8", newline="\n") as f:
        f.write(log)
    out(result="bumped", minecraft=latest, previous=current, version=updates["version"])


def published_everywhere(state, version):
    p = state.get("published", {}).get(version, {})
    return all(p.get(k) for k in ("github", "modrinth", "curseforge"))


def find_issue(mc):
    title = f"Automatic update to Minecraft {mc} failed"
    r = subprocess.run(["gh", "issue", "list", "--state", "open", "--search", f'"{title}" in:title',
                        "--json", "number,title,comments"], capture_output=True, text=True, check=True)
    for issue in json.loads(r.stdout):
        if issue["title"] == title:
            return issue
    return None


def cmd_needs():
    props = read_props()
    state = load_state()
    build = not published_everywhere(state, props["version"]) or os.environ.get("FORCE_BUILD") == "true"
    ai_allowed = True
    issue = find_issue(props["minecraft_version"]) if build else None
    if issue:
        last = None
        for c in issue.get("comments", []):
            if "Claude tried to fix the build" in c.get("body", ""):
                last = c["createdAt"]
        if last:
            age = dt.datetime.now(dt.timezone.utc) - dt.datetime.fromisoformat(last.replace("Z", "+00:00"))
            ai_allowed = age.days >= AI_RETRY_DAYS
    out(build=str(build).lower(), ai_allowed=str(ai_allowed).lower(), version=props["version"],
        minecraft=props["minecraft_version"])


def cmd_check_fix():
    changed = subprocess.run(["git", "status", "--porcelain"], capture_output=True, text=True, check=True).stdout
    forbidden = [l[3:] for l in changed.splitlines() if l[3:].startswith((".github/", "scripts/"))]
    if forbidden:
        print("reverting edits outside the mod sources: " + ", ".join(forbidden), file=sys.stderr)
        subprocess.run(["git", "checkout", "--", ".github", "scripts"], check=False)
        subprocess.run(["git", "clean", "-fd", "--", ".github", "scripts"], check=False)
    print(subprocess.run(["git", "status", "--short"], capture_output=True, text=True).stdout)


def cmd_set_type(kind):
    props = read_props()
    state = load_state()
    state.setdefault("published", {}).setdefault(props["version"], {})["type"] = kind
    save_state(state)


def cmd_report_failure():
    props = read_props()
    mc = props["minecraft_version"]
    ai_ran = os.environ.get("AI_RAN") == "true"
    log = ""
    for name in ("build-log-2.txt", "build-log.txt"):
        path = os.path.join(ROOT, name)
        if os.path.exists(path):
            with open(path, encoding="utf-8", errors="replace") as f:
                log = "".join(f.readlines()[-60:])
            break
    run_url = f"{os.environ.get('GITHUB_SERVER_URL')}/{os.environ.get('GITHUB_REPOSITORY')}/actions/runs/{os.environ.get('GITHUB_RUN_ID')}"
    body = (f"Minecraft {mc} is out, but the mod did not build or pass its game test after the version bump.\n\n"
            + ("Claude tried to fix the build and the test still fails.\n\n" if ai_ran else "")
            + f"Run: {run_url}\n\n```\n{log}\n```\n")
    issue = find_issue(mc)
    if issue is None:
        subprocess.run(["gh", "issue", "create", "--title", f"Automatic update to Minecraft {mc} failed", "--body", body],
                       check=True)
    elif ai_ran:
        subprocess.run(["gh", "issue", "comment", str(issue["number"]), "--body", body], check=True)


def cmd_keepalive():
    last = subprocess.run(["git", "log", "-1", "--format=%ct"], capture_output=True, text=True, check=True).stdout.strip()
    age = dt.datetime.now(dt.timezone.utc) - dt.datetime.fromtimestamp(int(last), dt.timezone.utc)
    if age.days < QUIET_DAYS:
        out(heartbeat="false")
        return
    state = load_state()
    state["last_heartbeat"] = dt.date.today().isoformat()
    save_state(state)
    out(heartbeat="true")


def main():
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    cmd = sys.argv[1]
    if cmd == "bump":
        cmd_bump()
    elif cmd == "prop":
        print(read_props()[sys.argv[2]])
    elif cmd == "needs":
        cmd_needs()
    elif cmd == "check-fix":
        cmd_check_fix()
    elif cmd == "set-type":
        cmd_set_type(sys.argv[2])
    elif cmd == "report-failure":
        cmd_report_failure()
    elif cmd == "keepalive":
        cmd_keepalive()
    else:
        raise SystemExit(f"unknown command {cmd}")


if __name__ == "__main__":
    main()
