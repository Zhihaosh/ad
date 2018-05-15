package ad;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "campaignId")
public class Campaign implements Serializable {
    @Id
    @Column(name = "campaignId")
    public Long campaignId;

    @Column(name = "budget")
    public double budget;
    public Campaign(){

    }
    public  Campaign(Long campaignId, Double budget){
        this.campaignId = campaignId;
        this.budget = budget;
    }
}
