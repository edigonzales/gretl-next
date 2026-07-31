from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.respond(200, b"healthy")
        elif self.path == "/download":
            self.respond(200, b"download-content")
        else:
            self.respond(404, b"missing")

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        self.rfile.read(length)
        if self.path == "/text":
            self.respond(201, b"accepted")
        elif self.path == "/binary":
            self.respond(200, b"ok")
        elif self.path == "/form":
            self.respond(200, b"form-ok")
        else:
            self.respond(404, b"missing")

    def respond(self, status, body):
        self.send_response(status)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        pass


HTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
