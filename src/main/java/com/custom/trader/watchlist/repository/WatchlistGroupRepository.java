package com.custom.trader.watchlist.repository;

import com.custom.trader.watchlist.entity.WatchlistGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 관심종목 그룹 저장소 (Repository).
 *
 * <p><b>책임:</b> WatchlistGroup 엔티티의 데이터 접근 계층 (Data Access Layer)</p>
 *
 * <p><b>Cascade 삭제 정책:</b></p>
 * <ul>
 *   <li>그룹 삭제 시 포함된 모든 종목(WatchlistStock)도 자동 삭제</li>
 *   <li>설정: WatchlistGroup.watchlistStocks @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)</li>
 * </ul>
 *
 * @see WatchlistGroup 관심종목 그룹 엔티티
 */
public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroup, Long> {

    /**
     * 사용자의 모든 관심종목 그룹 조회.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 그룹 목록 (없으면 빈 리스트)
     */
    List<WatchlistGroup> findByUserId(String userId);

    /**
     * 사용자의 특정 그룹 조회.
     *
     * @param userId 사용자 ID
     * @param groupCode 그룹 코드
     * @return 해당 그룹 (없으면 Optional.empty())
     */
    Optional<WatchlistGroup> findByUserIdAndGroupCode(String userId, String groupCode);

    /**
     * 사용자의 여러 그룹 조회 (IN 조건).
     *
     * @param userId 사용자 ID
     * @param groupCodes 조회할 그룹 코드 리스트
     * @return 해당하는 그룹 목록
     */
    List<WatchlistGroup> findByUserIdAndGroupCodeIn(String userId, List<String> groupCodes);

    /**
     * 사용자의 모든 그룹 삭제.
     *
     * <p>주의: 포함된 모든 WatchlistStock도 함께 삭제됨 (Cascade)</p>
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(String userId);

    /**
     * 사용자의 그룹 중 지정된 코드 목록에 포함되지 않는 그룹 삭제.
     *
     * <p><b>사용 사례:</b> 3-way 동기화에서 API에 없는 그룹만 삭제</p>
     * <p>예: API에 groupCode=[A, B, C]만 있고 DB에 [A, B, C, D]가 있으면, D만 삭제</p>
     * <p>주의: 포함된 모든 WatchlistStock도 함께 삭제됨 (Cascade)</p>
     *
     * @param userId 사용자 ID
     * @param groupCodes 유지할 그룹 코드 리스트
     */
    void deleteByUserIdAndGroupCodeNotIn(String userId, List<String> groupCodes);
}
