package com.dasisiheung.repository;

import com.dasisiheung.entity.faq.Faq;
import com.dasisiheung.entity.faq.FaqCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    /** 보여주는 순서 (순서가 같으면 먼저 만든 것부터). */
    List<Faq> findAllByOrderByDisplayOrderAscIdAsc();

    List<Faq> findByCategoryOrderByDisplayOrderAscIdAsc(FaqCategory category);

    /** @return 맨 뒤 순서. 질문이 없으면 -1 (새 질문이 0 이 되도록) */
    @Query("select coalesce(max(f.displayOrder), -1) from Faq f")
    int findMaxDisplayOrder();
}
