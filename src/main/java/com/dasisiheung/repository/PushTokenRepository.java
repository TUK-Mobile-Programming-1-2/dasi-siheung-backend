package com.dasisiheung.repository;

import com.dasisiheung.entity.member.PushToken;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    @Query("select t.token from PushToken t where t.member.id = :memberId order by t.id")
    List<String> findTokensByMemberId(@Param("memberId") Long memberId);

    /** @return 지운 수 (0 이면 이 회원의 토큰이 아니었거나 이미 없음) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PushToken t where t.member.id = :memberId and t.token = :token")
    int deleteByMemberIdAndToken(@Param("memberId") Long memberId, @Param("token") String token);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PushToken t where t.member.id = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PushToken t where t.token in :tokens")
    int deleteAllByTokenIn(@Param("tokens") Collection<String> tokens);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PushToken t where t.lastRegisteredAt < :before")
    int deleteAllRegisteredBefore(@Param("before") LocalDateTime before);
}
