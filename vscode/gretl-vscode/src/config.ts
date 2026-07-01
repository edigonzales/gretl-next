import * as vscode from 'vscode';

export interface GretlConfig {
    javaPath: string;
    jarPath: string;
    jvmArgs: string[];
    traceServer: string;
}

const defaultConfig: GretlConfig = {
    javaPath: '',
    jarPath: '',
    jvmArgs: [],
    traceServer: 'off',
};

export function loadConfig(): GretlConfig {
    const ws = vscode.workspace.getConfiguration('gretl');
    return {
        javaPath: ws.get<string>('java.path', defaultConfig.javaPath),
        jarPath: ws.get<string>('server.jarPath', defaultConfig.jarPath),
        jvmArgs: ws.get<string[]>('server.jvmArgs', defaultConfig.jvmArgs),
        traceServer: ws.get<string>(
            'trace.server',
            defaultConfig.traceServer
        ),
    };
}
