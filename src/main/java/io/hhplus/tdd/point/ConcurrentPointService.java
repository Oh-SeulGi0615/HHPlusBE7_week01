package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ConcurrentPointService {
    private final PointService pointService;
    private final ConcurrentHashMap<Long, ReentrantLock> userLock = new ConcurrentHashMap<>();

    private final ReentrantLock getUserLock(long userId) {
        return userLock.computeIfAbsent(userId, id -> new ReentrantLock(true));
    }

    // 포인트 조회
    public UserPoint getCurrentPointById(long userId) {
        ReentrantLock lock = getUserLock(userId);
        lock.lock();
        try {
            return pointService.getCurrentPointById(userId);
        } finally {
            lock.unlock();
        }
    }

    // 포인트 내역 조회
    public List<PointHistory> getAllHistoryById(long userId) {
        ReentrantLock lock = getUserLock(userId);
        lock.lock();
        try {
            return pointService.getAllHistoryById(userId);
        } finally {
            lock.unlock();
        }
    }

    // 포인트 충전
    public UserPoint chargeUserPoint(long userId, long amount, long chargeMillis) {
        ReentrantLock lock = getUserLock(userId);
        lock.lock();
        try {
            System.out.println("Charge, UserId: " + userId + ", Amount: " + amount + ", Time: " + chargeMillis);
            return pointService.chargeUserPoint(userId, amount, chargeMillis);
        } finally {
            lock.unlock();
        }
    }

    // 포인트 사용
    public UserPoint useUserPoint(long userId, long amount, long useMillis) {
        ReentrantLock lock = getUserLock(userId);
        lock.lock();
        try {
            System.out.println("Use, UserId: " + userId + ", Amount: " + amount + ", Time: " + useMillis);
            return pointService.useUserPoint(userId, amount, useMillis);
        } finally {
            lock.unlock();
        }
    }
}
