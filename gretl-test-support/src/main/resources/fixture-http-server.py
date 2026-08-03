from http.server import BaseHTTPRequestHandler, HTTPServer
import base64, hashlib, json, re

configurations = {}
logs = {}

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def body(self):
        length = self.headers.get('Content-Length')
        if length is not None:
            return self.rfile.read(int(length))
        if self.headers.get('Transfer-Encoding', '').lower() != 'chunked':
            return b''
        chunks = []
        while True:
            line = self.rfile.readline().strip()
            if not line:
                continue
            size = int(line.split(b';', 1)[0], 16)
            if size == 0:
                self.rfile.readline()
                break
            chunks.append(self.rfile.read(size))
            self.rfile.read(2)
        return b''.join(chunks)

    def send(self, code, body=b'', content_type='text/plain'):
        self.send_response(code)
        self.send_header('Content-Type', content_type)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        if body:
            self.wfile.write(body)

    def token(self):
        return self.path.split('/', 2)[2] if self.path.count('/') >= 2 else ''

    def authenticated(self, token):
        configured = configurations.get(token)
        if configured is None:
            return False
        header = self.headers.get('Authorization', '')
        if not header.startswith('Basic '):
            return False
        try:
            user, password = base64.b64decode(header[6:]).decode('utf-8').split(':', 1)
            return user == configured['username'] and password == configured['password']
        except Exception:
            return False

    def multipart(self, body):
        names, files = set(), {}
        match = re.search(r'boundary=([^;]+)', self.headers.get('Content-Type', ''))
        if not match:
            return names, files
        boundary = ('--' + match.group(1).strip('"')).encode()
        for part in body.split(boundary):
            if b'Content-Disposition' not in part:
                continue
            header, _, payload = part.partition(b'\r\n\r\n')
            if payload.endswith(b'\r\n'):
                payload = payload[:-2]
            field = re.search(br'name="([^"]+)"', header)
            if not field:
                continue
            name = field.group(1).decode('utf-8', 'replace')
            names.add(name)
            if b'filename=' in header:
                files[name] = hashlib.sha256(payload).hexdigest()
        return names, files

    def app(self):
        token = self.headers.get('X-GRETL-RUN-TOKEN', '')
        if not self.authenticated(token):
            self.send(401, b'unauthorized')
            return
        body = self.body()
        content_type = self.headers.get('Content-Type', '')
        fields, files = self.multipart(body) if content_type.startswith('multipart/') else (set(), {})
        logs.setdefault(token, []).append({
            'method': self.command, 'path': self.path, 'contentType': content_type,
            'bodyLength': len(body), 'bodySha256': hashlib.sha256(body).hexdigest(),
            'textBody': body.decode('utf-8', 'replace') if content_type.startswith(('text/', 'application/json')) else None,
            'authenticated': True, 'safeHeaders': {'Content-Type': content_type} if content_type else {},
            'multipartFieldNames': sorted(fields), 'multipartFileSha256': files})
        if self.path == '/download': self.send(200, b'download-content\n')
        elif self.path == '/text': self.send(201, b'accepted')
        elif self.path == '/form': self.send(200, b'form-ok')
        else: self.send(200, b'ok')

    def do_GET(self):
        if self.path == '/health': self.send(200, b'ok')
        elif self.path.startswith('/requests/'): self.send(200, json.dumps(logs.get(self.token(), [])).encode(), 'application/json')
        elif self.path in ('/download', '/text', '/binary', '/form'): self.app()
        else: self.send(404, b'not found')

    def do_POST(self):
        if self.path.startswith('/configure/'):
            try:
                value = json.loads(self.body().decode('utf-8'))
                configurations[self.token()] = {'username': value['username'], 'password': value['password']}
                logs[self.token()] = []
                self.send(204)
            except Exception: self.send(400, b'bad configuration')
        elif self.path.startswith('/reset/'):
            logs[self.token()] = []
            self.send(204)
        else: self.app()

    def do_DELETE(self):
        if self.path.startswith('/configure/'):
            configurations.pop(self.token(), None)
            logs.pop(self.token(), None)
            self.send(204)
        else: self.send(404, b'not found')

HTTPServer(('0.0.0.0', 8080), Handler).serve_forever()
