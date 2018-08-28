package crawler;

import ad.Ad;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import  java.io.FileReader;
import java.util.List;

public class CrawlerMain {
    private String adsDataFilePath;
    private String rawQueryDataFilePath;
    private  AmazonCrawler crawler;

    public void setAdsDataFilePath(String adsDataFilePath) {
        this.adsDataFilePath = adsDataFilePath;
    }

    public void setRawQueryDataFilePath(String rawQueryDataFilePath) {
        this.rawQueryDataFilePath = rawQueryDataFilePath;
    }

    public void setCrawler(AmazonCrawler crawler) {
        this.crawler = crawler;
    }

    public  void generateAd(){
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(adsDataFilePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            BufferedReader br = new BufferedReader(new FileReader(rawQueryDataFilePath));
            String line;
            while ((line = br.readLine()) != null) {
                if(line.length() == 0)
                    continue;
                System.out.println(line);
                String[] fields = line.split(",");
                String query = fields[0].trim();
                double bidPrice = Double.parseDouble(fields[1].trim());
                int campaignId = Integer.parseInt(fields[2].trim());
                int queryGroupId = Integer.parseInt(fields[3].trim());

                List<Ad> ads =  crawler.GetAdBasicInfoByQuery(query, bidPrice, campaignId, queryGroupId);
                for(Ad ad : ads) {
                    String jsonInString = mapper.writeValueAsString(ad);
                    //System.out.println(jsonInString);
                    bw.write(jsonInString);
                    bw.newLine();
                }
                Thread.sleep(2000);
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
