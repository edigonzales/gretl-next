import * as vscode from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';

export interface GretlOverviewModel {
    uri: string;
    tasks: OverviewTask[];
    graph: {
        nodes: TaskGraphNode[];
        edges: TaskGraphEdge[];
        problems: TaskGraphProblem[];
    };
    diagnostics: OverviewDiagnostic[];
    sqlParameterReport: {
        sqlFiles: string[];
        missingParams: MissingParam[];
        unusedParams: UnusedParam[];
    };
}

export interface OverviewTask {
    name: string;
    typeName: string;
    line: number;
    allRequiredPresent: boolean;
    requiredProperties: string[];
}

export interface TaskGraphNode {
    taskName: string;
    taskType: string;
    range: { start: { line: number; character: number }; end: { line: number; character: number } };
    status: string;
    missingRequiredProperties: string[];
    diagnosticCount: number;
}

export interface TaskGraphEdge {
    fromTask: string;
    toTask: string;
    kind: string;
}

export interface TaskGraphProblem {
    message: string;
    severity: string;
}

export interface OverviewDiagnostic {
    message: string;
    severity: string;
    range: { start: { line: number; character: number }; end: { line: number; character: number } };
    taskName: string;
}

export interface MissingParam {
    paramName: string;
    sqlFile: string;
}

export interface UnusedParam {
    paramName: string;
    taskName: string;
}

function escapeHtml(text: string): string {
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

function severityClass(severity: string): string {
    switch (severity) {
        case 'Error': return 'severity-error';
        case 'Warning': return 'severity-warning';
        case 'Info': return 'severity-info';
        case 'Hint': return 'severity-hint';
        default: return '';
    }
}

function statusClass(status: string): string {
    switch (status) {
        case 'OK': return 'status-ok';
        case 'WARNING': return 'status-warning';
        case 'ERROR': return 'status-error';
        default: return '';
    }
}

function statusIcon(status: string): string {
    switch (status) {
        case 'OK': return '\u2713';
        case 'WARNING': return '\u26A0';
        case 'ERROR': return '\u2717';
        default: return '?';
    }
}

export class GretlOverviewPanel {
    public static currentPanel: GretlOverviewPanel | undefined;
    private static readonly viewType = 'gretlOverview';

    private readonly panel: vscode.WebviewPanel;
    private readonly extensionUri: vscode.Uri;
    private disposables: vscode.Disposable[] = [];

    public static async openOrReveal(
        context: vscode.ExtensionContext,
        client: LanguageClient,
        documentUri: vscode.Uri
    ): Promise<void> {
        const column = vscode.ViewColumn.Beside;

        if (GretlOverviewPanel.currentPanel) {
            GretlOverviewPanel.currentPanel.panel.reveal(column);
            await GretlOverviewPanel.currentPanel.refresh(client, documentUri);
            return;
        }

        const panel = vscode.window.createWebviewPanel(
            GretlOverviewPanel.viewType,
            'GRETL Overview',
            column,
            {
                enableScripts: true,
                retainContextWhenHidden: true,
                localResourceRoots: [],
            }
        );

        GretlOverviewPanel.currentPanel = new GretlOverviewPanel(panel, context.extensionUri);
        await GretlOverviewPanel.currentPanel.refresh(client, documentUri);
    }

    private constructor(panel: vscode.WebviewPanel, extensionUri: vscode.Uri) {
        this.panel = panel;
        this.extensionUri = extensionUri;

        this.panel.onDidDispose(() => this.dispose(), null, this.disposables);

        this.panel.webview.onDidReceiveMessage(
            (message) => {
                if (message.command === 'navigateToTask' && typeof message.line === 'number') {
                    this.navigateToLine(message.line);
                }
            },
            null,
            this.disposables
        );
    }

    private async refresh(client: LanguageClient, documentUri: vscode.Uri): Promise<void> {
        this.panel.webview.html = this.buildLoadingHtml();

        try {
            const result = await client.sendRequest<GretlOverviewModel>(
                'workspace/executeCommand',
                {
                    command: 'gretl.getOverview',
                    arguments: [{ uri: documentUri.toString() }],
                }
            );

            if (result && !(result as any).error) {
                this.panel.webview.html = this.buildHtml(result);
            } else if ((result as any)?.error) {
                this.panel.webview.html = this.buildErrorHtml(
                    escapeHtml((result as any).error)
                );
            }
        } catch (e) {
            this.panel.webview.html = this.buildErrorHtml(
                escapeHtml(String(e))
            );
        }
    }

    public dispose(): void {
        GretlOverviewPanel.currentPanel = undefined;
        this.panel.dispose();
        for (const d of this.disposables) {
            d.dispose();
        }
    }

    private navigateToLine(line: number): void {
        const editor = vscode.window.activeTextEditor;
        if (editor) {
            const position = new vscode.Position(line, 0);
            editor.selection = new vscode.Selection(position, position);
            editor.revealRange(
                new vscode.Range(position, position),
                vscode.TextEditorRevealType.InCenter
            );
        }
    }

    private buildLoadingHtml(): string {
        return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline';">
<style>
body { font-family: var(--vscode-font-family); color: var(--vscode-foreground); padding: 16px; }
.spinner { display: inline-block; width: 20px; height: 20px; border: 2px solid var(--vscode-foreground); border-top-color: transparent; border-radius: 50%; animation: spin 0.8s linear infinite; margin-right: 8px; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
</head>
<body>
<p><span class="spinner"></span>Loading overview...</p>
</body>
</html>`;
    }

    private buildErrorHtml(message: string): string {
        return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; script-src 'none';">
<style>
body { font-family: var(--vscode-font-family); color: var(--vscode-foreground); padding: 16px; }
.error { color: var(--vscode-errorForeground); background: var(--vscode-inputValidation-errorBackground); border: 1px solid var(--vscode-inputValidation-errorBorder); padding: 12px; border-radius: 4px; }
</style>
</head>
<body>
<div class="error">${message}</div>
</body>
</html>`;
    }

    private buildHtml(model: GretlOverviewModel): string {
        const summary = this.buildSummary(model);
        const pipeline = this.buildPipeline(model.graph);
        const tasks = this.buildTasksSection(model.tasks);
        const diagnostics = this.buildDiagnosticsSection(model.diagnostics);
        const sqlFiles = this.buildSqlFilesSection(model.sqlParameterReport?.sqlFiles || []);
        const sqlParams = this.buildSqlParamsSection(model.sqlParameterReport);

        return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline';">
<style>
body {
    font-family: var(--vscode-font-family);
    font-size: var(--vscode-font-size);
    color: var(--vscode-foreground);
    background: var(--vscode-editor-background);
    padding: 16px;
    margin: 0;
    line-height: 1.5;
}
h1 { font-size: 1.4em; margin: 0 0 4px 0; }
h2 { font-size: 1.1em; margin: 20px 0 8px 0; border-bottom: 1px solid var(--vscode-widget-border); padding-bottom: 4px; }
.section { margin-bottom: 16px; }
.summary { display: flex; gap: 16px; flex-wrap: wrap; }
.summary-item {
    padding: 8px 16px;
    border-radius: 4px;
    background: var(--vscode-badge-background);
    color: var(--vscode-badge-foreground);
    font-weight: 600;
}
.summary-item.ok { background: var(--vscode-testing-iconPassed); color: #fff; }
.summary-item.warn { background: var(--vscode-testing-iconQueued); color: #000; }
.summary-item.err { background: var(--vscode-testing-iconFailed); color: #fff; }

.pipeline { padding: 8px 0; }
.pipeline-node {
    display: flex;
    align-items: center;
    padding: 6px 12px;
    margin: 4px 0;
    border-radius: 4px;
    background: var(--vscode-textBlockQuote-background);
    border-left: 3px solid var(--vscode-foreground);
}
.pipeline-node.ok { border-left-color: var(--vscode-testing-iconPassed); }
.pipeline-node.warn { border-left-color: var(--vscode-testing-iconQueued); }
.pipeline-node.err { border-left-color: var(--vscode-testing-iconFailed); }
.pipeline-node-name { font-weight: 600; margin-right: 8px; }
.pipeline-node-type { opacity: 0.7; font-size: 0.9em; margin-right: 8px; }
.pipeline-node-status { margin-left: auto; font-size: 0.85em; }
.pipeline-edge {
    padding: 2px 12px 2px 32px;
    font-size: 0.85em;
    opacity: 0.7;
    color: var(--vscode-descriptionForeground);
}
.pipeline-edge::before { content: '\\2191 '; }

.task-card {
    border: 1px solid var(--vscode-widget-border);
    border-radius: 4px;
    padding: 10px 14px;
    margin: 6px 0;
}
.task-card-header { display: flex; align-items: center; gap: 8px; }
.task-card-name { font-weight: 600; cursor: pointer; text-decoration: underline; }
.task-card-name:hover { color: var(--vscode-textLink-activeForeground); }
.task-card-type { opacity: 0.7; font-size: 0.9em; }
.task-card-status { margin-left: auto; }
.task-card-required { margin-top: 6px; font-size: 0.85em; }
.task-card-required .missing { color: var(--vscode-testing-iconFailed); }
.task-card-required .present { color: var(--vscode-testing-iconPassed); }

.diag-table { width: 100%; border-collapse: collapse; font-size: 0.9em; }
.diag-table th { text-align: left; padding: 4px 8px; border-bottom: 2px solid var(--vscode-widget-border); }
.diag-table td { padding: 4px 8px; border-bottom: 1px solid var(--vscode-widget-border); vertical-align: top; }
.diag-table .severity-badge {
    display: inline-block;
    padding: 1px 6px;
    border-radius: 3px;
    font-size: 0.8em;
    font-weight: 600;
    text-transform: uppercase;
}
.severity-error { background: var(--vscode-inputValidation-errorBackground); color: var(--vscode-inputValidation-errorForeground); border: 1px solid var(--vscode-inputValidation-errorBorder); }
.severity-warning { background: var(--vscode-inputValidation-warningBackground); color: var(--vscode-inputValidation-warningForeground); border: 1px solid var(--vscode-inputValidation-warningBorder); }
.severity-info { background: var(--vscode-inputValidation-infoBackground); color: var(--vscode-inputValidation-infoForeground); border: 1px solid var(--vscode-inputValidation-infoBorder); }
.severity-hint { background: var(--vscode-textBlockQuote-background); color: var(--vscode-descriptionForeground); border: 1px solid var(--vscode-widget-border); }

.status-ok { color: var(--vscode-testing-iconPassed); }
.status-warning { color: var(--vscode-testing-iconQueued); }
.status-error { color: var(--vscode-testing-iconFailed); }

.file-list { list-style: none; padding: 0; }
.file-list li { padding: 3px 0; font-family: var(--vscode-editor-font-family); font-size: 0.9em; }
.file-list li::before { content: '\\1F4C4 '; }

.empty { opacity: 0.5; font-style: italic; }
</style>
</head>
<body>
<h1>GRETL Job Overview</h1>
${summary}
${pipeline}
${tasks}
${diagnostics}
${sqlFiles}
${sqlParams}
<script>
const vscode = acquireVsCodeApi();
document.querySelectorAll('.task-card-name').forEach(el => {
    el.addEventListener('click', () => {
        const line = parseInt(el.getAttribute('data-line'), 10);
        vscode.postMessage({ command: 'navigateToTask', line: line });
    });
});
</script>
</body>
</html>`;
    }

    private buildSummary(model: GretlOverviewModel): string {
        const taskCount = model.tasks.length;
        const errorCount = model.diagnostics.filter((d) => d.severity === 'Error').length;
        const warningCount = model.diagnostics.filter((d) => d.severity === 'Warning').length;
        const infoCount = model.diagnostics.filter((d) => d.severity === 'Info' || d.severity === 'Hint').length;
        const parseMode = 'Groovy AST';

        let html = '<div class="section">';
        html += `<div class="summary">`;
        html += `<div class="summary-item ok">${escapeHtml(String(taskCount))} Tasks</div>`;
        if (errorCount > 0) {
            html += `<div class="summary-item err">${escapeHtml(String(errorCount))} Errors</div>`;
        }
        if (warningCount > 0) {
            html += `<div class="summary-item warn">${escapeHtml(String(warningCount))} Warnings</div>`;
        }
        if (infoCount > 0) {
            html += `<div class="summary-item">${escapeHtml(String(infoCount))} Info/Hints</div>`;
        }
        html += `<div class="summary-item">${escapeHtml(parseMode)}</div>`;
        html += `</div></div>`;
        return html;
    }

    private buildPipeline(graph: GretlOverviewModel['graph']): string {
        const nodes = graph.nodes || [];
        const edges = graph.edges || [];
        const problems = graph.problems || [];

        if (nodes.length === 0) {
            return '';
        }

        let html = '<div class="section"><h2>Pipeline</h2>';
        html += '<div class="pipeline">';

        const painted = new Set<string>();
        for (const node of nodes) {
            const cls = statusClass(node.status);
            html += '<div class="pipeline-node ' + cls + '">';
            html += '<span class="pipeline-node-name">' + escapeHtml(node.taskName) + '</span>';
            html += '<span class="pipeline-node-type">' + escapeHtml(node.taskType) + '</span>';
            html += '<span class="pipeline-node-status ' + cls + '">' + statusIcon(node.status) + '</span>';
            html += '</div>';

            const outgoing = edges.filter((e) => e.fromTask === node.taskName);
            for (const edge of outgoing) {
                html += '<div class="pipeline-edge">';
                html += escapeHtml(edge.kind) + ' \u2192 ' + escapeHtml(edge.toTask);
                html += '</div>';
            }
        }

        for (const problem of problems) {
            html += '<div class="pipeline-edge ' + severityClass(problem.severity) + '">';
            html += '\u26A0 ' + escapeHtml(problem.message);
            html += '</div>';
        }

        html += '</div></div>';
        return html;
    }

    private buildTasksSection(tasks: OverviewTask[]): string {
        if (tasks.length === 0) {
            return '<div class="section"><h2>Tasks</h2><p class="empty">No GRETL tasks found in this file.</p></div>';
        }

        let html = '<div class="section"><h2>Tasks</h2>';
        for (const task of tasks) {
            html += '<div class="task-card">';
            html += '<div class="task-card-header">';
            html += '<span class="task-card-name" data-line="' + escapeHtml(String(task.line)) + '">'
                + escapeHtml(task.name) + '</span>';
            html += '<span class="task-card-type">' + escapeHtml(task.typeName) + '</span>';
            html += '<span class="task-card-status">';
            if (task.allRequiredPresent) {
                html += '<span class="status-ok">\u2713 All required</span>';
            } else {
                html += '<span class="status-error">\u2717 Missing required</span>';
            }
            html += '</span></div>';

            if (task.requiredProperties.length > 0) {
                html += '<div class="task-card-required">Required: ';
                const presentSet = new Set<string>();
                for (const prop of task.requiredProperties) {
                    const present = task.allRequiredPresent ? true : false;
                    html += '<span class="' + (present ? 'present' : 'missing') + '">'
                        + escapeHtml(prop) + '</span> ';
                }
                html += '</div>';
            }
            html += '</div>';
        }
        html += '</div>';
        return html;
    }

    private buildDiagnosticsSection(diagnostics: OverviewDiagnostic[]): string {
        if (diagnostics.length === 0) {
            return '<div class="section"><h2>Diagnostics</h2><p class="empty">No diagnostics.</p></div>';
        }

        let html = '<div class="section"><h2>Diagnostics</h2>';
        html += '<table class="diag-table"><thead><tr>';
        html += '<th>Severity</th><th>Task</th><th>Line</th><th>Message</th>';
        html += '</tr></thead><tbody>';

        for (const diag of diagnostics) {
            const line = (diag.range?.start?.line ?? 0) + 1;
            html += '<tr>';
            html += '<td><span class="severity-badge ' + severityClass(diag.severity) + '">'
                + escapeHtml(diag.severity) + '</span></td>';
            html += '<td>' + (diag.taskName ? escapeHtml(diag.taskName) : '-') + '</td>';
            html += '<td>' + escapeHtml(String(line)) + '</td>';
            html += '<td>' + escapeHtml(diag.message) + '</td>';
            html += '</tr>';
        }

        html += '</tbody></table></div>';
        return html;
    }

    private buildSqlFilesSection(sqlFilePaths: string[]): string {
        if (sqlFilePaths.length === 0) {
            return '';
        }

        let html = '<div class="section"><h2>SQL Files</h2>';
        html += '<ul class="file-list">';
        for (const file of sqlFilePaths) {
            html += '<li>' + escapeHtml(file) + '</li>';
        }
        html += '</ul></div>';
        return html;
    }

    private buildSqlParamsSection(report: GretlOverviewModel['sqlParameterReport']): string {
        if (!report) {
            return '';
        }

        const missing = report.missingParams || [];
        const unused = report.unusedParams || [];

        if (missing.length === 0 && unused.length === 0) {
            return '';
        }

        let html = '<div class="section"><h2>SQL Parameters</h2>';

        if (missing.length > 0) {
            html += '<h3 class="status-warning">Missing Parameters (used in SQL but not provided)</h3>';
            html += '<table class="diag-table"><thead><tr><th>Parameter</th><th>SQL File</th></tr></thead><tbody>';
            for (const mp of missing) {
                html += '<tr><td><code>' + escapeHtml(mp.paramName) + '</code></td>';
                html += '<td>' + escapeHtml(mp.sqlFile) + '</td></tr>';
            }
            html += '</tbody></table>';
        }

        if (unused.length > 0) {
            html += '<h3 class="severity-info">Unused Parameters (provided but not used in SQL)</h3>';
            html += '<table class="diag-table"><thead><tr><th>Parameter</th><th>Task</th></tr></thead><tbody>';
            for (const up of unused) {
                html += '<tr><td><code>' + escapeHtml(up.paramName) + '</code></td>';
                html += '<td>' + escapeHtml(up.taskName) + '</td></tr>';
            }
            html += '</tbody></table>';
        }

        html += '</div>';
        return html;
    }
}
