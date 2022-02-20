import Entities.Message;
import Entities.Resort;
import Entities.Resorts;
import com.google.gson.Gson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "SkierServlet", value = "/skiers")
public class SkierServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String urlPath = req.getPathInfo();
        res.setContentType("application/json");
        Gson gson = new Gson();
        String queryString = req.getQueryString();

        // check we have a URL!
        if (urlPath == null || urlPath.length() == 0) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            Message response = new Message("resource not found");
            res.getOutputStream().print(gson.toJson(response));
            res.getOutputStream().flush();
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts, queryString)) {
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
                try {
                    int response = 111;
                    res.getOutputStream().print(gson.toJson(response));
                    res.getOutputStream().flush();
                } catch (Exception e) {
                    throw new ServletException();
                }
            }
            else {
                try{
                    Resort[] response = new Resort[1];
                    Resort resort = new Resort("123", 5);
                    response[0] = resort;
                    Resorts resorts = new Resorts(response);
                    res.getOutputStream().print(gson.toJson(resorts));
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
        System.out.println(urlPath);

        // check we have a URL!
        if (urlPath == null || urlPath.length() == 0) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            Message response = new Message("resource not found");
            res.getOutputStream().print(gson.toJson(response));
            res.getOutputStream().flush();
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isLongURL(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            Message response = new Message("resource not found");
            res.getOutputStream().print(gson.toJson(response));
            res.getOutputStream().flush();
            return;
        } else {
            res.setStatus(HttpServletResponse.SC_CREATED);
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
        }
    }

    private boolean isUrlValid(String[] urlPath, String queryString) {
        // TODO: validate the request url path according to the API spec
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123
        return isLongURL(urlPath) || isShortURL(urlPath, queryString);
    }

    private boolean isShortURL(String[] urlPath, String queryString) {
        if (urlPath.length != 3) return false;
        if (urlPath[0].isEmpty() && isInteger(urlPath[1]) && urlPath[2].equals("vertical")) {
            String[] queries = queryString.split("&");
            String[] resort = queries[0].split("=");
            boolean isResortValid = resort[0].equals("resort") && resort.length == 2 && resort[1] != null && !resort[1].isEmpty();
            if (queries.length == 1) {
                return isResortValid;
            }
            if (queries.length == 2) {
                String[] season = queries[1].split("=");
                return isResortValid && season[0].equals("season") && season.length == 2 && season[1] != null && !season[1].isEmpty();
            }
        }
        return false;
    }

    private boolean isLongURL(String[] urlPath) {
        if (urlPath.length != 8) return false;
        if (urlPath[0].isEmpty() && isInteger(urlPath[1]) && urlPath[2].equals("seasons") && isInteger(urlPath[3]) && urlPath[4].equals("days") && isInteger(urlPath[5]) && urlPath[6].equals("skiers") && isInteger(urlPath[7])) {
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
