package com.custom.trader.watchlist.repository;

import com.custom.trader.watchlist.entity.WatchlistGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroup, Long> {

    List<WatchlistGroup> findByAccountNumber(String accountNumber);

    Optional<WatchlistGroup> findByAccountNumberAndGroupCode(String accountNumber, String groupCode);
}
