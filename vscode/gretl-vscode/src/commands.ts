import * as vscode from 'vscode';
import { GretlLanguageClientController } from './languageServer';
import { GretlOverviewPanel } from './overviewWebview';

export function registerCommands(
    context: vscode.ExtensionContext,
    clientController: GretlLanguageClientController,
    outputChannel: vscode.OutputChannel
): void {
    context.subscriptions.push(
        vscode.commands.registerCommand(
            'gretl.restartLanguageServer',
            async () => {
                const restartingInfo =
                    vscode.window.createOutputChannel('GRETL');
                restartingInfo.appendLine(
                    'Restarting GRETL language server...'
                );
                restartingInfo.dispose();

                try {
                    await clientController.restart();
                    vscode.window.showInformationMessage(
                        'GRETL language server restarted.'
                    );
                } catch (e) {
                    vscode.window.showErrorMessage(
                        `Failed to restart language server: ${e}`
                    );
                }
            }
        )
    );

    context.subscriptions.push(
        vscode.commands.registerCommand(
            'gretl.showLanguageServerLogs',
            () => {
                outputChannel.show(true);
            }
        )
    );

    context.subscriptions.push(
        vscode.commands.registerCommand(
            'gretl.openOverview',
            async () => {
                const editor = vscode.window.activeTextEditor;
                if (!editor) {
                    vscode.window.showWarningMessage(
                        'Open a build.gradle file to view the GRETL Overview.'
                    );
                    return;
                }

                const client = clientController.getClient();
                if (!client) {
                    vscode.window.showErrorMessage(
                        'GRETL language server is not running.'
                    );
                    return;
                }

                try {
                    await GretlOverviewPanel.openOrReveal(
                        context,
                        client,
                        editor.document.uri
                    );
                } catch (e) {
                    vscode.window.showErrorMessage(
                        `Failed to open GRETL Overview: ${e}`
                    );
                }
            }
        )
    );

    context.subscriptions.push(
        vscode.commands.registerCommand(
            'gretl.refreshOverview',
            async () => {
                const editor = vscode.window.activeTextEditor;
                if (!editor) {
                    return;
                }

                const client = clientController.getClient();
                if (!client) {
                    return;
                }

                try {
                    await GretlOverviewPanel.openOrReveal(
                        context,
                        client,
                        editor.document.uri
                    );
                } catch {
                    // Silently ignore refresh errors
                }
            }
        )
    );
}
