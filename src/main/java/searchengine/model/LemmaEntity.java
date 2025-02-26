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
@Table(name = "lemma", indexes = @Index(name = "lemma_site_idx", columnList = "lemma, site_id", unique = true))
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4")
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private SiteEntity site;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaEntity lemma1 = (LemmaEntity) o;
        return Objects.equals(lemma, lemma1.lemma) && Objects.equals(site, lemma1.site);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma, site);
    }
}
