package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.http.CurlEngine;
import ch.so.agi.gretl.internal.http.CurlMethod;
import ch.so.agi.gretl.internal.http.CurlRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@GretlTaskDoc(name = "Curl", description = "Executes a small HTTP request and validates the response.")
public abstract class Curl extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(Curl.class);

    private String serverUrl;
    private MethodType method;
    private Integer expectedStatusCode;
    private String expectedBody;
    private Map<String, Object> formData = new LinkedHashMap<>();
    private String data;
    private Map<String, String> headers = new LinkedHashMap<>();
    private String user;
    private String password;

    @Input
    public String getServerUrl() {
        return serverUrl;
    }

    @Input
    @Optional
    public MethodType getMethod() {
        return method;
    }

    @Input
    public Integer getExpectedStatusCode() {
        return expectedStatusCode;
    }

    @Input
    @Optional
    public String getExpectedBody() {
        return expectedBody;
    }

    @Internal
    public Map<String, Object> getFormData() {
        return formData;
    }

    @Input
    @Optional
    public Map<String, String> getFormDataSignature() {
        if (formData == null || formData.isEmpty()) {
            return null;
        }
        return formData.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    Object value = entry.getValue();
                    if (value instanceof File file) {
                        return file.getAbsolutePath();
                    }
                    return String.valueOf(value);
                },
                (left, right) -> right,
                LinkedHashMap::new));
    }

    @Input
    @Optional
    public String getData() {
        return data;
    }

    @Input
    @Optional
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Input
    @Optional
    public String getUser() {
        return user;
    }

    @Internal
    public String getPassword() {
        return password;
    }

    @InputFile
    @Optional
    public abstract RegularFileProperty getDataBinary();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getOutputFile();

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @GretlDslMethod(required = true, description = "Configures the request URL.")
    public void serverUrl(String serverUrl) {
        setServerUrl(serverUrl);
    }

    public void setMethod(MethodType method) {
        this.method = method;
    }

    @GretlDslMethod(description = "Configures the HTTP method.")
    public void method(MethodType method) {
        setMethod(method);
    }

    public void setExpectedStatusCode(Integer expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    @GretlDslMethod(required = true, description = "Configures the expected HTTP response status.")
    public void expectedStatusCode(int expectedStatusCode) {
        setExpectedStatusCode(expectedStatusCode);
    }

    public void setExpectedBody(String expectedBody) {
        this.expectedBody = expectedBody;
    }

    @GretlDslMethod(description = "Configures text expected in the response body.")
    public void expectedBody(String expectedBody) {
        setExpectedBody(expectedBody);
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(formData);
    }

    @GretlDslMethod(description = "Configures multipart form data.")
    public void formData(Map<String, Object> formData) {
        setFormData(formData);
    }

    public void setData(String data) {
        this.data = data;
    }

    @GretlDslMethod(description = "Configures a text request body.")
    public void data(String data) {
        setData(data);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }

    @GretlDslMethod(description = "Configures request headers.")
    public void headers(Map<String, String> headers) {
        setHeaders(headers);
    }

    public void setUser(String user) {
        this.user = user;
    }

    @GretlDslMethod(description = "Configures basic-auth username.")
    public void user(String user) {
        setUser(user);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @GretlDslMethod(description = "Configures basic-auth password.")
    public void password(String password) {
        setPassword(password);
    }

    private void configureDataBinary(Object dataBinary) {
        setRegularFile(getDataBinary(), dataBinary);
    }

    @GretlDslMethod(description = "Configures a binary request body file.")
    public void dataBinary(Object dataBinary) {
        configureDataBinary(dataBinary);
    }

    private void configureOutputFile(Object outputFile) {
        setRegularFile(getOutputFile(), outputFile);
    }

    @GretlDslMethod(description = "Configures the file receiving the response body.")
    public void outputFile(Object outputFile) {
        configureOutputFile(outputFile);
    }

    @TaskAction
    public void request() {
        try {
            new CurlEngine().execute(new CurlRequest(
                    serverUrl,
                    method == null ? null : CurlMethod.valueOf(method.name()),
                    expectedStatusCode,
                    expectedBody,
                    headers == null ? Map.of() : headers,
                    formData == null ? Map.of() : formData,
                    data,
                    getDataBinary().isPresent() ? getDataBinary().get().getAsFile().toPath() : null,
                    getOutputFile().isPresent() ? getOutputFile().get().getAsFile().toPath() : null,
                    user,
                    password));
        } catch (Exception e) {
            log.error("Exception in Curl task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    public enum MethodType {
        GET,
        POST
    }
}
