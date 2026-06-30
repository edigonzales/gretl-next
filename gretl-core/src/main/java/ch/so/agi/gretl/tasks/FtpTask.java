package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ftp.FtpConnectionSpec;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

abstract class FtpTask extends AbstractCoreGretlTask {
    private String server;
    private String user;
    private String password;
    private String systemType = FTPClientConfig.SYST_UNIX;
    private String fileSeparator;
    private Boolean passiveMode = true;
    private Long controlKeepAliveTimeout = 300L;

    @Input
    public String getServer() {
        return server;
    }

    @Input
    public String getUser() {
        return user;
    }

    @Internal
    public String getPassword() {
        return password;
    }

    @Input
    @Optional
    public String getSystemType() {
        return systemType;
    }

    @Input
    @Optional
    public String getFileSeparator() {
        return fileSeparator;
    }

    @Input
    @Optional
    public Boolean getPassiveMode() {
        return passiveMode;
    }

    @Input
    @Optional
    public Long getControlKeepAliveTimeout() {
        return controlKeepAliveTimeout;
    }

    public void setServer(String server) {
        this.server = server;
    }

    @GretlDslMethod(required = true, description = "Configures the FTP server host, optionally as host:port.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den FTP-Server-Host, optional host:port.") })
    public void server(String server) {
        setServer(server);
    }

    public void setUser(String user) {
        this.user = user;
    }

    @GretlDslMethod(required = true, description = "Configures the FTP username.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den FTP-Benutzernamen.") })
    public void user(String user) {
        setUser(user);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @GretlDslMethod(required = true, description = "Configures the FTP password.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert das FTP-Passwort.") })
    public void password(String password) {
        setPassword(password);
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    @GretlDslMethod(description = "Configures the FTP server system type.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den FTP-Server-Systemtyp.") })
    public void systemType(String systemType) {
        setSystemType(systemType);
    }

    public void setFileSeparator(String fileSeparator) {
        this.fileSeparator = fileSeparator;
    }

    @GretlDslMethod(description = "Configures the path separator used on the FTP server.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert das Pfadtrennzeichen auf dem FTP-Server.") })
    public void fileSeparator(String fileSeparator) {
        setFileSeparator(fileSeparator);
    }

    public void setPassiveMode(Boolean passiveMode) {
        this.passiveMode = passiveMode;
    }

    @GretlDslMethod(description = "Configures passive FTP mode.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den passiven FTP-Modus.") })
    public void passiveMode(boolean passiveMode) {
        setPassiveMode(passiveMode);
    }

    public void setControlKeepAliveTimeout(Long controlKeepAliveTimeout) {
        this.controlKeepAliveTimeout = controlKeepAliveTimeout;
    }

    @GretlDslMethod(description = "Configures the FTP control keep-alive timeout in seconds.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert das FTP-Control-Keep-Alive-Timeout in Sekunden.") })
    public void controlKeepAliveTimeout(long controlKeepAliveTimeout) {
        setControlKeepAliveTimeout(controlKeepAliveTimeout);
    }

    protected FtpConnectionSpec connectionSpec() {
        return new FtpConnectionSpec(
                server,
                user,
                password,
                systemType,
                fileSeparator,
                passiveMode == null || passiveMode,
                controlKeepAliveTimeout == null ? 300L : controlKeepAliveTimeout
        );
    }
}
