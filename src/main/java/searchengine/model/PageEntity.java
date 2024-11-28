package searchengine.model;

import lombok.*;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.Set;

// Lombok
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
// Database
@Entity
@Table(name = "page", indexes = @Index(name = "path_idx", columnList = "path"))
public class PageEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4")
    private String relativePath;

    @Column(nullable = false)
    private Integer code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    @Transient
    private Set<String> childLinks;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageEntity page = (PageEntity) o;
        return Objects.equals(relativePath, page.relativePath) && Objects.equals(site, page.site);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath, site);
    }

}
