"""Shared helpers for the local seed and smoke scripts: tokens, API calls, SQL through the postgres container.

Local stack only (docker/compose.local.yaml); every value here is a throwaway local-development value.
"""
import json
import os
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

API_URL = os.environ.get("API_URL", "http://localhost:38080")
KEYCLOAK_URL = os.environ.get("KEYCLOAK_URL", "http://localhost:58080")
REALM = os.environ.get("KEYCLOAK_REALM", "elimika-local")
UI_CLIENT_ID = os.environ.get("KEYCLOAK_UI_CLIENT_ID", "elimika-ui")
UI_CLIENT_SECRET = os.environ.get("KEYCLOAK_UI_CLIENT_SECRET", "elimika-local-ui-secret")
PASSWORD = os.environ.get("QA_PASSWORD", "Passw0rd!")
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
COMPOSE = ["docker", "compose", "-f", os.path.join(ROOT, "docker", "compose.local.yaml")]

_tokens = {}


def email(user):
    return user if "@" in user else f"{user}@elimika.local"


def token(user, fresh=False):
    if user is None:
        return None
    key = email(user)
    cached = _tokens.get(key)
    if cached and not fresh and cached[1] > time.time() + 60:
        return cached[0]
    data = urllib.parse.urlencode({
        "grant_type": "password", "client_id": UI_CLIENT_ID, "client_secret": UI_CLIENT_SECRET,
        "username": key, "password": PASSWORD, "scope": "openid"}).encode()
    req = urllib.request.Request(f"{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token", data=data)
    with urllib.request.urlopen(req, timeout=30) as resp:
        body = json.loads(resp.read())
    _tokens[key] = (body["access_token"], time.time() + int(body.get("expires_in", 300)))
    return body["access_token"]


class Response:
    def __init__(self, status, text):
        self.status = status
        self.text = text
        try:
            self.json = json.loads(text) if text else None
        except ValueError:
            self.json = None

    @property
    def data(self):
        if isinstance(self.json, dict) and "data" in self.json:
            return self.json["data"]
        return self.json

    def __repr__(self):
        return f"<{self.status} {self.text[:300]}>"


def call(method, path, user=None, body=None, params=None, headers=None, timeout=60):
    url = API_URL + path
    if params:
        url += ("&" if "?" in url else "?") + urllib.parse.urlencode(params, doseq=True)
    data = None
    hdrs = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode()
        hdrs["Content-Type"] = "application/json"
    if user:
        hdrs["Authorization"] = f"Bearer {token(user)}"
    if headers:
        hdrs.update(headers)
    req = urllib.request.Request(url, data=data, method=method, headers=hdrs)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return Response(resp.status, resp.read().decode())
    except urllib.error.HTTPError as err:
        return Response(err.code, err.read().decode(errors="replace"))


def must(resp, what, ok=(200, 201, 202, 204)):
    if resp.status not in ok:
        raise SystemExit(f"FAILED {what}: {resp.status} {resp.text[:800]}")
    return resp.data


def sql(statement):
    """Runs SQL as the app's DB user; returns rows as lists of strings (tab separated, unaligned)."""
    out = subprocess.run(COMPOSE + ["exec", "-T", "postgres", "psql", "-U", "elimika", "-d", "elimika",
                                    "-v", "ON_ERROR_STOP=1", "-qAt", "-F", "\t", "-c", statement],
                         capture_output=True, text=True)
    if out.returncode != 0:
        raise SystemExit(f"SQL failed: {out.stderr.strip()}\n{statement[:500]}")
    return [line.split("\t") for line in out.stdout.splitlines() if line.strip()]


def sql_value(statement):
    rows = sql(statement)
    return rows[0][0] if rows and rows[0] and rows[0][0] != "" else None


def q(value):
    """Quotes a value as a SQL literal."""
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"
