#!/usr/bin/env python3
"""Publish the built jar to GitHub Releases, Modrinth and CurseForge.

Safe to re-run: every target that already has this version is skipped, and progress is recorded in
.github/autoupdate-state.json. Needs GH_TOKEN, MODRINTH_TOKEN and CURSEFORGE_TOKEN in the environment.
Standard library only.
"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from autoupdate import ROOT, UA, load_state, read_props, save_state  # noqa: E402

MODRINTH_PROJECT = "qMow3vYQ"
MODMENU_MODRINTH = "mOgUt4GM"
CURSEFORGE_PROJECT = "1740759"
MOD_NAME = "Less Villager Chatter"


def request(method, url, headers=None, body=None):
    req = urllib.request.Request(url, data=body, method=method, headers={"User-Agent": UA, **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            data = r.read()
            return r.status, (json.loads(data) if data else None)
    except urllib.error.HTTPError as e:
        raise SystemExit(f"{method} {url} failed: {e.code} {e.read().decode('utf-8', 'replace')}")


def multipart(fields, files):
    """fields: {name: (json_text, content_type)}, files: {name: (filename, bytes, content_type)}"""
    boundary = uuid.uuid4().hex
    parts = []
    for name, (text, ctype) in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n'
                     f'Content-Type: {ctype}\r\n\r\n'.encode() + text.encode() + b"\r\n")
    for name, (filename, data, ctype) in files.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'
                     f'Content-Type: {ctype}\r\n\r\n'.encode() + data + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode())
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def changelog_for(version):
    with open(os.path.join(ROOT, "CHANGELOG.md"), encoding="utf-8") as f:
        text = f.read()
    start = text.index(f"## {version}\n") + len(f"## {version}\n")
    end = text.find("\n## ", start)
    return text[start:end if end != -1 else None].strip()


def publish_github(version, jar, notes, kind):
    tag = f"v{version}"
    if subprocess.run(["gh", "release", "view", tag], capture_output=True).returncode == 0:
        print(f"GitHub: {tag} already exists")
        return
    cmd = ["gh", "release", "create", tag, jar, "--title", f"{MOD_NAME} {version}", "--notes", notes, "--target", "main"]
    if kind == "beta":
        cmd.append("--prerelease")
    subprocess.run(cmd, check=True)
    print(f"GitHub: released {tag}")


def publish_modrinth(version, mc, jar, notes, kind):
    auth = {"Authorization": os.environ["MODRINTH_TOKEN"]}
    _, existing = request("GET", f"https://api.modrinth.com/v2/project/{MODRINTH_PROJECT}/version", auth)
    if any(v["version_number"] == version for v in existing):
        print(f"Modrinth: {version} already exists")
        return
    data = {
        "project_id": MODRINTH_PROJECT, "name": f"{MOD_NAME} {version}", "version_number": version,
        "changelog": notes, "game_versions": [mc], "loaders": ["fabric"], "version_type": kind,
        "featured": True, "environment": "server_only_client_optional",
        "dependencies": [{"project_id": MODMENU_MODRINTH, "dependency_type": "optional"}],
        "file_parts": ["jar"], "primary_file": "jar",
    }
    with open(jar, "rb") as f:
        body, ctype = multipart({"data": (json.dumps(data), "application/json")},
                                {"jar": (os.path.basename(jar), f.read(), "application/java-archive")})
    _, created = request("POST", "https://api.modrinth.com/v2/version", {**auth, "Content-Type": ctype}, body)
    for v in existing:
        if v.get("featured"):
            request("PATCH", f"https://api.modrinth.com/v2/version/{v['id']}",
                    {**auth, "Content-Type": "application/json"}, b'{"featured": false}')
    print(f"Modrinth: created {created['id']}")


def curseforge_game_versions(mc, java):
    auth = {"X-Api-Token": os.environ["CURSEFORGE_TOKEN"]}
    _, types = request("GET", "https://minecraft.curseforge.com/api/game/version-types", auth)
    _, versions = request("GET", "https://minecraft.curseforge.com/api/game/versions", auth)
    slug = {t["id"]: t["slug"] for t in types}
    mc_slug = "minecraft-" + "-".join(mc.split(".")[:2])
    wanted = {(mc, mc_slug), (f"Java {java}", "java"), ("Fabric", "modloader"),
              ("Server", "environment"), ("Client", "environment")}
    ids = [v["id"] for v in versions if (v["name"], slug.get(v["gameVersionTypeID"])) in wanted]
    if not any(slug.get(v["gameVersionTypeID"]) == mc_slug and v["name"] == mc for v in versions):
        raise SystemExit(f"CurseForge does not list Minecraft {mc} yet")
    return ids


def publish_curseforge(version, mc, java, jar, notes, kind):
    meta = {
        "changelog": notes, "changelogType": "markdown", "displayName": f"{MOD_NAME} {version}",
        "gameVersions": curseforge_game_versions(mc, java), "releaseType": kind,
        "relations": {"projects": [{"slug": "modmenu", "type": "optionalDependency"}]},
    }
    with open(jar, "rb") as f:
        body, ctype = multipart({"metadata": (json.dumps(meta), "application/json")},
                                {"file": (os.path.basename(jar), f.read(), "application/java-archive")})
    _, created = request("POST", f"https://minecraft.curseforge.com/api/projects/{CURSEFORGE_PROJECT}/upload-file",
                         {"X-Api-Token": os.environ["CURSEFORGE_TOKEN"], "Content-Type": ctype}, body)
    print(f"CurseForge: uploaded file {created['id']}")


def main():
    props = read_props()
    version, mc, java = props["version"], props["minecraft_version"], props["java_version"]
    jar = os.path.join(ROOT, "build", "libs", f"less-villager-chatter-{version}.jar")
    if not os.path.exists(jar):
        raise SystemExit(f"missing {jar}; build first")
    state = load_state()
    entry = state.setdefault("published", {}).setdefault(version, {})
    kind = entry.get("type", "release")
    notes = changelog_for(version)

    # Each step saves progress right away, so a failure later on does not repeat earlier uploads.
    for target, action in (
        ("github", lambda: publish_github(version, jar, notes, kind)),
        ("modrinth", lambda: publish_modrinth(version, mc, jar, notes, kind)),
        ("curseforge", lambda: publish_curseforge(version, mc, java, jar, notes, kind)),
    ):
        if entry.get(target):
            print(f"{target}: already done")
            continue
        action()
        entry[target] = True
        save_state(state)


if __name__ == "__main__":
    main()
