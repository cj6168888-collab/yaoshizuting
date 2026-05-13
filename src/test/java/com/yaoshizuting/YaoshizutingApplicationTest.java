package com.yaoshizuting;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class YaoshizutingApplicationTest {

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            YaoshizutingApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(YaoshizutingApplication.class, args));
        }
    }
}
