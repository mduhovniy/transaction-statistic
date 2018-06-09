package com.n26.javachallenge;

import com.n26.javachallenge.dto.Transaction;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Random;

import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;
import static org.hamcrest.CoreMatchers.equalTo;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JavaChallengeApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    public void contextLoads() {
    }

    @Test
    public void transactionLoading() {
        given().port(port)
                .when().get("/statistics")
                .then().assertThat().body("count", equalTo(0));

        Random rnd = new Random();
        for(int i=0; i<10; i++) {
            Transaction transaction = new Transaction(Math.abs(rnd.nextInt() + rnd.nextInt(99)/100), System.currentTimeMillis());
            given().contentType(ContentType.JSON).port(port).body(transaction)
                    .when().post("/transactions")
                    .then().statusCode(200);
        }

        given().port(port)
                .config(RestAssured.config().jsonConfig(jsonConfig().numberReturnType(BIG_DECIMAL)))
                .when().get("/statistics")
                .then().assertThat().body("count", equalTo(10))
                .log().body();
    }

}
