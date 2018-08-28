package service;

import java.util.List;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateConnector {
    private  SessionFactory factory;
    public HibernateConnector() {
        factory = new Configuration().configure().buildSessionFactory();
    }
    public Session getSession() throws HibernateException {
        Session session = factory.openSession();
        if (!session.isConnected()) {
            this.reconnect();
        }
        return session;
    }
    private void reconnect() throws HibernateException {
        this.factory = new Configuration().configure().buildSessionFactory();
    }
    public void end(){
        this.factory.close();
    }
}