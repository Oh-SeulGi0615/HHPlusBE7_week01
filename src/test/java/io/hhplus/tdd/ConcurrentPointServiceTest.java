package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.ConcurrentPointService;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class ConcurrentPointServiceTest {
    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Autowired
    private PointService pointService;

    @Autowired
    private ConcurrentPointService concurrentPointService;

    @Test
    void 동시성테스트_단일사용자() throws InterruptedException {
        // given
        final long userId = 1L;
        final long initialPoint = 5000L;

        final long chargePoint = 2000L;
        final long usePoint = 1000L;

        concurrentPointService.chargeUserPoint(userId, initialPoint, System.currentTimeMillis());

        ExecutorService executor = Executors.newFixedThreadPool(10);
//
        // when
        for (int i = 0; i < 5; i++) {
            executor.submit(()->{concurrentPointService.chargeUserPoint(userId, chargePoint, System.currentTimeMillis());});
//            System.out.println("After Charge: " + userPointTable.selectById(userId).point());
        }
        for (int i = 0; i < 5; i++) {
            executor.submit(()->{concurrentPointService.useUserPoint(userId, usePoint, System.currentTimeMillis());});
//            System.out.println("After Use: " + userPointTable.selectById(userId).point());
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        UserPoint result = concurrentPointService.getCurrentPointById(userId);
//        System.out.println("Final Point: " + result.point());
        assertThat(result.point()).isEqualTo(10000L);
    }

    @Test
    void 동시성테스트_다중사용자() throws InterruptedException {
        // given
        final int numberOfUsers = 5;
        final long initialPoint = 5000L;
        final long chargePoint = 2000L;
        final long usePoint = 1000L;

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (long userId = 1; userId <= numberOfUsers; userId++) {
            concurrentPointService.chargeUserPoint(userId, initialPoint, System.currentTimeMillis());
        }

        // when
        for (long userId = 1; userId <= numberOfUsers; userId++) {
            final long currentUserId = userId;
            for (int i = 0; i < 5; i++) {
                executor.submit(() -> {
                    concurrentPointService.chargeUserPoint(currentUserId, chargePoint, System.currentTimeMillis());
                });
            }
            for (int i = 0; i < 5; i++) {
                executor.submit(() -> {
                    concurrentPointService.useUserPoint(currentUserId, usePoint, System.currentTimeMillis());
                });
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        for (long userId = 1; userId <= numberOfUsers; userId++) {
            UserPoint result = concurrentPointService.getCurrentPointById(userId);
            System.out.println("UserId: " + userId + ", Final Point: " + result.point());
            assertThat(result.point()).isEqualTo(10000L);
        }
    }
}
