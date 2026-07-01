import * as vscode from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';
import * as path from 'path';
import * as fs from 'fs';
import { loadConfig } from './config';

export class GretlLanguageClientController {
    private client: LanguageClient | undefined;

    constructor(
        private readonly context: vscode.ExtensionContext,
        private readonly outputChannel: vscode.OutputChannel
    ) {}

    async start(): Promise<void> {
        const config = loadConfig();
        const serverOptions = this.buildServerOptions(config);
        const clientOptions: LanguageClientOptions = {
            documentSelector: [
                { scheme: 'file', language: 'groovy', pattern: '**/*.gradle' },
            ],
            outputChannel: this.outputChannel,
            synchronize: {
                fileEvents: vscode.workspace.createFileSystemWatcher(
                    '**/*.{gradle,sql,properties}'
                ),
            },
        };

        this.client = new LanguageClient(
            'gretl-lsp',
            'GRETL Language Server',
            serverOptions,
            clientOptions
        );

        this.outputChannel.appendLine('Starting GRETL language server...');
        await this.client.start();
        this.outputChannel.appendLine('GRETL language server started');
    }

    async stop(): Promise<void> {
        if (this.client) {
            await this.client.stop();
            this.outputChannel.appendLine('GRETL language server stopped');
        }
    }

    async restart(): Promise<void> {
        if (this.client) {
            await this.stop();
        }
        await this.start();
    }

    getClient(): LanguageClient | undefined {
        return this.client;
    }

    private buildServerOptions(config: {
        javaPath: string;
        jarPath: string;
        jvmArgs: string[];
    }): ServerOptions {
        const javaCommand = resolveJavaCommand(config.javaPath);
        const serverJarPath = resolveServerJar(
            config.jarPath,
            this.context.extensionPath
        );

        if (!fs.existsSync(serverJarPath)) {
            this.outputChannel.appendLine(
                `WARNING: Server JAR not found at ${serverJarPath}`
            );
            this.outputChannel.appendLine(
                "Build it with: ./gradlew copyDevGretlServerJar"
            );
        }

        const args = [...config.jvmArgs, '-jar', serverJarPath, '--stdio'];

        this.outputChannel.appendLine(
            `Server command: ${javaCommand} ${args.join(' ')}`
        );

        return {
            command: javaCommand,
            args,
            options: {
                env: { ...process.env },
            },
        };
    }
}

export function resolveJavaCommand(configuredPath: string): string {
    return configuredPath || 'java';
}

export function resolveServerJar(
    configuredPath: string,
    extensionPath: string
): string {
    if (configuredPath) {
        return configuredPath;
    }
    return path.join(extensionPath, 'server', 'gretl-lsp-all.jar');
}
