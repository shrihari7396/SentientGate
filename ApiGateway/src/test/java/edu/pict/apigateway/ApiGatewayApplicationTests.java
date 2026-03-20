package edu.pict.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "sentinel.security.secret-key=asbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryh",
            "jwt.secret-key=asbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryhasbdfhabsdfhnjsndjnjdsncnsadncaiusdhfiuwhe7ryh"
        })
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {}
}
