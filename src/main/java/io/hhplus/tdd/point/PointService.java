package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // 포인트 조회
    public UserPoint getCurrentPointById(long userId) {
        UserPoint currentPoint = userPointTable.selectById(userId);
        return currentPoint;
    }

    // 포인트 충전/사용 내역 조회
    public List<PointHistory> getAllHistoryById(long userId) {
        List<PointHistory> userHistory = pointHistoryTable.selectAllByUserId(userId);
        return userHistory;
    }

    /*
    포인트 충전
    1.충전 후 포인트가 최대 보유가능 포인트보다 많으면 실패
    2.충전하려는 포인트가 최소 충전가능 포인트보다 적으면 실패
     */
    public UserPoint chargeUserPoint(long userId, long amount, long chargeMillis) {
        UserPoint userPoint = userPointTable.selectById(userId);

        if (userPoint == null) {
            pointHistoryTable.insert(userId, 0, TransactionType.CHARGE, chargeMillis);
        }

        if (amount < 1000) {
            throw new RuntimeException("포인트 최소 충전 금액보다 적습니다");
        }
        if (userPoint.point() + amount > 10_000_000) {
            throw new RuntimeException("포인트 최대 보유 한도를 초과했습니다");
        }

        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, chargeMillis);
        UserPoint chargePoint = userPointTable.insertOrUpdate(userId, (userPoint.point() + amount));
        return chargePoint;
    }

    /*
    포인트 사용
    1.사용하려는 포인트가 0이하면 실패
    2.현재 포인트가 사용하려는 양보다 적으면 실패
     */
    public UserPoint useUserPoint(long userId, long amount, long useMillis) {
        UserPoint userPoint = userPointTable.selectById(userId);

        if (amount <= 0) {
            throw new RuntimeException("사용하려는 포인트는 0보다 커야 합니다");
        }
        if (userPoint.point() < amount) {
            throw new RuntimeException("포인트가 부족합니다");
        }

        pointHistoryTable.insert(userId, amount, TransactionType.USE, useMillis);
        UserPoint usePoint = userPointTable.insertOrUpdate(userId, (userPoint.point() - amount));
        return usePoint;
    }
}
