import * as vscode from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
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
        const javaCommand = resolveJavaCommand(config.javaPath, this.context);
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

export function resolveJavaCommand(
    configuredPath: string,
    context?: vscode.ExtensionContext
): string {
    if (configuredPath) {
        return configuredPath;
    }
    if (context) {
        const bundled = resolveBundledJavaPath(context);
        if (bundled) {
            return bundled.command;
        }
    }
    return 'java';
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

export function runtimePlatformId(
    platform: NodeJS.Platform = os.platform(),
    arch: string = os.arch()
): string | undefined {
    if (platform === 'darwin' && arch === 'arm64') {
        return 'darwin-arm64';
    }
    if (platform === 'darwin' && arch === 'x64') {
        return 'darwin-x64';
    }
    if (platform === 'linux' && arch === 'arm64') {
        return 'linux-arm64';
    }
    if (platform === 'linux' && arch === 'x64') {
        return 'linux-x64';
    }
    if (platform === 'win32' && arch === 'x64') {
        return 'win32-x64';
    }
    return undefined;
}

export function resolveBundledJavaPath(
    context: vscode.ExtensionContext
): { command: string; platformId: string } | undefined {
    const platformId = runtimePlatformId();
    if (!platformId) {
        return undefined;
    }

    const executable = os.platform() === 'win32' ? 'java.exe' : 'java';
    const command = context.asAbsolutePath(path.join('server', 'jre', platformId, 'bin', executable));
    if (!fs.existsSync(command)) {
        return undefined;
    }

    return {
        command,
        platformId
    };
}
