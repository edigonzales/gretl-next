package ch.so.agi.gretl.control.api;

public record RunClaimResponse(ClaimedRun run, String message) {
    public RunClaimResponse(ClaimedRun run) {
        this(run, null);
    }

    public boolean hasRun() {
        return run != null;
    }
}
