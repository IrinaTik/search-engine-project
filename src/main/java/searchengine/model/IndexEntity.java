package searchengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

// Lombok
@Data
@NoArgsConstructor
@AllArgsConstructor
// Database
@Entity
@Table(name = "search_index", indexes = @Index(name = "lemma_page_idx", columnList = "lemma_id, page_id"))
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaEntity lemma;

    @Column(name = "lemma_rank", nullable = false)
    private float rank;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntity index = (IndexEntity) o;
        return Float.compare(index.rank, rank) == 0 && Objects.equals(page, index.page) && Objects.equals(lemma, index.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, lemma, rank);
    }

}
