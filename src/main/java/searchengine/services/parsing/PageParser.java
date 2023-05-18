package searchengine.services.parsing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.lemmatizator.LemmaFinder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;
import static java.lang.Thread.sleep;

public class PageParser extends RecursiveAction {
    private final Site site;
    private final SiteParser siteParser;
    private final SiteParser parentUrl;
    private final List<String> alreadyVisited;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static final CopyOnWriteArraySet<String> urlSet = new CopyOnWriteArraySet<>();

    public PageParser(SiteParser siteParser, SiteParser parentUrl, Site site) {
        this.siteParser = siteParser;
        this.parentUrl = parentUrl;
        siteRepository = siteParser.getIndexingService().getSiteRepository();
        pageRepository = siteParser.getIndexingService().getPageRepository();
        lemmaRepository = siteParser.getIndexingService().getLemmaRepository();
        indexRepository = siteParser.getIndexingService().getIndexRepository();
        this.site = site;
        alreadyVisited = new ArrayList<>();
    }



    @Override
    protected void compute() {
        Set<PageParser> taskSet = new HashSet<>();
        try {
            sleep(150);
            Document document = Jsoup.connect(siteParser.getUrl()).userAgent("LuckySearchBot (Windows; U; WindowsNT" +
                    " 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com")
                    .ignoreContentType(true).ignoreHttpErrors(true).get();
            Elements urls = document.select("a");
            for (Element url : urls) {
                String child = url.attr("abs:href");
                if (isValid(child) & !alreadyVisited.contains(child)) {
                    this.siteParser.addChild(new SiteParser(child, this.siteParser.getIndexingService()));
                    urlSet.add(child);
                    addPage(document, child);
                    alreadyVisited.add(child);
                }
            }

            for (SiteParser url : siteParser.getChild()) {
                PageParser task = new PageParser(url, parentUrl, this.getSite());
                task.fork();
                taskSet.add(task);
            }

            for (PageParser task : taskSet) {
                task.join();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Site site = siteRepository.findSiteByUrl(siteParser.getUrl());
            site.setStatus(Status.FAILED);
            site.setLastError("Ошибка индексации сайта " + site.getUrl());
            siteRepository.save(site);
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

    private boolean isValid(String url) {
        return (!url.isEmpty() && !urlSet.contains(url)
                && url.startsWith(parentUrl.getUrl()) && !url.contains("#")
                && !url.matches("(\\S+(\\.(?i)(jpg|pdf|png|gif|bmp))$)"));
    }

    public Site getSite() {
        return site;
    }
}
