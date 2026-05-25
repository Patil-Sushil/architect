package com.kalibyte.architect.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;

/**
 * Explicit Flyway configuration to ensure migrations run reliably on startup.
 * Enforces execution before Hibernate/JPA schema validation to prevent startup crashes.
 */
@Configuration
@Slf4j
public class FlywayConfig implements BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Value("${spring.flyway.enabled:true}")
    private boolean enabled;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    /**
     * Programmatic Flyway bean. The execution of migrate() inside the bean definition
     * ensures schema is updated during bean instantiation.
     */
    @Bean(name = "flyway")
    public Flyway flyway(DataSource dataSource) {
        log.info("Configuring programmatic Flyway bean...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(baselineOnMigrate)
                .locations(locations.split(","))
                .load();

        if (enabled) {
            log.info("Starting explicit Flyway database migration...");
            flyway.migrate();
            log.info("Explicit Flyway database migration completed successfully.");
        } else {
            log.info("Flyway migration is disabled via configuration.");
        }

        return flyway;
    }

    /**
     * BeanPostProcessor that ensures EntityManagerFactory depends on Flyway.
     * This forces Flyway to initialize and run migrate() before Hibernate 
     * attempts to validate the schema.
     */
    @Bean
    public BeanPostProcessor entityManagerFactoryDependencyPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) {
                if (bean instanceof EntityManagerFactory || bean instanceof AbstractEntityManagerFactoryBean) {
                    // Force initialization of the flyway bean before the EntityManagerFactory
                    beanFactory.getBean(Flyway.class);
                    log.info("Ensured Flyway migration completed before initializing: {}", beanName);
                }
                return bean;
            }
        };
    }
}
