package com.finrisk.api;

import com.finrisk.support.IntegrationTestSupport;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiIT {

    static {
        IntegrationTestSupport.configureTestDatabase();
    }

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void create_and_get_user() {
        String email = "api-" + System.nanoTime() + "@example.com";
        int id =
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body("{\"fullName\":\"API User\",\"email\":\"" + email + "\"}")
                        .post("/api/v1/users")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        RestAssured.given()
                .get("/api/v1/users/" + id)
                .then()
                .statusCode(200)
                .body("email", equalTo(email));
    }
}
