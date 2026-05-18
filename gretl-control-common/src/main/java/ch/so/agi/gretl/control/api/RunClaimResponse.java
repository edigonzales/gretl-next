package ch.so.agi.gretl.control.api;

public record RunClaimResponse(ClaimedRun run) {
    public boolean hasRun() {
        return run != null;
    }
}
