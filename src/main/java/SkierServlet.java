import Entities.LiftRideLocal;
import Entities.Message;
import com.google.gson.Gson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeoutException;

import redis.clients.jedis.Jedis;

@WebServlet(name = "SkierServlet", value = "/skiers")
public class SkierServlet extends HttpServlet {

    private Publisher publisherToSkierService;
    private Publisher publisherToResortService;
    private final String SKIER_QUEUE_NAME = "skierServiceQueue";
    private final String RESORT_QUEUE_NAME = "resortServiceQueue";


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json"); // the difference between application/json and text/plain ?
        String urlPath = req.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)
        for(int i = 0; i < urlParts.length; i++) {
            if(i == 3 || i == 5 || i == 7) System.out.println(urlParts[i]);
        }
        try{
            Jedis jedis = new Jedis("localhost", 6379); // consumer instance private ip: 172.31.26.108
            System.out.println("Successfully connected to Redis...");
            if (!isUrlValidGet(urlParts)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().write("{\n" +
                        "  \"message\": \"invalid get url\"\n" +
                        "}");
            } else {
                res.setStatus(HttpServletResponse.SC_OK);
                // do any sophisticated processing with urlParts which contains all the url params
                // TODO: process url params in `urlParts`
                if(validateGet1(urlParts)){
                    int totalVertical = 0;
                    for(int i = 1; i <= 90; i++){
                        String key = "vertical-num"+urlParts[1]+"/"+ i;
                        if (jedis.exists(key.getBytes(StandardCharsets.UTF_8))) {
                            List<String> list = jedis.lrange( key, 0, -1 );
                            for(String s : list){
                                totalVertical += Integer.valueOf(s);
                            }
                        }
                    }
                    res.getWriter().write("{\n" +
                            "  \"seasonID\": \"Mission Ridge\",\n" +
                            "  \"totalVert\": " + String.valueOf(totalVertical*10) +
                            "\n}");
                }else if(validateGet2(urlParts)){
                    int totalVertical = 0;
                    String key = "vertical-num"+urlParts[7]+"/"+urlParts[5];
                    if (jedis.exists(key.getBytes(StandardCharsets.UTF_8))) {
                        totalVertical = Integer.parseInt(jedis.get(key))*10;
                        res.getWriter().write(String.valueOf(totalVertical));
                    }
                }
            }
        }catch(Exception e){
            System.out.println(e);
        }
    }

    private boolean validateGet1(String[] urlPath) {
        if(urlPath != null) {
            if (urlPath.length == 3) {
                return isInteger(urlPath[1])
                        && "vertical".equals(urlPath[2]);
            }
        }
        return false;
    }


    private boolean validateGet2(String[] urlPath) {
        if (urlPath.length != 8) return false;
        if (urlPath[0].isEmpty() && isInteger(urlPath[1]) && urlPath[2].equals("seasons") && isInteger(urlPath[3]) && urlPath[4].equals("days") && isInteger(urlPath[5]) && urlPath[6].equals("skiers") && isInteger(urlPath[7])) {
            return true;
        }
        return false;
    }
    private boolean isUrlValidGet(String[] urlPath) {
        return validateGet1(urlPath) || validateGet2(urlPath);
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

        // validate url first
        if (!isLongURL(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            Message response = new Message("resource not found");
            res.getOutputStream().print(gson.toJson(response));
            res.getOutputStream().flush();
            return;
        } else {
            res.setStatus(HttpServletResponse.SC_CREATED);
            // process url params in `urlParts` and process request body
            try {
                // validate json payload and send to message queue
                BufferedReader reader = req.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String message = sb.toString();
                String[] queries = message.split(",");
                String liftID = queries[1].split(":")[1];
                String time = queries[0].split(":")[1];
                String resortID = urlParts[1];
                String dayID = urlParts[5];
                String skierID = urlParts[7];
                String seasonID = urlParts[3];
                LiftRideLocal liftRide = new LiftRideLocal(resortID, dayID, skierID, time, liftID, seasonID);
                System.out.println("about to send out messages");
                publisherToSkierService.send(liftRide);
                publisherToResortService.send(liftRide);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServletException();
            }
        }
    }

//    // {"time":394,"liftID":13,"waitTime":9}
//    private boolean isPayloadValid (String payload) {
//        String[] payloads = payload.split(",");
//        if (payloads.length != 3)  return false;
//        String[] payload0 = payloads[0].split(":");
//        String[] payload1 = payloads[1].split(":");
//        String[] payload2 = payloads[2].split(":");
//        return payload0[0].contains("\"time\"") && isInteger(payload0[1]) && payload1[0].contains("\"liftID\"") && isInteger(payload1[1]) && payload2[0].contains("\"waitTime\"") && isInteger(payload2[1].substring(0, payload2[1].length() - 1)) && payload2[1].endsWith("}");
//    }

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

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            publisherToSkierService = new Publisher(SKIER_QUEUE_NAME);
            publisherToResortService = new Publisher(RESORT_QUEUE_NAME);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}

