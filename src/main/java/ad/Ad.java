package ad;

import java.io.Serializable;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "ad")
public class Ad implements Serializable{
    /**
     *
     */
    @Transient
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "adId")
    public  long adId;


    @Column(name = "campaignId")
    public long campaignId;


    @Column(name="keyWords")
    public String keyWords;

    @Transient
    public double relevanceScore;
    @Transient
    public double pClick;
    @Column(name = "bidPrice")
    public double bidPrice;
    @Transient
    public double rankScore;
    @Transient
    public double qualityScore;
    @Transient
    public double costPerClick;
    @Transient
    public int position;//1: top , 2: bottom
    @Column(name = "title")
    public String title; // required
    @Column(name = "price")
    public double price; // required
    @Column(name = "thumbnail")
    public String thumbnail; // required
    @Transient
    public String description; // required
    @Column(name = "brand")
    public String brand; // required
    @Column(name = "detail_url")
    public String detail_url; // required
    @Transient
    public String query; //required
    @Transient
    public int query_group_id;
    @Column(name = "category")
    public String category;

}
