#!/usr/bin/env bash
set -euo pipefail

failures=0

report() {
    printf 'check-agent-skills: %s\n' "$*" >&2
    failures=$((failures + 1))
}

has_frontmatter_field() {
    local file="$1"
    local field="$2"
    awk -v field="$field" '
        NR == 1 && $0 != "---" { exit 2 }
        NR > 1 && $0 == "---" { exit 0 }
        NR > 1 && $0 ~ "^" field ":[[:space:]]*.+" { found = 1 }
        END { exit found ? 0 : 1 }
    ' "$file"
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

while IFS= read -r -d '' file; do
    parent="$(basename "$(dirname "$file")")"

    if [[ "$(head -n 1 "$file")" != "---" ]]; then
        report "$file missing opening frontmatter delimiter"
        continue
    fi

    if ! awk 'NR > 1 && $0 == "---" { found = 1; exit } END { exit found ? 0 : 1 }' "$file"; then
        report "$file missing closing frontmatter delimiter"
    fi

    if ! has_frontmatter_field "$file" "name"; then
        report "$file missing frontmatter name"
    fi

    if ! has_frontmatter_field "$file" "description"; then
        report "$file missing frontmatter description"
    fi

    name="$(frontmatter_value "$file" "name" || true)"
    if [[ -n "$name" && "$name" != "$parent" ]]; then
        report "$file frontmatter name '$name' does not match folder '$parent'"
    fi

    if grep -nE '\b(TODO|FIXME|XXX|PLACEHOLDER)\b' "$file" >/dev/null; then
        report "$file contains placeholder text"
    fi

    if grep -nEi '(password|token|secret|api[_-]?key)[[:space:]]*[:=][[:space:]]*["'\'']?[[:alnum:]_./+=-]{8,}' "$file" >/dev/null; then
        report "$file appears to contain a literal credential"
    fi
done < <(find .agents/skills .claude/skills -name SKILL.md -print0 2>/dev/null | sort -z)

if [[ "$failures" -gt 0 ]]; then
    exit 1
fi

printf 'check-agent-skills: ok\n'
