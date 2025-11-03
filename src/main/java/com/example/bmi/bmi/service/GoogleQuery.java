package com.example.bmi.bmi.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GoogleQuery Service for fetching search results.
 * Supports both Jsoup scraping and Google Custom Search API.
 */
@Service
public class GoogleQuery {

    private final RestTemplate restTemplate;

    // read values from application.properties (keys used in this project)
    @Value("${google.cse.apiKey:}")
    private String apiKey;

    @Value("${google.cse.cx:}")
    private String cx;

    public GoogleQuery() {
        this.restTemplate = new RestTemplate();
    }

    public String searchKeyword;
    public String url;
    public String content;

    public GoogleQuery(String searchKeyword) {
        this();
        this.searchKeyword = searchKeyword;
        try {
            String encodeKeyword = URLEncoder.encode(searchKeyword, StandardCharsets.UTF_8.name());
            this.url = "https://www.google.com/search?q=" + encodeKeyword + "&oe=utf8&num=20";
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private String fetchContent() throws IOException {
        StringBuilder retVal = new StringBuilder();
        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        conn.setRequestProperty("User-agent", "Chrome/107.0.5304.107");
        try (InputStream in = conn.getInputStream();
             InputStreamReader inReader = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader bufReader = new BufferedReader(inReader)) {
            String line;
            while ((line = bufReader.readLine()) != null) {
                retVal.append(line);
            }
        }
        return retVal.toString();
    }

    /** 原始 Jsoup 版本爬蟲 */
    public HashMap<String, String> query() throws IOException {
        if (content == null) {
            content = fetchContent();
        }
        HashMap<String, String> retVal = new HashMap<>();
        Document doc = Jsoup.parse(content);
        Elements lis = doc.select("div").select(".kCrYT");
        for (Element li : lis) {
            try {
                String citeUrl = li.select("a").get(0).attr("href").replace("/url?q=", "");
                String title = li.select("a").get(0).select(".vvjwJb").text();
                if (title.isEmpty()) continue;
                retVal.put(title, citeUrl);
            } catch (IndexOutOfBoundsException ignored) {}
        }
        return retVal;
    }

    /** ✅ 新增的 Google Custom Search API 版本 */
    public Map<String, String> queryByApi(String query) {
        Map<String, String> results = new HashMap<>();

        if (apiKey == null || apiKey.isEmpty() || cx == null || cx.isEmpty()) {
            System.out.println("⚠️ Google API key or CX not configured in application.properties");
            return results;
        }

        try {
            String q = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String apiUrl = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cx + "&num=10&q=" + q;

            ResponseEntity<Map<String, Object>> resp =
                    restTemplate.getForEntity(apiUrl, (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> body = resp.getBody();
            if (body == null) return results;

            Object itemsObj = body.get("items");
            if (itemsObj instanceof List) {
                List<?> items = (List<?>) itemsObj;
                for (Object itemObj : items) {
                    if (itemObj instanceof Map) {
                        Map<String, Object> item = (Map<String, Object>) itemObj;
                        String title = item.get("title") == null ? "" : item.get("title").toString();
                        String link = item.get("link") == null ? "" : item.get("link").toString();
                        if (!title.isEmpty() && !link.isEmpty()) {
                            results.put(title, link);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
}
