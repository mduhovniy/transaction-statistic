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

    // we use reasonable count to make all posts in 1 minute
    private static final int TRANSACTION_MAX_COUNT = 1000;
    // for big number of transactions we use rounding to integer representation
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.");

    @Test
    public void timestampSequentialTransactionLoading() throws InterruptedException {
        transactionProcessor.clearStatistic();
        transactionLoading(0);
    }

    @Test
    public void timestampDiscrepancyTransactionLoading() throws InterruptedException {
        transactionProcessor.clearStatistic();
        Random rnd = new Random();
        // we use random lag for every < 10 sec
        transactionLoading((long) rnd.nextInt(10_000));
    }

    private void transactionLoading(long lagInMs) throws InterruptedException {
        // first we check transactionProcessor restart
        given().port(port)
                .when().get("/statistics")
                .then().assertThat().body("count", equalTo(0));
        // than we check expired transaction filter
        Transaction expiredTransaction = new Transaction(1000, System.currentTimeMillis() - 60_000);
        given().contentType(ContentType.JSON).port(port).body(expiredTransaction)
                .when().post("/transactions")
                .then().statusCode(204);
        // than we send transaction that goes out of boundaries when bulk is running
        Transaction transactionGoesOutOfBound = new Transaction(1000, System.currentTimeMillis() - 59_000);
        given().contentType(ContentType.JSON).port(port).body(transactionGoesOutOfBound)
                .when().post("/transactions")
                .then().statusCode(201);
        // than we send bulk posts
        Random rnd = new Random();
        double sum = 0, max = 0, min = 0;
        for (int i = 0; i < TRANSACTION_MAX_COUNT; i++) {
            double amount = Math.abs((double) rnd.nextInt(100_000) / 100);
            sum += amount;
            max = amount > max ? amount : max;
            min = i == 0 ? amount : amount < min ? amount : min;
            log.info("Amount={} generated", amount);
            long timestamp = System.currentTimeMillis();
            // if we have lag than we emulate delay in timeStamp for every second transaction
            if (i % 2 == 0 && lagInMs != 0) timestamp -= lagInMs;
            Transaction transaction = new Transaction(amount, timestamp);
            Thread.sleep(10);
            given().contentType(ContentType.JSON).port(port).body(transaction)
                    .when().post("/transactions")
                    .then().statusCode(201);
        }
        double avg = sum / TRANSACTION_MAX_COUNT;
        // and finally we check the result
        log.info("sum={} avg={} max={} min={} count={}", DECIMAL_FORMAT.format(sum), DECIMAL_FORMAT.format(avg), DECIMAL_FORMAT.format(max), DECIMAL_FORMAT.format(min), TRANSACTION_MAX_COUNT);
        checkStatisticResult(new Statistic(sum, avg, max, min, TRANSACTION_MAX_COUNT));
    }

    private void checkStatisticResult(Statistic stat) {

        given().port(port)
                .when().get("/statistics")
                .then().log().body()
                .assertThat().body("count", equalTo((int) stat.getCount()));

        String json = given().port(port).get("/statistics").asString();
        double sum = from(json).getDouble("sum");
        double avg = from(json).getDouble("avg");
        double max = from(json).getDouble("max");
        double min = from(json).getDouble("min");
        assertThat(DECIMAL_FORMAT.format(stat.getSum()), equalTo(DECIMAL_FORMAT.format(sum)));
        assertThat(DECIMAL_FORMAT.format(stat.getAvg()), equalTo(DECIMAL_FORMAT.format(avg)));
        assertThat(DECIMAL_FORMAT.format(stat.getMax()), equalTo(DECIMAL_FORMAT.format(max)));
        assertThat(DECIMAL_FORMAT.format(stat.getMin()), equalTo(DECIMAL_FORMAT.format(min)));
        log.info("Divergence of SUM={}", sum - stat.getSum());
        log.info("Divergence of AVG={}", avg - stat.getAvg());
        log.info("Divergence of MAX={}", max - stat.getMax());
        log.info("Divergence of MIN={}", min - stat.getMin());
    }

}
