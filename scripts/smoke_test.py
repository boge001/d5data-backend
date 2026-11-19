"""Exercise the real HTTP login/session/logout flow using Python's standard library."""

import argparse
import json
from http.cookiejar import CookieJar
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import HTTPCookieProcessor, Request, build_opener


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:8080")
    args = parser.parse_args()
    opener = build_opener(HTTPCookieProcessor(CookieJar()))

    def call(path, expected, data=None, headers=None):
        request = Request(args.url.rstrip("/") + path, data=data, headers=headers or {})
        try:
            with opener.open(request, timeout=10) as response:
                status, body = response.status, response.read().decode()
        except HTTPError as error:
            status, body = error.code, error.read().decode()
        if status != expected:
            raise RuntimeError(
                f"{request.get_method()} {path}: expected {expected}, got {status}: {body}"
            )
        print(
            f"{request.get_method()} {path} -> {status}"
            + (f" {body}" if path == "/api/hello" else "")
        )
        return body

    def csrf():
        payload = json.loads(call("/api/auth/csrf", 200))
        return {
            payload["headerName"]: payload["token"],
            "Content-Type": "application/x-www-form-urlencoded",
        }

    call("/api/hello", 401)
    call(
        "/api/auth/login",
        401,
        urlencode({"username": "test", "password": "wrong"}).encode(),
        csrf(),
    )
    call(
        "/api/auth/login",
        200,
        urlencode({"username": "test", "password": "123456"}).encode(),
        csrf(),
    )
    if call("/api/hello", 200) != "Hello World":
        raise RuntimeError("Unexpected Hello World response")
    call("/api/auth/logout", 204, b"", csrf())
    call("/api/hello", 401)
    print("PASS: authentication persists in the session and is removed on logout.")


if __name__ == "__main__":
    main()
