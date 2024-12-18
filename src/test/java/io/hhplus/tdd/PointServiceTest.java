package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointServiceTest {
    private final UserPointTable userPointTable = mock(UserPointTable.class);
    private final PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);
    final PointService pointService = new PointService(userPointTable, pointHistoryTable);

    @Test
    void userid로_포인트조회_테스트() {
        // given
        final long userId = 1L;
        final long chargedPoint = 15000L;
        final long chargeMillis = System.currentTimeMillis();
        final UserPoint userPoint = new UserPoint(userId, chargedPoint, chargeMillis);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // when
        UserPoint result = pointService.getCurrentPointById(userId);

        // then
        assertThat(result.point()).isEqualTo(chargedPoint);
    }

    @Test
    void userid로_포인트내역조회_테스트() {
        // given
        final long userId = 1L;
        final long chargePoint = 10000L;
        final long usePoint = 3000L;
        final List<PointHistory> pointHistories = List.of(
                new PointHistory(1L, userId, chargePoint, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, usePoint, TransactionType.USE, System.currentTimeMillis())
        );

        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistories);

        // when
        List<PointHistory> result = pointService.getAllHistoryById(userId);

        // then
        assertThat(result).isEqualTo(pointHistories);
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void 포인트충전테스트_1_정상케이스() {
        // given
        final long userId = 1L;
        final long chargePoint = 15000L;
        final long chargeMillis = System.currentTimeMillis();
        final UserPoint userPoint = new UserPoint(userId, chargePoint, chargeMillis);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, chargePoint)).thenReturn(userPoint);

        // when
        UserPoint result = pointService.chargeUserPoint(userId, chargePoint, chargeMillis);

        // then
        assertThat(result.point()).isEqualTo(chargePoint);
    }

    @Test
    void 포인트충전테스트_2_최소충전금액미만() {
        // given
        final long userId = 1L;
        final long chargePoint = 900L;
        final long chargeMillis = System.currentTimeMillis();
        final UserPoint userPoint = new UserPoint(userId, chargePoint, chargeMillis);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, chargePoint)).thenReturn(userPoint);

        // when + then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.chargeUserPoint(userId, chargePoint, chargeMillis);
                }
        );

        // 검증
        assertThat(exception.getMessage()).isEqualTo("포인트 최소 충전 금액보다 적습니다");
    }

    @Test
    void 포인트충전테스트_3_최대충전금액초과() {
        // given
        final long userId = 1L;
        final long chargePoint = 15_000_000L;
        final long chargeMillis = System.currentTimeMillis();
        final UserPoint userPoint = new UserPoint(userId, chargePoint, chargeMillis);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, chargePoint)).thenReturn(userPoint);

        // when + then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.chargeUserPoint(userId, chargePoint, chargeMillis);
                }
        );

        // 검증
        assertThat(exception.getMessage()).isEqualTo("포인트 최대 보유 한도를 초과했습니다");
    }

    @Test
    void 포인트충전테스트_4_합산포인트한도초과() {
        // given
        final long userId = 1L;
        final long chargingPoint = 1_500_000L;
        final long chargedPoint = 9_000_000L;

        final long chargeMillis = System.currentTimeMillis();
        final UserPoint userPoint = new UserPoint(userId, chargedPoint, chargeMillis);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, chargingPoint)).thenReturn(userPoint);

        // when + then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.chargeUserPoint(userId, chargingPoint, chargeMillis);
                }
        );

        // 검증
        assertThat(exception.getMessage()).isEqualTo("포인트 최대 보유 한도를 초과했습니다");
    }

    @Test
    void 포인트사용테스트_1_정상케이스() {
        // given
        final long userId = 1L;
        final long chargedPoint = 10000L;
        final long usePoint = 3000L;

        final long useMillis = System.currentTimeMillis();
        final UserPoint userPoint = new UserPoint(userId, chargedPoint, System.currentTimeMillis());
        final UserPoint afterPoint = new UserPoint(userId, (chargedPoint - usePoint), useMillis);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, (userPoint.point() - usePoint))).thenReturn(afterPoint);

        // when
        UserPoint result = pointService.useUserPoint(userId, usePoint, useMillis);

        // then
        assertThat(result.point()).isEqualTo(chargedPoint - usePoint);
    }

    @Test
    void 포인트사용테스트_2_음수포인트사용요청() {
        // given
        final long userId = 1L;
        final long chargedPoint = 10000L;
        final long usePoint = -1000L;

        final long useMillis = System.currentTimeMillis();
        final UserPoint userPoint = new UserPoint(userId, chargedPoint, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // when
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.useUserPoint(userId, usePoint, useMillis);
                }
        );

        // then
        assertThat(exception.getMessage()).isEqualTo("사용하려는 포인트는 0보다 커야 합니다");
    }

    @Test
    void 포인트사용테스트_3_포인트부족() {
        // given
        final long userId = 1L;
        final long chargedPoint = 5000L;
        final long usePoint = 10000L;

        final long useMillis = System.currentTimeMillis();
        final UserPoint userPoint = new UserPoint(userId, chargedPoint, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // when + then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.useUserPoint(userId, usePoint, useMillis);
                }
        );

        // 검증
        assertThat(exception.getMessage()).isEqualTo("포인트가 부족합니다");
    }
}
