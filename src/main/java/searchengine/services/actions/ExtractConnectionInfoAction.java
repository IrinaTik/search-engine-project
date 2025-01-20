package searchengine.services.actions;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.JsoupConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class ExtractConnectionInfoAction {

    private static final String CSS_QUERY = "a[href]";
    private static final String LINK_ELEMENT_KEY = "href";
    public static final Integer PAGE_CODE_SUCCESS = 200;

    private static final List<String> ACCEPTABLE_CONTENT_TYPES = List.of(
            "text/plain",
            "text/html",
            "text/xml",
            "application/xhtml+xml",
            "application/xml",
            "application/rtf");

    public static Connection.Response getResponseFromUrl(String pageUrl, JsoupConfig jsoupConfig) throws Exception {
        holdTimeoutBeforeParse(pageUrl, jsoupConfig);
        Connection connection = configureConnection(pageUrl, jsoupConfig);
        return connection.execute();
    }

    private static void holdTimeoutBeforeParse(String pageUrl, JsoupConfig jsoupConfig) throws InterruptedException {
        double pause = Math.random() * jsoupConfig.getPauseBeforeParseMultiplier() + jsoupConfig.getPauseBeforeParseSummand();
        log.debug("Pause {} before parsing page {}", String.format("%.2f", pause), pageUrl);
        Thread.sleep((long) pause);
    }

    private static Connection configureConnection(String pageUrl, JsoupConfig jsoupConfig) {
        return Jsoup.connect(pageUrl)
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReferrer())
                .timeout(jsoupConfig.getTimeout())
                .maxBodySize(jsoupConfig.getMaxBodySize())
                .ignoreContentType(jsoupConfig.isIgnoreContentType());
    }

    private static boolean isResponseContentTypeAcceptable(Connection.Response response) {
        for (String contentType : ACCEPTABLE_CONTENT_TYPES) {
            if (StringUtils.containsIgnoreCase(response.contentType(), contentType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPageCodeSuccessful(Integer pageCode) {
        return pageCode != null && pageCode.equals(PAGE_CODE_SUCCESS);
    }

    public static Set<String> getChildLinksFromResponse(Connection.Response response, String pageUrl)
            throws IOException {
        if (isPageCodeSuccessful(response.statusCode()) && isResponseContentTypeAcceptable(response)) {
            Document doc = response.parse();
            Elements links = doc.select(CSS_QUERY);
            return links.stream()
                    .map(linkCode -> linkCode.absUrl(LINK_ELEMENT_KEY))
                    .filter(link -> FormatUrlAction.isGoodLink(pageUrl, link))
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
