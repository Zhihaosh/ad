package service;

import ad.Ad;

import java.util.List;

public class AdService {
    private AdDao adDao;

    public void setAdDao(AdDao adDao) {
        this.adDao = adDao;
    }
    public void persist(Ad ad) {
        adDao.openCurrentSessionwithTransaction();
        adDao.persist(ad);
        System.out.println("persist 1");
        adDao.closeCurrentSessionwithTransaction();
    }
    public Ad findById(long id) {
        adDao.openCurrentSession();
        Ad ad = adDao.findById(id);
        adDao.closeCurrentSession();
        System.out.println("find 1");

        return ad;
    }
    public List<Ad> findAll() {
        adDao.openCurrentSession();
        List<Ad> ads = adDao.findAll();
        adDao.closeCurrentSession();
        return ads;
    }
    public void end(){
        this.adDao.end();
    }

}
