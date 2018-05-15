package crawler;

import ad.Ad;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Deduper {
    private String rawAdPath;
    private  String adsDataFilePath;
    private HashSet<String> uniqueSet = new HashSet();
    private int id = 0;
    public void setRawAdPath(String rawAdPath) {
        this.rawAdPath = rawAdPath;
    }

    public void setAdsDataFilePath(String adsDataFilePath) {
        this.adsDataFilePath = adsDataFilePath;
    }

    public void dedupe(){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            BufferedReader br = new BufferedReader(new FileReader(rawAdPath));
            BufferedWriter bw = new BufferedWriter(new FileWriter(adsDataFilePath));
            String line;
            while ((line = br.readLine()) != null) {
                JSONObject obj = new JSONObject(line);
                if(uniqueSet.contains(obj.getString("detail_url"))){
                    continue;
                }
                uniqueSet.add(obj.getString("detail_url"));
                Ad ad = new Ad();
                ad.detail_url = obj.getString("detail_url");
                ad.category = obj.getString("category");
                ad.price = obj.getDouble("price") == 0.0 ? Math.random() * 2000 :  obj.getDouble("price");
                ad.title = obj.getString("title");
                ad.bidPrice = obj.getDouble("bidPrice");
                ad.campaignId = obj.getInt("campaignId");
                ad.brand = obj.getString("brand");
                ad.query = obj.getString("query");
                ad.query_group_id = obj.getInt("query_group_id");
                ad.thumbnail = obj.getString("thumbnail");
                ad.adId = id;
                ad.keyWords = String.join(" ",(obj.getString("title").split(" ")));
                id++;
                String jsonInString = objectMapper.writeValueAsString(ad);
                bw.write(jsonInString);
                bw.newLine();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
