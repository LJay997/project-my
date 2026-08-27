package com.qq.ijay997.config;

import org.drools.io.ClassPathResource;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {
    private static final  KieServices kieServices = KieServices.Factory.get();
    //制定规则文件的路径
    private static final String RULES_CUSTOMER_RULES_DRL = "rules/orderScore.drl";

    @Bean
    public KieContainer kieContainer(){
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(new ClassPathResource(RULES_CUSTOMER_RULES_DRL));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());

    }
}