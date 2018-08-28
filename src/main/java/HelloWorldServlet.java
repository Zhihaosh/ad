import ad.Ad;
import ad.AdsEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.springframework.context.support.FileSystemXmlApplicationContext;


public class HelloWorldServlet extends HttpServlet
{
    AdsEngine adEngine;
    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext ctx = new AnnotationConfigApplicationContext(springConfig.class);

        adEngine = ctx.getBean(AdsEngine.class);
    }

    public void doGet (HttpServletRequest req,
                       HttpServletResponse res)
            throws ServletException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        PrintWriter out = res.getWriter();
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        String query = req.getParameter("name");
        String device_id = req.getParameter("device_id");
        String device_ip = req.getParameter("device_ip");
        String query_category = req.getParameter("query_category");
        List<Ad> result = adEngine.selectAds(query,device_id,device_ip,query_category);
        for (Ad cur : result){
            out.println(mapper.writeValueAsString(cur));
        }
        out.close();
    }
}