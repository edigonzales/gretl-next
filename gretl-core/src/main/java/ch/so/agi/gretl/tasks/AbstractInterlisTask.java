package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.services.InterlisBuildService;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

public abstract class AbstractInterlisTask extends AbstractCoreGretlTask {

    @Internal
    public abstract Property<InterlisBuildService> getInterlisService();
}
