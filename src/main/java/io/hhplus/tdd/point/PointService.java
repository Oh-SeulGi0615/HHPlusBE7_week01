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
    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    // 포인트 조회
    public UserPoint getCurrentPointById(long userId) {
        try {
            log.info("포인트 조회 시작: userId={}", userId);
            UserPoint currentPoint = userPointTable.selectById(userId);
            log.info("포인트 조회 완료: userId={}", userId);
            return currentPoint;
        } catch (RuntimeException e) {
            log.error("포인트 조회 중 에러발생: userId={}", userId, e);
            throw new RuntimeException(e);
        }
    }

    // 포인트 충전/사용 내역 조회
    public List<PointHistory> getAllHistoryById(long userId) {
        try {
            log.info("포인트 내역 조회 시작: userId={}", userId);
            List<PointHistory> userHistory = pointHistoryTable.selectAllByUserId(userId);
            log.info("포인트 내역 조회 완료: userId={}", userId);
            return userHistory;
        } catch (RuntimeException e) {
            log.error("포인트 내역 조회 중 에러발생: userId={}", userId, e);
            throw new RuntimeException(e);
        }
    }

    /*
    포인트 충전
    1.충전 후 포인트가 최대 보유가능 포인트보다 많으면 실패
    2.충전하려는 포인트가 최소 충전가능 포인트보다 적으면 실패
     */
    public UserPoint chargeUserPoint(long userId, long amount, long chargeMillis) {
        try {
            log.info("포인트 충전 시작: userId={}, amount={}, chargeMillis={}", userId, amount, chargeMillis);
            UserPoint userPoint = userPointTable.selectById(userId);
            if (amount < 1000) {
                throw new RuntimeException("포인트 최소 충전 금액보다 적습니다");
            }
            if (userPoint.point() + amount > 10000000) {
                throw new RuntimeException("포인트 최대 보유 한도를 초과했습니다");
            }
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, chargeMillis);
            log.info("포인트 충전 내역 추가: userId={}, amount={}", userId, amount);

            UserPoint chargePoint = userPointTable.insertOrUpdate(userId, amount);
            log.info("포인트 충전 완료: userId={}, amount={}", userId, amount);
            return chargePoint;
        } catch (RuntimeException e) {
            log.error("포인트 충전 중 에러발생: userId={}, amount={}, chargeMillis={}", userId, amount, chargeMillis, e);
            throw new RuntimeException("포인트 충전 중 오류가 발생했습니다",e);
        }
    }

    /*
    포인트 사용
    1.사용하려는 포인트가 0이하면 실패
    2.현재 포인트가 사용하려는 양보다 적으면 실패
     */
    public UserPoint useUserPoint(long userId, long amount, long useMillis) {
        try {
            log.info("포인트 사용 시작: userId={}, amount={}, chargeMillis={}", userId, amount, useMillis);
            UserPoint userPoint = userPointTable.selectById(userId);
            if (amount <= 0) {
                throw new RuntimeException("사용하려는 포인트는 0보다 커야 합니다");
            }
            if (userPoint.point() < amount) {
                throw new RuntimeException("포인트가 부족합니다");
            }

            pointHistoryTable.insert(userId, amount, TransactionType.USE, useMillis);
            log.info("포인트 사용 내역 추가: userId={}, amount={}", userId, amount);

            UserPoint usePoint = userPointTable.insertOrUpdate(userId, (userPoint.point() - amount));
            log.info("포인트 사용 완료: userId={}, amount={}", userId, amount);
            return usePoint;
        } catch (RuntimeException e) {
            log.error("포인트 사용 중 에러발생: userId={}, amount={}, chargeMillis={}", userId, amount, useMillis, e);
            throw new RuntimeException(e);
        }
    }
}
