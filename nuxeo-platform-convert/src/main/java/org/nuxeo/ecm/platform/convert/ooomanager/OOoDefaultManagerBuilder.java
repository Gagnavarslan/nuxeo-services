package org.nuxeo.ecm.platform.convert.ooomanager;

import org.apache.commons.io.FileUtils;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeConnectionProtocol;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.artofsolving.jodconverter.office.OfficeUtils;
import org.artofsolving.jodconverter.util.PlatformUtils;
import org.nuxeo.runtime.api.Framework;

import java.io.File;
import java.io.IOException;

public class OOoDefaultManagerBuilder implements OOoManagerBuilder {
    private static final String CONNECTION_PROTOCOL_PROPERTY_KEY = "jod.connection.protocol";

    private static final String MAX_TASKS_PER_PROCESS_PROPERTY_KEY = "jod.max.tasks.per.process";

    private static final String OFFICE_HOME_PROPERTY_KEY = "jod.office.home";

    private static final String TASK_EXECUTION_TIMEOUT_PROPERTY_KEY = "jod.task.execution.timeout";

    private static final String TASK_QUEUE_TIMEOUT_PROPERTY_KEY = "jod.task.queue.timeout";

    private static final String TEMPLATE_PROFILE_DIR_PROPERTY_KEY = "jod.template.profile.dir";

    private static final String DEFAULT_LINUX_OO_HOME = "/usr/lib/openoffice/";

    public OfficeManager buildOfficeManager(OOoManagerDescriptor descriptor) throws IOException {
        DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();

        // Properties configuration
        String connectionProtocol = Framework.getProperty(CONNECTION_PROTOCOL_PROPERTY_KEY);
        if (connectionProtocol != null && !"".equals(connectionProtocol)) {
            if (OfficeConnectionProtocol.PIPE.toString().equals(
                    connectionProtocol)) {
                ConfigBuilderHelper.hackClassLoader();
                configuration.setConnectionProtocol(OfficeConnectionProtocol.PIPE);
            } else if (OfficeConnectionProtocol.SOCKET.toString().equals(
                    connectionProtocol)) {
                configuration.setConnectionProtocol(OfficeConnectionProtocol.SOCKET);
            }
        }
        String maxTasksPerProcessProperty = Framework.getProperty(MAX_TASKS_PER_PROCESS_PROPERTY_KEY);
        if (maxTasksPerProcessProperty != null
                && !"".equals(maxTasksPerProcessProperty)) {
            Integer maxTasksPerProcess = Integer.valueOf(maxTasksPerProcessProperty);
            configuration.setMaxTasksPerProcess(maxTasksPerProcess);
        }
        String officeHome = Framework.getProperty(OFFICE_HOME_PROPERTY_KEY);
        if (officeHome != null && !"".equals(officeHome)) {
            configuration.setOfficeHome(officeHome);
        } else {
            if (PlatformUtils.isLinux()) {
                File officeHomeDirectory = new File(DEFAULT_LINUX_OO_HOME);
                if (officeHomeDirectory.isDirectory()) {
                    File officeExecutable = OfficeUtils.getOfficeExecutable(officeHomeDirectory);
                    if (officeExecutable.exists()) {
                        configuration.setOfficeHome(DEFAULT_LINUX_OO_HOME);
                    }
                    officeExecutable = null;
                }
                officeHomeDirectory = null;
            }
        }

        String taskExecutionTimeoutProperty = Framework.getProperty(TASK_EXECUTION_TIMEOUT_PROPERTY_KEY);
        if (taskExecutionTimeoutProperty != null
                && !"".equals(taskExecutionTimeoutProperty)) {
            Long taskExecutionTimeout = Long.valueOf(taskExecutionTimeoutProperty);
            configuration.setTaskExecutionTimeout(taskExecutionTimeout);
        }
        String taskQueueTimeoutProperty = Framework.getProperty(TASK_QUEUE_TIMEOUT_PROPERTY_KEY);
        if (taskQueueTimeoutProperty != null
                && !"".equals(taskQueueTimeoutProperty)) {
            Long taskQueueTimeout = Long.valueOf(taskQueueTimeoutProperty);
            configuration.setTaskQueueTimeout(taskQueueTimeout);
        }
        String templateProfileDir = Framework.getProperty(TEMPLATE_PROFILE_DIR_PROPERTY_KEY);
        if (templateProfileDir != null && !"".equals(templateProfileDir)) {
            File templateDirectory = new File(templateProfileDir);
            if (!templateDirectory.exists()) {
                try {
                    FileUtils.forceMkdir(templateDirectory);
                } catch (IOException e) {
                    throw new RuntimeException(
                            "I/O Error: could not create JOD templateDirectory");
                }
            }
            configuration.setTemplateProfileDir(templateDirectory);
        }

        // Descriptor configuration
        String[] pipeNames = descriptor.getPipeNames();
        if (pipeNames != null && pipeNames.length != 0) {
            configuration.setPipeNames(pipeNames);
        }
        int[] portNumbers = descriptor.getPortNumbers();
        if (portNumbers != null && portNumbers.length != 0) {
            configuration.setPortNumbers(portNumbers);
        }

        return configuration.buildOfficeManager();
    }
}
