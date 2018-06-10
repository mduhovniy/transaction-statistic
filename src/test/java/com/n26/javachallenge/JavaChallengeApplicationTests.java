package com.n26.javachallenge;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.Transaction;
import com.n26.javachallenge.repository.TransactionRepository;
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
    private TransactionRepository transactionRepository;

    private static final DecimalFormat DF4;
    private static final DecimalFormat DF2;

    static {
        DF2 = new DecimalFormat("#.##");
        DF4 = new DecimalFormat("#.####");
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void timestampSequentialTransactionLoading() throws InterruptedException {
        transactionRepository.clearStatistic();
        transactionLoading(false);
    }

    @Test
    public void timestampDiscrepancyTransactionLoading() throws InterruptedException {
        transactionRepository.clearStatistic();
        transactionLoading(true);
    }

    private void transactionLoading(boolean isDiscrepant) throws InterruptedException {
        given().port(port)
                .when().get("/statistics")
                .then().assertThat().body("count", equalTo(0));

        Random rnd = new Random();
        double sum = 0, max = 0, min = 0;
        int count = 100;
        if (isDiscrepant) {
            Transaction transactionOutOfBound = new Transaction(1000, System.currentTimeMillis() - 60_000);
            given().contentType(ContentType.JSON).port(port).body(transactionOutOfBound)
                    .when().post("/transactions")
                    .then().statusCode(200);
        }
        for (int i = 0; i < count; i++) {
            double amount = Math.abs((double) rnd.nextInt(100_000) / 100);
            sum += amount;
            max = amount > max ? amount : max;
            min = i == 0 ? amount : amount < min ? amount : min;
            log.info("Amount={} generated", amount);
            Transaction transaction = new Transaction(amount, System.currentTimeMillis());
            Thread.sleep(100);
            given().contentType(ContentType.JSON).port(port).body(transaction)
                    .when().post("/transactions")
                    .then().statusCode(200);
        }
        double avg = sum / count;
        log.info("sum={} avg={} max={} min={} count={}", DF2.format(sum), DF4.format(avg), DF2.format(max), DF2.format(min), count);
        checkStatisticResult(new Statistic(sum, avg, max, min, count));
    }

    private void checkStatisticResult(Statistic stat) {

        given().port(port)
                .when().get("/statistics")
                .then().log().body()
                .assertThat().body("count", equalTo((int) stat.getCount()));

        String json = given().port(port).get("/statistics").asString();
        assertThat(DF2.format(stat.getSum()), equalTo(from(json).getString("sum")));
        assertThat(DF4.format(stat.getAvg()), equalTo(from(json).getString("avg")));
        assertThat(DF2.format(stat.getMax()), equalTo(from(json).getString("max")));
        assertThat(DF2.format(stat.getMin()), equalTo(from(json).getString("min")));
    }

}
