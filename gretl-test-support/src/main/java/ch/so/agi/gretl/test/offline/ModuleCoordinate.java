package ch.so.agi.gretl.test.offline;

public record ModuleCoordinate(String group, String module, String version) {
    public String notation() {
        return group + ":" + module + ":" + version;
    }
}
