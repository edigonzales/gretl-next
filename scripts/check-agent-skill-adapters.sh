#!/usr/bin/env bash
set -euo pipefail

failures=0

report() {
    printf 'check-agent-skill-adapters: %s\n' "$*" >&2
    failures=$((failures + 1))
}

frontmatter_value() {
    local file="$1"
    local field="$2"
    awk -v field="$field" '
        NR == 1 && $0 != "---" { exit 2 }
        NR > 1 && $0 == "---" { exit }
        NR > 1 && $0 ~ "^" field ":[[:space:]]*" {
            sub("^" field ":[[:space:]]*", "")
            print
            exit
        }
    ' "$file"
}

canonical_skills=()
while IFS= read -r -d '' file; do
    skill="$(basename "$(dirname "$file")")"
    canonical_skills+=("$skill")
    adapter=".claude/skills/$skill/SKILL.md"

    if [[ ! -f "$adapter" ]]; then
        report "missing Claude adapter for $skill"
        continue
    fi

    canonical_name="$(frontmatter_value "$file" "name" || true)"
    adapter_name="$(frontmatter_value "$adapter" "name" || true)"
    canonical_description="$(frontmatter_value "$file" "description" || true)"
    adapter_description="$(frontmatter_value "$adapter" "description" || true)"

    if [[ "$adapter_name" != "$canonical_name" ]]; then
        report "$adapter name does not match canonical skill $skill"
    fi

    if [[ "$adapter_description" != "$canonical_description" ]]; then
        report "$adapter description does not match canonical skill $skill"
    fi

    if ! grep -F ".agents/skills/$skill/SKILL.md" "$adapter" >/dev/null; then
        report "$adapter does not point to canonical .agents skill"
    fi
done < <(find .agents/skills -mindepth 2 -maxdepth 2 -name SKILL.md -print0 2>/dev/null | sort -z)

if [[ "${#canonical_skills[@]}" -eq 0 ]]; then
    report "no canonical skills found under .agents/skills"
fi

if [[ -d .opencode/commands ]]; then
    while IFS= read -r -d '' command_file; do
        while IFS= read -r skill_ref; do
            [[ -z "$skill_ref" ]] && continue
            if [[ ! -f ".agents/skills/$skill_ref/SKILL.md" ]]; then
                report "$command_file references missing skill $skill_ref"
            fi
        done < <(grep -Eo 'gretl-[a-z0-9-]+' "$command_file" | sort -u)
    done < <(find .opencode/commands -type f -name '*.md' -print0 | sort -z)
fi

if [[ "$failures" -gt 0 ]]; then
    exit 1
fi

printf 'check-agent-skill-adapters: ok\n'
