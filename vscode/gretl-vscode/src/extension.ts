import * as vscode from 'vscode';
import { GretlLanguageClientController } from './languageServer';
import { registerCommands } from './commands';
import { createOutputChannel } from './logging';

let clientController: GretlLanguageClientController | undefined;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
    const outputChannel = createOutputChannel();
    outputChannel.appendLine('GRETL extension activating...');

    clientController = new GretlLanguageClientController(context, outputChannel);

    try {
        await clientController.start();
    } catch (e) {
        outputChannel.appendLine(`Failed to start GRETL language server: ${e}`);
        vscode.window.showErrorMessage(
            'Failed to start GRETL language server. Check the "GRETL" output channel for details.'
        );
    }

    registerCommands(context, clientController, outputChannel);

    outputChannel.appendLine('GRETL extension activated');
}

export async function deactivate(): Promise<void> {
    if (clientController) {
        await clientController.stop();
    }
}
