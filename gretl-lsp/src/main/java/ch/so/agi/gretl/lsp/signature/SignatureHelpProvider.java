package ch.so.agi.gretl.lsp.signature;

import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SignatureHelpProvider {

    private final GretlMetadata metadata;

    public SignatureHelpProvider(GretlMetadata metadata) {
        this.metadata = metadata;
    }

    public Optional<SignatureHelp> signatureHelp(GretlScript script, Position position) {
        Optional<GretlTaskBlock> taskBlock = findTaskBlock(script, position);
        if (taskBlock.isEmpty() || taskBlock.get().typeName().isEmpty()) {
            return Optional.empty();
        }

        GretlTaskBlock block = taskBlock.get();
        Optional<TaskMetadata> taskMeta = metadata.findTask(block.typeName().get());
        if (taskMeta.isEmpty()) {
            return Optional.empty();
        }

        Optional<GretlDslCall> activeCall = findActiveCall(block, position);
        if (activeCall.isEmpty()) {
            return Optional.empty();
        }

        GretlDslCall call = activeCall.get();
        Optional<PropertyMetadata> propOpt = taskMeta.get().findProperty(call.name());
        if (propOpt.isEmpty()) {
            return Optional.empty();
        }

        PropertyMetadata prop = propOpt.get();
        SignatureHelp help = new SignatureHelp();
        List<SignatureInformation> signatures = new ArrayList<>();

        for (AcceptedForm form : prop.acceptedForms()) {
            if (!form.legacy() && form.argumentCount() != null && form.argumentCount() > 0) {
                SignatureInformation sig = new SignatureInformation();
                sig.setLabel(form.signature() != null ? form.signature() : prop.name());
                List<ParameterInformation> params = buildParameters(form.signature(), form.argumentCount());
                sig.setParameters(params);
                signatures.add(sig);
            }
        }

        if (signatures.isEmpty()) {
            return Optional.empty();
        }

        help.setSignatures(signatures);
        help.setActiveSignature(0);

        int activeParam = activeParameterIndex(call.sourceText(), position);
        int maxParams = signatures.get(0).getParameters().size();
        help.setActiveParameter(Math.min(activeParam, maxParams - 1));

        return Optional.of(help);
    }

    private Optional<GretlTaskBlock> findTaskBlock(GretlScript script, Position position) {
        return script.tasks().stream()
                .filter(t -> t.fullRange() != null && inside(position, t.fullRange()))
                .findFirst();
    }

    private Optional<GretlDslCall> findActiveCall(GretlTaskBlock block, Position position) {
        return block.calls().stream()
                .filter(c -> inside(position, c.fullRange()))
                .findFirst();
    }

    private List<ParameterInformation> buildParameters(String signature, int argumentCount) {
        List<ParameterInformation> params = new ArrayList<>();
        if (signature == null) {
            for (int i = 0; i < argumentCount; i++) {
                params.add(new ParameterInformation("arg" + (i + 1)));
            }
            return params;
        }

        String afterName = signature;
        int space = signature.indexOf(' ');
        if (space > 0) {
            afterName = signature.substring(space + 1);
        }

        String[] parts = afterName.split(",");
        for (String part : parts) {
            String label = part.trim();
            if (!label.isEmpty()) {
                params.add(new ParameterInformation(label));
            }
        }

        if (params.isEmpty()) {
            for (int i = 0; i < argumentCount; i++) {
                params.add(new ParameterInformation("arg" + (i + 1)));
            }
        }

        return params;
    }

    int activeParameterIndex(String sourceText, Position position) {
        if (sourceText == null) {
            return 0;
        }

        String beforeCursor;
        try {
            beforeCursor = sourceText.substring(0, Math.min(position.getCharacter(), sourceText.length()));
        } catch (StringIndexOutOfBoundsException e) {
            return 0;
        }

        int commaCount = 0;
        for (char c : beforeCursor.toCharArray()) {
            if (c == ',') {
                commaCount++;
            }
        }
        return commaCount;
    }

    static boolean inside(Position pos, org.eclipse.lsp4j.Range range) {
        if (range == null) {
            return false;
        }
        if (pos.getLine() < range.getStart().getLine()) {
            return false;
        }
        if (pos.getLine() > range.getEnd().getLine()) {
            return false;
        }
        if (pos.getLine() == range.getStart().getLine()
                && pos.getCharacter() < range.getStart().getCharacter()) {
            return false;
        }
        if (pos.getLine() == range.getEnd().getLine()
                && pos.getCharacter() > range.getEnd().getCharacter()) {
            return false;
        }
        return true;
    }
}
