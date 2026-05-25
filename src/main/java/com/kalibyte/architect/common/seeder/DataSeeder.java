package com.kalibyte.architect.common.seeder;

import com.kalibyte.architect.auth.entity.enums.RoleName;
import com.kalibyte.architect.auth.entity.Role;
import com.kalibyte.architect.auth.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("seed")// Only runs in dev profile
@org.springframework.core.annotation.Order(1)
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;


    @Override
    @Transactional
    public void run(String... args) {
        log.info("===== STARTING DATA SEEDING =====");

        seedRoles();

        log.info("===== DATA SEEDING COMPLETE =====");
    }
    private void seedRoles() {
        log.info("Seeding roles...");
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(roleName);
                        role.setDescription(roleName.name() + " Role");
                        return roleRepository.save(role);
                    });
        }
        log.info("Roles seeded.");
    }

}
