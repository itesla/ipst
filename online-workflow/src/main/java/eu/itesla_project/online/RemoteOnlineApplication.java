/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import eu.itesla_project.modules.online.OnlineWorkflowParameters;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Quinary <itesla@quinary.com>
 */
public class RemoteOnlineApplication implements OnlineApplication, NotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteOnlineApplication.class);

    private final List<OnlineApplicationListener> listeners = new CopyOnWriteArrayList<>();

    private JMXConnector connector;

    private MBeanServerConnection mbsc;

    private JMXServiceURL serviceURL;
    Map<String, String> jmxEnv;

    private LocalOnlineApplicationMBean application;

    private boolean connected;

    public RemoteOnlineApplication(OnlineWorkflowStartParameters conf)
            throws IOException, MalformedObjectNameException, InstanceNotFoundException {
        String host = conf.getJmxHost();
        int port = conf.getJmxPort();
        String urlString = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
        serviceURL = new JMXServiceURL(urlString);
        jmxEnv = new HashMap<>();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        ScheduledFuture scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    application.ping();
                } catch (Exception ex) {
                    try {
                        notifyDisconnection();
                        connect();
                    } catch (Throwable t) {
                    }
                }
            }
        }, 2, 10, TimeUnit.SECONDS);
    }

    private void connect() throws IOException, MalformedObjectNameException, InstanceNotFoundException {
        try {
            this.connector = JMXConnectorFactory.connect(serviceURL, jmxEnv);
            mbsc = connector.getMBeanServerConnection();

            ObjectName name = new ObjectName(LocalOnlineApplicationMBean.BEAN_NAME);
            application = MBeanServerInvocationHandler.newProxyInstance(mbsc, name, LocalOnlineApplicationMBean.class,
                    false);
            mbsc.addNotificationListener(name, this, null, null);
            connected = true;
            for (OnlineApplicationListener l : listeners) {
                l.onConnection();
            }
        } catch (Exception ex) {
            LOGGER.error("Exception connecting JMX to " + serviceURL + ": " + ex.getMessage(), ex);
        }
    }

    private void notifyDisconnection() {
        if (connected) {
            connected = false;
            for (OnlineApplicationListener l : listeners) {
                l.onDisconnection();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleNotification(Notification notification, Object handback) {
        AttributeChangeNotification notification1 = (AttributeChangeNotification) notification;
        switch (notification1.getAttributeName()) {
        case LocalOnlineApplicationMBean.BUSY_CORES_ATTRIBUTE:
            for (OnlineApplicationListener l : listeners) {
                l.onBusyCoresUpdate((int[]) notification1.getNewValue());
            }
            break;
        case LocalOnlineApplicationMBean.RUNNING_ATTRIBUTE:
            for (OnlineApplicationListener l : listeners) {
                l.onWorkflowUpdate((StatusSynthesis) notification1.getNewValue());
            }
            break;
        case LocalOnlineApplicationMBean.WCA_RUNNING_ATTRIBUTE:
            for (OnlineApplicationListener l : listeners) {
                l.onWcaUpdate((RunningSynthesis) notification1.getNewValue());
            }
            break;
        case LocalOnlineApplicationMBean.STATES_ACTIONS_ATTRIBUTE:
            for (OnlineApplicationListener l : listeners) {
                l.onStatesWithActionsUpdate((ContingencyStatesActionsSynthesis) notification1.getNewValue());
            }
            break;
        case LocalOnlineApplicationMBean.STATES_INDEXES_ATTRIBUTE:
            for (OnlineApplicationListener l : listeners) {
                l.onStatesWithIndexesUpdate((ContingencyStatesIndexesSynthesis) notification1.getNewValue());
            }
            break;
        case LocalOnlineApplicationMBean.WORK_STATES_ATTRIBUTE:
            for (OnlineApplicationListener l : listeners) {
                l.onWorkflowStateUpdate((WorkSynthesis) notification1.getNewValue());
            }
            break;
        case LocalOnlineApplicationMBean.INDEXES_SECURITY_RULES_ATTRIBUTE:

            for (OnlineApplicationListener l : listeners) {
                l.onStatesWithSecurityRulesResultsUpdate(
                        (IndexSecurityRulesResultsSynthesis) notification1.getNewValue());
            }
            break;
        case LocalOnlineApplicationMBean.WCA_CONTINGENCIES_ATTRIBUTE:
            for (OnlineApplicationListener l : listeners) {
                l.onWcaContingencies((WcaContingenciesSynthesis) notification1.getNewValue());
            }

            break;

        default:
            throw new AssertionError();
        }
    }

    @Override
    public int getAvailableCores() {
        int cores = 0;
        try {
            cores = application.getAvailableCores();
        } catch (JMRuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            notifyDisconnection();
        }
        return cores;
    }

    @Override
    public String startWorkflow(OnlineWorkflowStartParameters start, OnlineWorkflowParameters params) {
        try {
            return application.startWorkflow(start, params);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void stopWorkflow() {
        try {
            application.stopWorkflow();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            notifyDisconnection();
        }
    }

    @Override
    public void notifyListeners() {
        application.notifyListeners();
    }

    @Override
    public void close() throws Exception {
        mbsc.removeNotificationListener(new ObjectName(LocalOnlineApplicationMBean.BEAN_NAME), this);
        connector.close();
    }

    @Override
    public void addListener(OnlineApplicationListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListener(OnlineApplicationListener l) {
        listeners.remove(l);
    }

    @Override
    public String startProcess(String name, String owner, DateTime date, DateTime creationDate,
            OnlineWorkflowStartParameters start, OnlineWorkflowParameters params, DateTime[] basecases)
                    throws Exception {
        try {
            return application.startProcess(name, owner, date, creationDate, start, params, basecases);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            notifyDisconnection();
        }
        return null;
    }
}
