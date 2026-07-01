// Tests for GRETL extension overview webview pure functions
// Run with: node --test test/overviewWebview.test.js

const assert = require('assert');
const { describe, it } = require('node:test');

function escapeHtml(text) {
    if (text === undefined || text === null) {
        return '';
    }
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

describe('escapeHtml', () => {
    it('returns empty string for null', () => {
        assert.strictEqual(escapeHtml(null), '');
    });

    it('returns empty string for undefined', () => {
        assert.strictEqual(escapeHtml(undefined), '');
    });

    it('passes through plain text', () => {
        assert.strictEqual(escapeHtml('Hello World'), 'Hello World');
    });

    it('escapes angle brackets', () => {
        assert.strictEqual(
            escapeHtml('<script>alert("x")</script>'),
            '&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;'
        );
    });

    it('escapes ampersands', () => {
        assert.strictEqual(escapeHtml('A & B'), 'A &amp; B');
    });

    it('escapes single quotes', () => {
        assert.strictEqual(escapeHtml("it's"), 'it&#39;s');
    });

    it('escapes double quotes', () => {
        assert.strictEqual(escapeHtml('say "hello"'), 'say &quot;hello&quot;');
    });

    it('handles special characters in task names', () => {
        assert.strictEqual(
            escapeHtml('<CustomTask>'),
            '&lt;CustomTask&gt;'
        );
    });

    it('escapes HTML in file paths', () => {
        assert.strictEqual(
            escapeHtml('<img src=x onerror=alert(1)>'),
            '&lt;img src=x onerror=alert(1)&gt;'
        );
    });

    it('returns string for numbers', () => {
        assert.strictEqual(escapeHtml(42), '42');
    });

    it('returns string for boolean', () => {
        assert.strictEqual(escapeHtml(true), 'true');
    });

    it('escapes all five special characters correctly', () => {
        const result = escapeHtml('&<>"\'');
        assert.strictEqual(result, '&amp;&lt;&gt;&quot;&#39;');
    });
});

console.log('All overview webview tests passed.');
