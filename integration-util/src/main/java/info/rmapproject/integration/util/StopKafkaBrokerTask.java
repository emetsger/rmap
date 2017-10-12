package info.rmapproject.integration.util;

import org.apache.tools.ant.BuildException;

public class StopKafkaBrokerTask extends AbstractKafkaBrokerTask {

    @Override
    public void execute() throws BuildException {
        if (BROKER == null || !BROKER_IS_STARTED) {
            return;
        }

        BROKER.after();
    }
}
