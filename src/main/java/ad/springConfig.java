package ad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.AdDao;
import service.AdService;
import service.GenericDaoHibernateImpl;
import service.HibernateConnector;

@Configuration

public class springConfig {
    @Bean
    public AdsEngine adsEngine(){
        GenericDaoHibernateImpl genericDaoHibernate = genericDaoHibernate();
        genericDaoHibernate.setSessionFactory(hibernateConnector());

        return new AdsEngine("/home/zhihao/Desktop/adserver/src/main/java/crawler/Ad.txt",
                "/home/zhihao/Desktop/adserver/src/main/java/data/budget.txt",
                "127.0.0.1",
                11211,
                adService(),
                genericDaoHibernate
                );
    }

    @Bean
    public AdService adService(){
        AdService adService =  new AdService();
        adService.setAdDao(adDao());
        return adService;
    }

    @Bean
    public GenericDaoHibernateImpl genericDaoHibernate(){
        return new GenericDaoHibernateImpl(Campaign.class);
    }

    @Bean
    public HibernateConnector hibernateConnector(){
        return new HibernateConnector();
    }
    @Bean
    public AdDao adDao(){
        AdDao adDao = new AdDao();
        adDao.setHibernateConnector(hibernateConnector());
        return adDao;
    }
    @Bean
    public Campaign campaign(){
        return new Campaign();
    }
}


