package crawler;

import ad.Ad;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.util.*;
import java.io.*;
import java.io.IOException;
import java.net.*;

public class AmazonCrawler {
    private static final String AMAZON_QUERY_URL = "https://www.amazon.com/s/ref=nb_sb_noss?field-keywords=";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36";
    private final String authUser = "bittiger";
    private final String authPassword = "cs504";
    private List<String> proxyList;
    private List<String> titleList;
    private List<String> categoryList;
    private HashSet crawledUrl;
    BufferedWriter logBFWriter;
    private int index = 0;
    private String proxy_file;
    private  String log_file;

    public void setProxy_file(String proxy_file){
        this.proxy_file = proxy_file;
    }
    public void setLog_file(String log_file){
        this.log_file = log_file;
        init();
    }

    public void init(){
        crawledUrl = new HashSet();
        initProxyList(this.proxy_file);
        initHtmlSelector();
        initLog(this.log_file);
    }

    private String normalizeUrl(String url) {
        int i = url.indexOf("ref");
        String normalizedUrl = url.substring(0, i - 1);
        return normalizedUrl;
    }
    public void cleanup() {
        if (logBFWriter != null) {
            try {
                logBFWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initProxyList(String proxy_file) {
        proxyList = new ArrayList<String>();
        try  {
            BufferedReader br = new BufferedReader(new FileReader(proxy_file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                String ip = fields[0].trim();
                proxyList.add(ip);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Authenticator.setDefault(
                new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                authUser, authPassword.toCharArray());
                    }
                }
        );

        System.setProperty("http.proxyUser", authUser);
        System.setProperty("http.proxyPassword", authPassword);
        System.setProperty("socksProxyPort", "61336"); // set proxy port
    }

    private void initHtmlSelector() {
        titleList = new ArrayList<String>();
        titleList.add(" > div > div > div > div.a-fixed-left-grid-col.a-col-right > div.a-row.a-spacing-small > div:nth-child(1)  > a > h2");
        titleList.add(" > div > div > div > div.a-fixed-left-grid-col.a-col-right > div.a-row.a-spacing-small > a > h2");

        categoryList = new ArrayList<String>();
        //#refinements > div.categoryRefinementsSection > ul.forExpando > li:nth-child(1) > a > span.boldRefinementLink
        categoryList.add("#leftNavContainer > ul:nth-child(3) > li.s-ref-indent-one > span > h4");
        categoryList.add("#leftNavContainer > ul:nth-child(3) > div > li:nth-child(1) > span > a > h4");


    }

    private void initLog(String log_path) {
        try {
            File log = new File(log_path);
            // if file doesnt exists, then create it
            if (!log.exists()) {
                log.createNewFile();
            }
            FileWriter fw = new FileWriter(log);
            logBFWriter = new BufferedWriter(fw);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setProxy() {
        //rotate
        if (index == proxyList.size()) {
            index = 0;
        }
        String proxy = proxyList.get(index);
        System.setProperty("socksProxyHost", proxy); // set proxy server
        index++;
    }

    public List<Ad> GetAdBasicInfoByQuery(String query, double bidPrice,int campaignId,int queryGroupId) {
        List<Ad> products = new ArrayList();
        try {
            setProxy();
            String url = AMAZON_QUERY_URL + query;
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, sdch, br");
            headers.put("Accept-Language", "en-US,en;q=0.8");
            Document doc = Jsoup.connect(url).headers(headers).userAgent(USER_AGENT).timeout(100000).get();
            Elements results = doc.select("li[data-asin]");
            for (int i = 0; i < results.size(); i++) {
                Ad ad = new Ad();
                String detail_path = "#result_"+Integer.toString(i)+" > div > div > div > div.a-fixed-left-grid-col.a-col-left > div > div > a";
                Element detail_url_ele = doc.select(detail_path).first();
                if(detail_url_ele != null) {
                    String detail_url = detail_url_ele.attr("href");
                    System.out.println("detail = " + detail_url);
                    String normalizedUrl = normalizeUrl(detail_url);
                    if(crawledUrl.contains(normalizedUrl)) {
                        logBFWriter.write("crawled url:" + normalizedUrl);
                        logBFWriter.newLine();
                        continue;
                    }
                    crawledUrl.add(normalizedUrl);
                    System.out.println("normalized url  = " + normalizedUrl);
                    ad.detail_url = normalizedUrl;
                } else {
                    logBFWriter.write("cannot parse detail for query:" + query + ", title: " + ad.title);
                    logBFWriter.newLine();
                    continue;
                }
                if (ad.detail_url == null){
                    ad.detail_url = "";
                }
                ad.query = query;
                ad.query_group_id = queryGroupId;
                ad.keyWords = "";
                for (String title : titleList) {
                    String title_ele_path = "#result_"+Integer.toString(i)+ title;
                    Element title_ele = doc.select(title_ele_path).first();
                    if(title_ele != null) {
                        System.out.println("title = " + title_ele.text());
                        ad.title = title_ele.text();
                        break;
                    }
                }
                if (ad.title == "") {
                    logBFWriter.write("cannot parse title for query: " + query);
                    logBFWriter.newLine();
                    continue;
                }
                if (ad.title == null){
                    ad.category = "";
                }
                //thumbnail
                String thumbnail_path = "#result_"+Integer.toString(i)+" > div > div > div > div.a-fixed-left-grid-col.a-col-left > div > div > a > img";
                Element thumbnail_ele = doc.select(thumbnail_path).first();
                if(thumbnail_ele != null) {
                    //System.out.println("thumbnail = " + thumbnail_ele.attr("src"));
                    ad.thumbnail = thumbnail_ele.attr("src");
                } else {
                    logBFWriter.write("cannot parse thumbnail for query:" + query + ", title: " + ad.title);
                    logBFWriter.newLine();
                    continue;
                }
                String brand_path = "#result_"+Integer.toString(i)+" > div > div > div > div.a-fixed-left-grid-col.a-col-right > div.a-row.a-spacing-small > div > span:nth-child(2)";
                Element brand = doc.select(brand_path).first();
                if(brand != null) {
                    //System.out.println("brand = " + brand.text());
                    ad.brand = brand.text();
                }
                if (ad.brand == null){
                    ad.brand = "";
                }
                //#result_2 > div > div > div > div.a-fixed-left-grid-col.a-col-right > div:nth-child(3) > div.a-column.a-span7 > div.a-row.a-spacing-none > a > span > span > span
                ad.bidPrice = bidPrice;
                ad.campaignId = campaignId;
                ad.price = 0.0;
                //price
                String price_whole_path = "#result_"+Integer.toString(i)+" > div > div.a-fixed-left-grid > div > div.a-fixed-left-grid-col.a-col-right > div:nth-child(2) > div.a-column.a-span7 > div.a-row.a-spacing-none > a > span.a-color-base.sx-zero-spacing > span > span";
                String price_fraction_path = "#result_"+Integer.toString(i)+" > div > div.a-fixed-left-grid > div > div.a-fixed-left-grid-col.a-col-right > div:nth-child(2) > div.a-column.a-span7 > div.a-row.a-spacing-none > a > span.a-color-base.sx-zero-spacing > span > sup.sx-price-fractional";
                Element price_whole_ele = doc.select(price_whole_path).first();
                if(price_whole_ele != null) {
                    String price_whole = price_whole_ele.text();
                    //System.out.println("price whole = " + price_whole);
                    //remove ","
                    //1,000
                    if (price_whole.contains(",")) {
                        price_whole = price_whole.replaceAll(",","");
                    }

                    try {
                        ad.price = Double.parseDouble(price_whole);
                    } catch (NumberFormatException ne) {
                        // TODO Auto-generated catch block
                        ne.printStackTrace();
                        //log
                    }
                }

                Element price_fraction_ele = doc.select(price_fraction_path).first();
                if(price_fraction_ele != null) {
                    //System.out.println("price fraction = " + price_fraction_ele.text());
                    try {
                        ad.price = ad.price + Double.parseDouble(price_fraction_ele.text()) / 100.0;
                    } catch (NumberFormatException ne) {
                        ne.printStackTrace();
                    }
                }
                for (String category : categoryList) {
                    Element category_ele = doc.select(category).first();
                    if(category_ele != null) {
                        System.out.println("category = " + category_ele.text());
                        ad.category = category_ele.text();
                        break;
                    }
                }
                if (ad.category  == "") {
                    logBFWriter.write("cannot parse category for query:" + query + ", title: " + ad.title);
                    logBFWriter.newLine();
                    continue;
                }
                if (ad.category == null){
                    ad.category = "";
                }
                products.add(ad);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return products;
    }

}