package ch.so.agi.gretl.lsp.server;

import java.util.concurrent.atomic.AtomicReference;

public final class ServerLifecycle {

    enum State {
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.RUNNING);

    public boolean isRunning() {
        return state.get() == State.RUNNING;
    }

    public boolean isShuttingDown() {
        return state.get() == State.SHUTTING_DOWN;
    }

    public void transitionToShuttingDown() {
        state.compareAndExchange(State.RUNNING, State.SHUTTING_DOWN);
    }

    public void transitionToShutdown() {
        state.set(State.SHUTDOWN);
    }

    public int exitCode() {
        return 0;
    }
}
