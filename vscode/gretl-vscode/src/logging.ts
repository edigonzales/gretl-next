import * as vscode from 'vscode';

let outputChannel: vscode.OutputChannel | undefined;

export function createOutputChannel(): vscode.OutputChannel {
    if (!outputChannel) {
        outputChannel = vscode.window.createOutputChannel('GRETL', {
            log: true,
        });
    }
    return outputChannel;
}
