import Entities.EndpointStat;
import Entities.EndpointStats;
import Entities.RealResort;
import com.google.gson.Gson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet(name = "StatisticsServlet", value = "/statistics")
public class StatisticsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        Gson gson = new Gson();
        res.setStatus(HttpServletResponse.SC_OK);
        try{
            EndpointStat[] response = new EndpointStat[1];
            EndpointStat stat = new EndpointStat("/resorts", "GET", 11,385);
            response[0] = stat;
            EndpointStats endpointStats = new EndpointStats(response);
            res.getOutputStream().print(gson.toJson(endpointStats));
            res.getOutputStream().flush();
        } catch (Exception e) {
            throw new ServletException();
        }
    }
}
