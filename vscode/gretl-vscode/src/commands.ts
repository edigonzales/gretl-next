import * as vscode from 'vscode';
import { GretlLanguageClientController } from './languageServer';

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
        vscode.commands.registerCommand('gretl.openOverview', () => {
            vscode.window.showInformationMessage(
                'GRETL Overview will be available in a future update.'
            );
        })
    );
}
