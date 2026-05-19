package ch.so.agi.gretl.control.worker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerAgentTest {
    @Test
    void reportsChangedNoClaimMessagesOnlyOnce() {
        WorkerAgent agent = new WorkerAgent(new WorkerProperties(), null, null);

        assertFalse(agent.shouldLogNoClaimMessage(null));
        assertFalse(agent.shouldLogNoClaimMessage(" "));
        assertTrue(agent.shouldLogNoClaimMessage("No queued run matches worker labels []."));
        assertFalse(agent.shouldLogNoClaimMessage("No queued run matches worker labels []."));
        assertTrue(agent.shouldLogNoClaimMessage("No queued run matches worker labels [small]."));
        assertFalse(agent.shouldLogNoClaimMessage("No queued run matches worker labels [small]."));
    }
}
