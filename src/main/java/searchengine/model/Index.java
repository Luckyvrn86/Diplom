package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "`index`", uniqueConstraints = @UniqueConstraint(columnNames = {"page_id", "lemma_id"}))
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional=false)
    @JoinColumn(name = "page_id", nullable = false, columnDefinition = "INT")
    private Page page;

    @ManyToOne(optional=false)
    @JoinColumn(name = "lemma_id", nullable = false, columnDefinition = "INT")
    private Lemma lemma;

    @Column(name = "`rank`", nullable = false, columnDefinition = "FLOAT")
    private float rank;

}