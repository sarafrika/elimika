# Sourced by scripts/local/*.sh: exports docker/.env.local (created from docker/.env.local.example on first use).
# Values already present in the environment win over the file.
_elimika_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
_elimika_env="$_elimika_root/docker/.env.local"
if [[ ! -f "$_elimika_env" && -f "$_elimika_root/docker/.env.local.example" ]]; then
    cp "$_elimika_root/docker/.env.local.example" "$_elimika_env"
fi
if [[ -f "$_elimika_env" ]]; then
    while IFS= read -r _line || [[ -n "$_line" ]]; do
        [[ "$_line" =~ ^[[:space:]]*(#|$) ]] && continue
        _key="${_line%%=*}"
        [[ -n "${!_key+x}" ]] || export "$_line"
    done <"$_elimika_env"
fi
unset _elimika_root _elimika_env _line _key
