package searchengine.services.parsing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.services.lemmatizator.LemmaFinder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import static java.lang.Thread.sleep;

@RequiredArgsConstructor
public class SiteParser extends RecursiveAction {
    private String url;
    private final Site site;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final List<SiteParser> taskList = new ArrayList<>();

    public SiteParser(Site site, IndexingServiceImpl indexingService) {
        this.site = site;
        pageRepository = indexingService.getPageRepository();
        siteRepository = indexingService.getSiteRepository();
        lemmaRepository = indexingService.getLemmaRepository();
        indexRepository = indexingService.getIndexRepository();
    }

    public SiteParser(String url, SiteParser siteParser) {
        this.url = url;
        site = siteParser.site;
        pageRepository = siteParser.pageRepository;
        siteRepository = siteParser.siteRepository;
        lemmaRepository = siteParser.lemmaRepository;
        indexRepository = siteParser.indexRepository;
    }

    @Override
    protected void compute() {
        String currentUrl = site.getUrl();
        currentUrl = url == null ? currentUrl : currentUrl + url;
        try {
            sleep(150);
            Document document = Jsoup.connect(currentUrl).userAgent("LuckySearchBot (Windows; U; WindowsNT" +
                    " 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com")
                    .ignoreContentType(true).ignoreHttpErrors(true).get();
            if (url != null) addPage(document, url);
            Elements urls = document.select("a");
            for (Element url : urls) {
                String child = url.absUrl("href").replaceAll("\\?.+", "");
                if (isValid(child) & pageRepository.findByPath(correctUrl(child)) == null) {
                    SiteParser parser = new SiteParser(correctUrl(child), this);
                    parser.fork();
                    taskList.add(parser);
                }
            }

            for (SiteParser task : taskList) {
                task.join();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Site newSite = siteRepository.findSiteByUrl(site.getUrl());
            newSite.setStatus(Status.FAILED);
            newSite.setLastError("Ошибка индексации сайта " + newSite.getUrl());
            siteRepository.save(newSite);
        }

    }

    private synchronized void addPage(Document document, String child) throws IOException {
        if (child.equals("")) return;
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        Page page = new Page();
        page.setSite(site);
        page.setCode(document.connection().response().statusCode());
        page.setPath(child.replace(site.getUrl(), ""));
        page.setContent(document.html());
        pageRepository.save(page);
        if (page.getCode() < 400) addLemmaAndIndex(page, document);
    }

    private synchronized void addLemmaAndIndex(Page page, Document document) throws IOException {
        Map<String, Integer> lemmas = LemmaFinder.getInstance().collectLemmas(document.text());
        lemmas.forEach((word, rank) -> {
            Lemma lemma = lemmaRepository.findBySiteAndLemma(site, word);
            if (!(lemma ==null)) {
                lemma.setFrequency(lemma.getFrequency() + 1);
            } else {
                lemma = new Lemma();
                lemma.setSite(site);
                lemma.setLemma(word);
                lemma.setFrequency(1);
            }
            lemmaRepository.save(lemma);
            Index index = indexRepository.findByPageAndLemma(page, lemma);
            if (index == null) {
                index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(rank);
                indexRepository.save(index);
            }
        });
    }

    private String correctUrl(String url) {
        url = url.replace(site.getUrl(), "").replace("//", "/");
        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return url;
    }

    private boolean isValid(String url) {
        return (!url.isEmpty() && url.startsWith(site.getUrl()) && !url.contains("#")
                && !url.matches("(\\S+(\\.(?i)(jpg|pdf|png|gif|bmp))$)"));
    }
}
