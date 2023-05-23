package searchengine.services.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.lemmatizator.LemmaFinder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Getter
@Setter
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private LemmaRepository lemmaRepository;

    private LemmaFinder lemmaFinder;
    {
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchResponse search(String query, String urlSite, int offset, int limit)  {
        List<SearchData> searchData = new ArrayList<>();
        if (urlSite.isEmpty()) {
            for (Site site : siteRepository.findAll()) {
                searchData.addAll(oneSiteSearch(query, site.getUrl(), offset, limit));
            }
        } else searchData.addAll(oneSiteSearch(query, urlSite, offset, limit));
        int count = searchData.size();
        if (searchData.size() > limit) searchData.subList(0, limit);
        searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
        return new SearchResponse(true, count, searchData);
    }

    private List<SearchData> oneSiteSearch (String query, String urlSite, int offset, int limit){
        List<String> searchRequest = (lemmaFinder.getLemmaSet(query)).stream().toList();
        Site site = siteRepository.findSiteByUrl(urlSite);
        List<Lemma> lemmaRequest = new ArrayList<>(lemmaRepository.findLemmaListBySite(searchRequest, site));
        List<Lemma> sortedLemmas = getAverageFrequency(lemmaRequest);
        List<SearchData> searchData;
        searchData = getSearchResponse(sortedLemmas, searchRequest, offset);
        return searchData.subList(0, limit);
    }

    private Set<Page> getPages (List<Lemma> lemmas){
        Set<Index> indexSet = indexRepository.findAllByLemma(lemmas.get(0));
        Set<Page> pages = new HashSet<>();
        for (Index index : indexSet) {
            pages.add(index.getPage());
        }
        for (int i = 1; i < lemmas.size() -1; i++){
            indexSet.clear();
            indexSet.addAll(indexRepository.findAllByLemma(lemmas.get(i)));
            Set<Page> currentPages = new HashSet<>();
            for (Index index : indexSet) currentPages.add(index.getPage());
            pages.retainAll(currentPages);
        }

        return pages;
    }
    private List<Lemma> getAverageFrequency(List<Lemma> lemmas){
        int totalFrequency = 0;
        for (Lemma lemma : lemmas){
            totalFrequency += lemma.getFrequency();
        }
        int averageFrequency = totalFrequency / lemmas.size();
        List<Lemma> lemmaList = new ArrayList<>();
        for (Lemma lemma : lemmas){
            if (lemma.getFrequency() < averageFrequency) lemmaList.add(lemma);
        }
        lemmaList.sort((o1, o2) -> Float.compare(o1.getFrequency(), o2.getFrequency()));
        return lemmaList;
    }

    private List<SearchData> getSearchResponse(List<Lemma> lemmaList, List<String> wordList, int offset) {
        List<Page> pages = getPages(lemmaList).stream().toList();
        List<Index> indexes = indexRepository.findByPagesAndLemmas(lemmaList, pages);
        HashMap<Page, Float> pageRelevance = getRelevance(pages, indexes);
        List<SearchData> searchData = getSearchData(pageRelevance, wordList);
        if (searchData.size() < offset) return new ArrayList<>();
        else return searchData.subList(offset, searchData.size() - 1);
    }

    private HashMap<Page, Float> getRelevance(List<Page> pageList, List<Index> indexList){
        HashMap<Page, Float> relevance = new HashMap<>();
        for (Page page : pageList){
            float rel = 0;
            for (Index index : indexList){
                if (index.getPage() == page) rel += index.getRank();
            }
            relevance.put(page, rel);
        }
        HashMap<Page, Float> absRelevance = new HashMap<>();
        for (Page page : relevance.keySet()){
            float absRel = relevance.get(page) / Collections.max(relevance.values());
            absRelevance.put(page, absRel);
        }
        return absRelevance.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, HashMap::new));
    }

    private List<SearchData> getSearchData(HashMap<Page, Float> pageRelevance, List<String> wordList) {
        List<SearchData> searchData = new ArrayList<>();
        for (Page page : pageRelevance.keySet()){
            String site = page.getSite().getUrl();
            String siteName = page.getSite().getName();
            String uri = page.getPath();
            String title = getContent(page.getContent(), "title");
            String body = getContent(page.getContent(), "body");
            String snippet = getSnippet(body, wordList);
            Float relevance = pageRelevance.get(page);
            searchData.add(new SearchData(site, siteName, uri, title, snippet, relevance));
        }
        return searchData;
    }

    private String getSnippet(String text, List<String> words) {
        List<Integer> lemmaIndex = new ArrayList<>();
        for (String word : words){
            lemmaIndex.addAll(lemmaFinder.getLemmaIndex(text, word));
        }
        Collections.sort(lemmaIndex);
        StringBuilder result = new StringBuilder();
        List<String> wordsList = getWordsFromContent(text, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String text, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = text.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < lemmaIndex.size() && lemmaIndex.get(nextPoint) - end > 0 && lemmaIndex.get(nextPoint) - end < 5) {
                end = text.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint += 1;
            }
            i = nextPoint - 1;
            String words = getWordsFromIndex(start, end, text);
            result.add(words);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String text) {
        String word = text.substring(start, end);
        int first;
        int last;
        if (text.lastIndexOf(" ", start) != -1) {
            first = text.lastIndexOf(" ", start);
        } else first = start;
        if (text.indexOf(" ", end + 30) != -1) {
            last = text.indexOf(" ", end + 30);
        } else last = text.indexOf(" ", end);
        String words = text.substring(first, last);
        words = words.replaceAll(word, "<b>" + word + "</b>");
        return words;
    }

    private String getContent (String text, String content){
        StringBuilder result = new StringBuilder();
        Document document = Jsoup.parse(text);
        Elements elements = document.select(content);
        for (Element element : elements) result.append(element.html());
        return Jsoup.parse(result.toString()).text();
    }

}