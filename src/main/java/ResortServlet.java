import Entities.*;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "ResortServlet", value = "/resorts")
public class ResortServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String urlPath = req.getPathInfo();
        res.setContentType("application/json");
        Gson gson = new Gson();

        // if is null url
        if (urlPath == null) {
            res.setStatus(HttpServletResponse.SC_OK);
            try{
                RealResort[] response = new RealResort[1];
                RealResort resort = new RealResort("new resort", 5);
                response[0] = resort;
                RealResorts resorts = new RealResorts(response);
                res.getOutputStream().print(gson.toJson(resorts));
                res.getOutputStream().flush();
            } catch (Exception e) {
                throw new ServletException();
            }
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            Message response = new Message("resource not found");
            res.getOutputStream().print(gson.toJson(response));
            res.getOutputStream().flush();
            return;
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            if (isLongURL(urlParts)) {
                try{
                    Jedis jedis = new Jedis("35.88.188.227", 6379); // consumer instance private ip: 172.31.26.108
                    System.out.println("Successfully connected to Redis...");
                    String key = "skier-num"+'/'+urlParts[1] + '/' + urlParts[3]+"/"+urlParts[5];
                    System.out.println(key);
                    System.out.println("result: "+jedis.hlen(key));
                    if (jedis.exists(key.getBytes(StandardCharsets.UTF_8))) {
                        res.getWriter().write("{\n" +
                                "  \"Time\": \"237\",\n" +
                                "  \"NumOfSkiers\": " + jedis.hlen(key) +
                                "\n}");
                    }
                } catch (Exception e) {
                    throw new ServletException();
                }
            }
            else {
                try{
                    String[] seasons = new String[2];
                    seasons[0] = "Spring";
                    seasons[1] = "Winter";
                    SeasonsList response = new SeasonsList(seasons);
                    res.getOutputStream().print(gson.toJson(response));
                    res.getOutputStream().flush();
                } catch (Exception e) {
                    throw new ServletException();
                }
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();
        Gson gson = new Gson();

        // check we have a URL!
        if (urlPath == null || urlPath.length() == 0) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            Message response = new Message("resource not found");
            res.getOutputStream().print(gson.toJson(response));
            res.getOutputStream().flush();
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isShortURL(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            Message response = new Message("resource not found");
            res.getOutputStream().print(gson.toJson(response));
            res.getOutputStream().flush();
            return;
        } else {
            // TODO: process url params in `urlParts` and process request body
            try {
                // read request body
                BufferedReader reader = req.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                System.out.println(sb.toString());
            } catch (Exception e) {
                throw new ServletException();
            }
            res.setStatus(HttpServletResponse.SC_CREATED);
        }
    }

    private boolean isUrlValid(String[] urlPath) {
        // TODO: validate the request url path according to the API spec
        return isLongURL(urlPath) || isShortURL(urlPath);
    }

    private boolean isShortURL(String[] urlPath) {
        if (urlPath.length != 3) return false;
        return urlPath[0].isEmpty() && isInteger(urlPath[1]) && urlPath[2].equals("seasons");
    }

    private boolean isLongURL(String[] urlPath) {
        if (urlPath.length != 7) return false;
        if (urlPath[0].isEmpty() && isInteger(urlPath[1]) && urlPath[2].equals("seasons") && isInteger(urlPath[3]) && urlPath[4].equals("day") && isInteger(urlPath[5]) && urlPath[6].equals("skiers")) {
            return true;
        }
        return false;
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(Exception e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }
}
