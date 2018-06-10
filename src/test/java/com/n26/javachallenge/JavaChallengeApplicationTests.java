package com.n26.javachallenge;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.Transaction;
import com.n26.javachallenge.services.TransactionProcessor;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.DecimalFormat;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JavaChallengeApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TransactionProcessor transactionProcessor;

    private static final int TRANSACTION_MAX_COUNT = 1000;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");

    @Test
    public void timestampSequentialTransactionLoading() throws InterruptedException {
        //transactionProcessor.clearStatistic();
        transactionLoading(false);
    }

    @Test
    public void timestampDiscrepancyTransactionLoading() throws InterruptedException {
        //transactionProcessor.clearStatistic();
        transactionLoading(true);
    }

    private void transactionLoading(boolean isDiscrepant) throws InterruptedException {
        given().port(port)
                .when().get("/statistics")
                .then().assertThat().body("count", equalTo(0));

        Random rnd = new Random();
        double sum = 0, max = 0, min = 0;
        if (isDiscrepant) {
            Transaction transactionOutOfBound = new Transaction(1000, System.currentTimeMillis() - 60_000);
            given().contentType(ContentType.JSON).port(port).body(transactionOutOfBound)
                    .when().post("/transactions")
                    .then().statusCode(204);
        }
        for (int i = 0; i < TRANSACTION_MAX_COUNT; i++) {
            double amount = Math.abs((double) rnd.nextInt(100_000) / 100);
            sum += amount;
            max = amount > max ? amount : max;
            min = i == 0 ? amount : amount < min ? amount : min;
            log.info("Amount={} generated", amount);
            Transaction transaction = new Transaction(amount, System.currentTimeMillis());
            Thread.sleep(10);
            given().contentType(ContentType.JSON).port(port).body(transaction)
                    .when().post("/transactions")
                    .then().statusCode(201);
        }
        double avg = sum / TRANSACTION_MAX_COUNT;
        log.info("sum={} avg={} max={} min={} count={}", DECIMAL_FORMAT.format(sum), DECIMAL_FORMAT.format(avg), DECIMAL_FORMAT.format(max), DECIMAL_FORMAT.format(min), TRANSACTION_MAX_COUNT);
        checkStatisticResult(new Statistic(sum, avg, max, min, TRANSACTION_MAX_COUNT));
    }

    private void checkStatisticResult(Statistic stat) {

        given().port(port)
                .when().get("/statistics")
                .then().log().body()
                .assertThat().body("count", equalTo((int) stat.getCount()));

        String json = given().port(port).get("/statistics").asString();
        assertThat(DECIMAL_FORMAT.format(stat.getSum()), equalTo(DECIMAL_FORMAT.format(from(json).getDouble("sum"))));
        assertThat(DECIMAL_FORMAT.format(stat.getAvg()), equalTo(DECIMAL_FORMAT.format(from(json).getDouble("avg"))));
        assertThat(DECIMAL_FORMAT.format(stat.getMax()), equalTo(DECIMAL_FORMAT.format(from(json).getDouble("max"))));
        assertThat(DECIMAL_FORMAT.format(stat.getMin()), equalTo(DECIMAL_FORMAT.format(from(json).getDouble("min"))));
    }

}
