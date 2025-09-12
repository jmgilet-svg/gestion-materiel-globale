package com.materiel.suite.client;

import com.materiel.suite.client.config.AppConfig;
import com.materiel.suite.client.net.ServiceFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigAndBackendPingTest {
    @Test
    void loadConfigAndInit() {
        AppConfig cfg = AppConfig.load();
        ServiceFactory.init(cfg);
//        assertNotNull(ServiceFactory.getConfig());
    }
}
