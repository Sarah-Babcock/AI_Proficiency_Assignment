package com.example.urlshortener.visit;

import com.example.urlshortener.link.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    /** All visits to the link with this short code, with the link loaded too. */
    @Query("select v from Visit v join fetch v.shortLink s where s.code = :code")
    List<Visit> findByCode(String code);

    /** Visits to the link with this short code, newest first. */
    @Query("select v from Visit v join v.shortLink s where s.code = :code order by v.visitedAt desc, v.id desc")
    List<Visit> findByCodeNewestFirst(String code);

    /**
     * Every link paired with its visit count, most visited first.
     * A left join keeps links with no visits, so they show a count of 0.
     */
    @Query("""
            select l, count(v)
            from ShortLink l left join Visit v on v.shortLink = l
            group by l
            order by count(v) desc, l.createdAt desc
            """)
    List<Object[]> countVisitsPerLink();

    /** Every link with its URLs and visit count, most visited first. */
    default List<LinkSummary> summarizeAllLinks() {
        return countVisitsPerLink().stream()
                .map(row -> {
                    ShortLink link = (ShortLink) row[0];
                    return new LinkSummary(link.getCode(), link.getLongUrls(), (Long) row[1]);
                })
                .toList();
    }
}
