package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.services.CoreGretlBuildService;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

public abstract class AbstractCoreGretlTask extends DefaultTask {

    @Internal
    public abstract Property<CoreGretlBuildService> getCoreService();
}
