package searchengine.services.parsing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;

import static java.lang.Thread.sleep;

public class SiteGetChild extends RecursiveAction {
    private final Site site;
    private final MainSite mainSite;
    private final MainSite parentUrl;
    private final List<String> alreadyVisited;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private static final CopyOnWriteArraySet<String> urlSet = new CopyOnWriteArraySet<>();

    public SiteGetChild(MainSite mainSite, MainSite parentUrl, Site site) {
        this.mainSite = mainSite;
        this.parentUrl = parentUrl;
        siteRepository = mainSite.getIndexingService().getSiteRepository();
        pageRepository = mainSite.getIndexingService().getPageRepository();
        this.site = site;
        alreadyVisited = new ArrayList<>();
    }



    @Override
    protected void compute() {
        Set<SiteGetChild> taskSet = new HashSet<>();
        try {
            sleep(150);
            Document document = Jsoup.connect(mainSite.getUrl()).userAgent("LuckySearchBot (Windows; U; WindowsNT" +
                    " 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com")
                    .ignoreContentType(true).ignoreHttpErrors(true).get();
            Elements urls = document.select("a");
            for (Element url : urls) {
                String child = url.attr("abs:href");
                if (isValid(child) & !alreadyVisited.contains(child)) {
                    this.mainSite.addChild(new MainSite(child, this.mainSite.getIndexingService()));
                    urlSet.add(child);
                    addPage(document, child);
                    alreadyVisited.add(child);
                }
            }

            for (MainSite url : mainSite.getChild()) {
                SiteGetChild task = new SiteGetChild(url, parentUrl, this.getSite());
                task.fork();
                taskSet.add(task);
            }

            for (SiteGetChild task : taskSet) {
                task.join();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Site site = siteRepository.findSiteByUrl(mainSite.getUrl());
            site.setStatus(Status.FAILED);
            site.setLastError("Ошибка индексации сайта " + site.getUrl());
            siteRepository.save(site);
        }

    }

    private void addPage(Document document, String child) {
        if (child.equals("")) return;
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        Page page = new Page();
        page.setSite(site);
        page.setCode(document.connection().response().statusCode());
        page.setPath(child.replace(site.getUrl(), ""));
        page.setContent(document.html());
        pageRepository.save(page);
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
