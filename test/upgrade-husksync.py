#!/usr/bin/env python3
# This script automates most changes required to target and support a specific Minecraft version

"""
Usage: python3 upgrade-husksync.py <version>   (e.g. upgrade-husksync.py 26.2)

Creates/updates files for a new Minecraft version target:
  - bukkit/<version>/gradle.properties
  - common/.../DataVersionSupplier.java
  - .github/workflows/ci.yml
  - .github/workflows/release.yml
"""

import json
import os
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

MCMETA_URL = "https://raw.githubusercontent.com/misode/mcmeta/summary/versions/data.json"
PAPER_METADATA_URL = "https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml"
MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
JAVA_VERSION = "25"

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def fetch_json(url):
    with urllib.request.urlopen(url) as r:
        return json.loads(r.read().decode())


def fetch_text(url):
    with urllib.request.urlopen(url) as r:
        return r.read().decode()


def parse_version(version_str):
    m = re.match(r"^(\d+)\.(\d+)(?:\.(\d+))?$", version_str)
    if not m:
        print(f"Error: '{version_str}' is not a valid Minecraft version (e.g. 26.1, 26.1.2)")
        sys.exit(1)
    major = int(m.group(1))
    minor = int(m.group(2))
    patch = int(m.group(3)) if m.group(3) else 0
    return major, minor, patch


def get_data_version(version_str):
    data = fetch_json(MCMETA_URL)
    for entry in data:
        if entry.get("id") == version_str and entry.get("type") == "release":
            return entry.get("data_version")
    for entry in data:
        if entry.get("id") == version_str:
            return entry.get("data_version")
    print(f"Error: version '{version_str}' not found in mcmeta versions data")
    print(f"  Check available versions at {MCMETA_URL}")
    sys.exit(1)


def get_paper_api_version(version_str):
    xml_text = fetch_text(PAPER_METADATA_URL)
    root = ET.fromstring(xml_text)
    all_versions = root.findall(".//version")

    candidates = []
    for v in all_versions:
        ver = v.text
        if ver and ver.startswith(f"{version_str}."):
            m = re.search(r"\.build\.(\d+)", ver)
            if m:
                build_num = int(m.group(1))
                channel = "alpha"
                if "-stable" in ver.lower():
                    channel = "stable"
                elif "-beta" in ver.lower():
                    channel = "beta"
                candidates.append((build_num, channel, ver))

    if not candidates:
        print(f"Warning: no Paper API builds found for version {version_str}")
        print(f"  Check {PAPER_METADATA_URL}")
        print(f"  Falling back to '{version_str}.build.1-alpha'")
        return f"{version_str}.build.1-alpha"

    stable = [c for c in candidates if c[1] == "stable"]
    beta = [c for c in candidates if c[1] == "beta"]
    alpha = [c for c in candidates if c[1] == "alpha"]

    if stable:
        best = max(stable, key=lambda c: c[0])
        print(f"  Paper API: STABLE build {best[2]}")
        return best[2]
    if beta:
        best = max(beta, key=lambda c: c[0])
        print(f"  Paper API: no STABLE, using BETA build {best[2]}")
        return best[2]
    best = max(alpha, key=lambda c: c[0])
    print(f"  Paper API: no STABLE/BETA, using ALPHA build {best[2]}")
    return best[2]


def constant_name(major, minor):
    return f"VERSION{major}_{minor}"


def create_gradle_properties(version_str, api_version, numeric, version_range, paper_api):
    dir_path = os.path.join(REPO_ROOT, "bukkit", version_str)
    os.makedirs(dir_path, exist_ok=True)
    path = os.path.join(dir_path, "gradle.properties")
    content = (
        f"minecraft_version_range={version_range}\n"
        f"minecraft_version_numeric={numeric}\n"
        f"minecraft_api_version={api_version}\n"
        f"paper_api_version={paper_api}\n"
        f"java_version={JAVA_VERSION}\n"
    )
    with open(path, "w") as f:
        f.write(content)
    print(f"  Created {path}")


def edit_data_version_supplier(major, minor, data_version):
    path = os.path.join(
        REPO_ROOT, "common/src/main/java/net/william278/husksync/util",
        "DataVersionSupplier.java"
    )
    with open(path) as f:
        content = f.read()

    const_name = constant_name(major, minor)
    version_label = f"{major}.{minor}"

    # 1. Add constant if not present
    new_const = f"\tint {const_name} = {data_version};"
    if new_const in content:
        print(f"  Constant {const_name} already exists, skipping")
    else:
        const_pattern = r"(int VERSION\d+(?:_\d+)+ = \d+;)"
        matches = list(re.finditer(const_pattern, content))
        if not matches:
            print("Error: could not find existing VERSION constants")
            sys.exit(1)
        insert_pos = matches[-1].end()
        content = content[:insert_pos] + "\n" + new_const + content[insert_pos:]
        print(f"  Added constant {const_name} = {data_version}")

    # 2. Add switch case before default
    new_case = f"case \"{version_label}\" -> {const_name};"
    if new_case in content:
        print(f"  Switch case for '{version_label}' already exists, skipping")
    else:
        default_match = re.search(r"(\t*default -> .*; // Latest supported version)", content)
        if not default_match:
            print("Error: could not find default case in switch")
            sys.exit(1)
        insert_pos = default_match.start()
        content = content[:insert_pos] + f"\t\t\t{new_case}\n" + content[insert_pos:]
        print(f"  Added switch case for '{version_label}'")

    # 3. Update default to point to new constant
    default_pattern = r"(default -> )(\w+)(; // Latest supported version)"
    default_line = re.search(default_pattern, content)
    if default_line:
        content = content.replace(
            default_line.group(0),
            f"{default_line.group(1)}{const_name}{default_line.group(3)}"
        )
        print(f"  Updated default to {const_name}")

    with open(path, "w") as f:
        f.write(content)
    print(f"  Updated {path}")


def get_block_items(content, block_key):
    """Return list of items (with leading whitespace) in a YAML list block."""
    pattern = rf"^\s*{re.escape(block_key)}: \|\s*$"
    match = re.search(pattern, content, re.MULTILINE)
    if not match:
        return []
    block_start = match.end()
    lines = content[block_start:].split("\n")
    items = []
    block_indent = None
    for line in lines:
        stripped = line.rstrip()
        if not stripped:
            continue
        line_indent = len(line) - len(line.lstrip())
        if block_indent is None:
            block_indent = line_indent
        if line_indent >= block_indent:
            items.append(line.rstrip())
        else:
            break
    return items


def update_workflow(workflow_file, version_str):
    """Append a new distro entry to a workflow file's publish step."""
    path = os.path.join(REPO_ROOT, ".github/workflows", workflow_file)
    with open(path) as f:
        content = f.read()

    version_tag = f"paper-{version_str}"
    if version_tag in content:
        print(f"  Distro {version_tag} already in {workflow_file}, skipping")
        return

    file_var = "env.version_name" if workflow_file == "ci.yml" else "github.event.release.tag_name"

    # The 4 blocks are parallel arrays — ensure they all stay in sync
    blocks = [
        ("distro-names", version_tag),
        ("distro-groups", "paper"),
        ("distro-descriptions", f"Paper {version_str}"),
        ("files", f"target/HuskSync-Bukkit-${{{{ {file_var} }}}}+mc.{version_str}.jar"),
    ]

    # Count entries in distro-names as the reference
    ref_count = len(get_block_items(content, "distro-names"))

    for block_key, new_line in blocks:
        items = get_block_items(content, block_key)
        if len(items) >= ref_count + 1:
            print(f"    {block_key}: already has {len(items)} entries, skipping")
            continue
        if not items:
            continue

        # Determine the indent from the first item
        item_indent = len(items[0]) - len(items[0].lstrip())
        new_entry = " " * item_indent + new_line

        # Insert after the last item in the block
        block_pattern = rf"^\s*{re.escape(block_key)}: \|\s*$"
        block_match = re.search(block_pattern, content, re.MULTILINE)
        block_start = block_match.end()

        insert_after = block_start
        for _ in range(len(items)):
            insert_after = content.index("\n", insert_after) + 1
        content = content[:insert_after] + new_entry + "\n" + content[insert_after:]
        print(f"    {block_key}: added '{new_line}'")

    content = content.rstrip("\n") + "\n"

    with open(path, "w") as f:
        f.write(content)
    print(f"  Updated {workflow_file}: added {version_tag}")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        try:
            manifest = fetch_json(MANIFEST_URL)
            print(f"\nLatest Minecraft release: {manifest['latest']['release']}")
        except Exception:
            pass
        sys.exit(1)

    version_str = sys.argv[1]
    print(f"== Upgrading HuskSync to Minecraft {version_str} ==\n")

    major, minor, patch = parse_version(version_str)
    api_version = f"{major}.{minor}"
    version_range = f">={version_str} <={version_str}"
    numeric = major * 10000 + minor * 100 + patch

    print(f"  Version:        {version_str}")
    print(f"  API version:    {api_version}")
    print(f"  Numeric:        {numeric}")
    print(f"  Version range:  {version_range}")

    print("\n--- Fetching data version ---")
    data_version = get_data_version(version_str)
    print(f"  Data version:   {data_version}")

    print("\n--- Fetching Paper API version ---")
    paper_api = get_paper_api_version(version_str)

    print("\n--- Bukkit gradle.properties ---")
    create_gradle_properties(version_str, api_version, numeric, version_range, paper_api)

    print("\n--- DataVersionSupplier.java ---")
    edit_data_version_supplier(major, minor, data_version)

    print("\n--- CI workflow ---")
    update_workflow("ci.yml", version_str)

    print("\n--- Release workflow ---")
    update_workflow("release.yml", version_str)

    print(f"\n== Upgrade to {version_str} complete ==")
    print("Review changes with: git diff")
    print('Commit: git add -A && git commit -m "feat: add Paper {} support"'.format(version_str))


if __name__ == "__main__":
    main()
