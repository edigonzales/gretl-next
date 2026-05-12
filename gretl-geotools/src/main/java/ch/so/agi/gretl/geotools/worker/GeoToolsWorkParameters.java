package ch.so.agi.gretl.geotools.worker;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

public interface GeoToolsWorkParameters extends WorkParameters {

    Property<String> getOperation();

    MapProperty<String, String> getParameters();

    ListProperty<Double> getValues();
}
