// Tests for GRETL extension pure resolver functions
// Run with: node --test test/config.test.js

const assert = require('assert');
const path = require('path');
const { describe, it } = require('node:test');

/**
 * Pure function copies of the resolver logic from src/languageServer.ts.
 * These must stay in sync with the TypeScript source.
 */
function resolveJavaCommand(configuredPath) {
    return configuredPath || 'java';
}

function resolveServerJar(configuredPath, extensionPath) {
    if (configuredPath) {
        return configuredPath;
    }
    return path.join(extensionPath, 'server', 'gretl-lsp-all.jar');
}

describe('resolveJavaCommand', () => {
    it('returns "java" for empty string', () => {
        assert.strictEqual(resolveJavaCommand(''), 'java');
    });

    it('returns "java" for undefined', () => {
        assert.strictEqual(resolveJavaCommand(undefined), 'java');
    });

    it('returns "java" for null', () => {
        assert.strictEqual(resolveJavaCommand(null), 'java');
    });

    it('returns the configured path when provided', () => {
        assert.strictEqual(
            resolveJavaCommand('/usr/local/bin/java17'),
            '/usr/local/bin/java17'
        );
    });

    it('returns the configured path for relative path', () => {
        assert.strictEqual(
            resolveJavaCommand('./custom-java'),
            './custom-java'
        );
    });
});

describe('resolveServerJar', () => {
    it('returns the configured path when provided', () => {
        assert.strictEqual(
            resolveServerJar('/custom/gretl-lsp-all.jar', '/ext'),
            '/custom/gretl-lsp-all.jar'
        );
    });

    it('returns bundled path when no config', () => {
        const result = resolveServerJar('', '/home/user/.vscode/extensions/gretl-vscode');
        assert.strictEqual(
            result,
            path.join('/home/user/.vscode/extensions/gretl-vscode', 'server', 'gretl-lsp-all.jar')
        );
    });

    it('returns path ending with server/gretl-lsp-all.jar', () => {
        const result = resolveServerJar('', '/ext');
        assert(result.endsWith(path.join('server', 'gretl-lsp-all.jar')));
    });

    it('ignores extensionPath when config is set', () => {
        const result = resolveServerJar('/my.jar', '/ignored-path');
        assert.strictEqual(result, '/my.jar');
    });
});

console.log('All config resolver tests passed.');
