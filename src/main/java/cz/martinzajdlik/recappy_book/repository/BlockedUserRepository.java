package cz.martinzajdlik.recappy_book.repository;

import cz.martinzajdlik.recappy_book.dto.BlockedUserResponse;
import cz.martinzajdlik.recappy_book.model.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    void deleteByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    // Mazání uživatele musí uklidit obě strany vazby – ať už mazaný uživatel
    // někoho blokoval, nebo byl zablokovaný jinými.
    void deleteByBlocker_Id(Long userId);

    void deleteByBlocked_Id(Long userId);

    @Query("SELECT b.blocked.id FROM BlockedUser b WHERE b.blocker.id = :blockerId")
    List<Long> findBlockedIdsByBlocker(@Param("blockerId") Long blockerId);

    @Query("SELECT new cz.martinzajdlik.recappy_book.dto.BlockedUserResponse(b.blocked.id, b.blocked.username) " +
            "FROM BlockedUser b WHERE b.blocker.id = :blockerId")
    List<BlockedUserResponse> findBlockedUsersByBlocker(@Param("blockerId") Long blockerId);
}
