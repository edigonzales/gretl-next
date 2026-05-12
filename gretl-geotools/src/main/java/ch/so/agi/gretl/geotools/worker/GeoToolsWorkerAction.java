package ch.so.agi.gretl.geotools.worker;

import org.gradle.workers.WorkAction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class GeoToolsWorkerAction implements WorkAction<GeoToolsWorkParameters> {

    private static final String RUNTIME_CLASS = "ch.so.agi.gretl.geotools.worker.GeoToolsWorkerRuntime";

    @Override
    public void execute() {
        String operation = getParameters().getOperation().get();
        Map<String, String> parameters = getParameters().getParameters().getOrElse(Collections.emptyMap());
        List<Double> values = getParameters().getValues().getOrElse(Collections.emptyList());
        ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Class<?> runtimeClass = Class.forName(RUNTIME_CLASS);
            Thread.currentThread().setContextClassLoader(runtimeClass.getClassLoader());
            Method execute = runtimeClass.getMethod("execute", String.class, Map.class, List.class);
            execute.invoke(null, operation, parameters, values);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not invoke GeoTools worker runtime.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(previousContextClassLoader);
        }
    }
}
