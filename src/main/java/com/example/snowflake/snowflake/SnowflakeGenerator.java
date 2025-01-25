package com.example.snowflake.snowflake;

import lombok.NoArgsConstructor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Snowflake ID 생성기
 * https://cmhyuk.github.io/infra/system-design/07-unique-id.html - snowflake 설명
 */
@Component
@NoArgsConstructor
public class SnowflakeGenerator extends SequenceStyleGenerator {

    // 비트 길이 설정
    private static final int DATACENTER_ID_BITS = 5;  // 데이터센터 ID 비트 (0~31)
    private static final int SERVER_ID_BITS = 5;      // 서버 ID 비트 (0~31)
    private static final int SEQUENCE_BITS = 12;      // 일련번호 비트 (0~4095)

    // 시퀀스의 최대 값 (4095)
    private static final long MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1;

    // 커스텀 Epoch (2015-01-01 00:00:00)
    private static final long CUSTOM_EPOCH = 1420070400000L;

    // 데이터센터와 서버 ID 설정 (예시 값, 실제 값은 환경에 따라 설정)
    private static final long DATACENTER_ID = 1; // 데이터센터 ID (0~31)
    private static final long SERVER_ID = 1;     // 서버 ID (0~31)

    // 현재 시퀀스 상태 (초기값은 0)
    private volatile long sequence = 0L;

    // 마지막 타임스탬프 기록 (초기값은 -1)
    private volatile long lastTimestamp = -1L;

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        // nextId() 메소드를 호출하여 ID를 생성하고 반환
        return nextId();
    }

    // 현재 타임스탬프 (CUSTOM_EPOCH 기준으로 밀리초 단위로 반환)
    private static long timestamp() {
        return Instant.now().toEpochMilli() - CUSTOM_EPOCH;  // 현재 시간에서 CUSTOM_EPOCH을 뺀 값
    }

    // 고유한 ID를 생성하는 메소드
    public synchronized long nextId() {
        long currentTimestamp = timestamp();  // 현재 타임스탬프를 구함

        // 시스템 시계가 뒤로 갔을 때 예외 처리 (현재 시간보다 이전 시간일 경우)
        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!");  // 시스템 시계가 잘못 설정됨
        }

        // 같은 타임스탬프일 경우 시퀀스를 증가시킴
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;  // 시퀀스를 증가시켜서 최대값을 넘지 않도록 함
            if (sequence == 0) {
                // 시퀀스가 0이면 밀리초 단위로 대기
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0; // 새로운 타임스탬프이면 시퀀스를 0으로 초기화
        }

        // 마지막 타임스탬프 갱신
        lastTimestamp = currentTimestamp;

        // 고유 ID 생성
        return generateId(currentTimestamp);
    }

    // Snowflake ID를 생성하는 메소드
    private long generateId(long currentTimestamp) {
        // SIGN_BIT는 사용하지 않지만 0으로 설정하여 최상위 비트 위치를 맞추기 위해 포함시킴
        // currentTimestamp << (DATACENTER_ID_BITS + SERVER_ID_BITS + SEQUENCE_BITS): 타임스탬프 값을 왼쪽으로 시프트하여 가장 높은 비트 위치로 이동 타임스탬프는 41비트로, 다른 값들이 그 뒤를 따르므로 가장 왼쪽에 위치
        // (DATACENTER_ID << (SERVER_ID_BITS + SEQUENCE_BITS)): 데이터센터 ID를 해당 비트 위치로 시프트하여 타임스탬프 뒤에 위치
        // (SERVER_ID << SEQUENCE_BITS): 서버 ID를 해당 비트 위치로 시프트하여 데이터센터 ID 뒤에 위치
        // sequence: 일련번호는 가장 낮은 비트에 위치하며, 시퀀스를 12비트로 관리
        return (currentTimestamp << (DATACENTER_ID_BITS + SERVER_ID_BITS + SEQUENCE_BITS)) |
                (DATACENTER_ID << (SERVER_ID_BITS + SEQUENCE_BITS)) |
                (SERVER_ID << SEQUENCE_BITS) |
                sequence;
    }

    // 타임스탬프가 달라질 때까지 대기
    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp();  // 타임스탬프를 다시 얻음
        }
        return currentTimestamp;  // 새 타임스탬프 반환
    }
}
