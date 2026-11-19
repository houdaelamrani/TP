package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayProfilesTests {
    @Test
    void simultaneousRoutingProfilesAreRejected() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("static", "discovery");
        assertThatThrownBy(() -> new ApiGatewayApplication().exclusiveRoutingProfiles(environment))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("un seul profil");
    }
}
