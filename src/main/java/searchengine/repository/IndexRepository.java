package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Index findByPageAndLemma(Page page, Lemma lemma);
    Set<Index> findAllByLemma(Lemma lemma);
    List<Index> findAllByPage(Page page);
    @Query(value = "SELECT i.* FROM `index` i WHERE i.lemma_id IN :lemmas AND i.page_id IN :pages", nativeQuery = true)
    List<Index> findByPagesAndLemmas(@Param("lemmas") List<Lemma> lemmas, @Param("pages") List<Page> pages);
}
