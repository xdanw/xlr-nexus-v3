package com.xebialabs.xlrelease.plugin.nexus;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xebialabs.deployit.plumbing.scheduler.Scheduler;
import com.xebialabs.xlrelease.actors.ReleaseActorService;
import com.xebialabs.xlrelease.config.XlrConfig;
import com.xebialabs.xlrelease.repository.TaskRepository;
import com.xebialabs.xlrelease.script.ScriptLifeCycle;
import com.xebialabs.xlrelease.script.ScriptPermissionsProvider;
import com.xebialabs.xlrelease.script.jython.JythonEngineInstance;
import com.xebialabs.xlrelease.script.jython.JythonScriptExecutor;
import com.xebialabs.xlrelease.script.jython.JythonScriptService;
import com.xebialabs.xlrelease.security.PermissionChecker;
import com.xebialabs.xlrelease.security.authentication.AuthenticationService;
import com.xebialabs.xlrelease.service.ReleaseService;
import com.xebialabs.xlrelease.service.VariableService;

public abstract class TriggerTest {
    @Mock
    private Scheduler scheduler;
    @Mock
    private ScriptLifeCycle scriptLifeCycle;

    private XlrConfig xlrConfig = XlrConfig.bootConfig("unit-tests.conf");

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private ReleaseActorService releaseActorService;
    @Mock
    private VariableService variableService;
    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private ReleaseService releaseService;

    @Mock
    private TaskRepository taskRepository;

    JythonScriptService jythonScriptService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        final JythonScriptExecutor jythonScriptExecutor = new JythonScriptExecutor(new JythonEngineInstance(), new ScriptPermissionsProvider(), xlrConfig);
        jythonScriptService = new JythonScriptService(scheduler, releaseActorService, scriptLifeCycle, jythonScriptExecutor,
                authenticationService, releaseService,
                variableService, permissionChecker, null, null, null, null, null, taskRepository, null);
    }
}
