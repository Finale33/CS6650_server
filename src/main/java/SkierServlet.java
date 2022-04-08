import Entities.Message;
import Entities.Resort;
import Entities.Resorts;
import com.google.gson.Gson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;

@WebServlet(name = "SkierServlet", value = "/skiers")
public class SkierServlet extends HttpServlet {

    private String queueName = "myQueue";
    private ObjectPool<Channel> channelPool;
    private EventCountCircuitBreaker circuitBreaker;
    private static final int BACKOFF_UPPER = 800;
    private static final int BACKOFF_LOWER = 600;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String urlPath = req.getPathInfo();
        res.setContentType("application/json");
        // adding in circuit breaker
        if(!circuitBreaker.incrementAndCheckState()) {
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.getWriter().write("Too many request sent per second");
            return;
        }
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
        if(!circuitBreaker.incrementAndCheckState()) {
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.getWriter().write("Too many request sent per second");
            return;
        }
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
                System.out.println(message);
                if (isPayloadValid(message)) {
                    System.out.println("about to send out messages");
                    // append the skier id and day id
                    sb.append(urlParts[7]);
                    sb.append(",");
                    sb.append(urlParts[5]);
                    sb.append(",");
                    sb.append(urlParts[3]);
                    sb.append(",");
                    sb.append(urlParts[1]);
                    String cur = sb.toString();
                    Channel channel = null;
                    try{
                        channel = channelPool.borrowObject();
                        channel.queueDeclare(queueName, true, false, false, null);
                        channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, cur.getBytes(StandardCharsets.UTF_8));
                        System.out.println(" [x] Sent '" + cur + "'");
                    }catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to borrow channel from pool" + e.toString());
                    }finally {
                        try {
                            if (channel != null) {
                                channelPool.returnObject(channel);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    Message response = new Message("invalid input!");
                    res.getOutputStream().print(gson.toJson(response));
                    res.getOutputStream().flush();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServletException();
            }
        }
    }

    // {"time":394,"liftID":13,"waitTime":9}
    private boolean isPayloadValid (String payload) {
        String[] payloads = payload.split(",");
        if (payloads.length != 3)  return false;
        String[] payload0 = payloads[0].split(":");
        String[] payload1 = payloads[1].split(":");
        String[] payload2 = payloads[2].split(":");
        return payload0[0].contains("\"time\"") && isInteger(payload0[1]) && payload1[0].contains("\"liftID\"") && isInteger(payload1[1]) && payload2[0].contains("\"waitTime\"") && isInteger(payload2[1].substring(0, payload2[1].length() - 1)) && payload2[1].endsWith("}");
    }

    private boolean isUrlValid(String[] urlPath, String queryString) {
        // validate the request url path according to the API spec
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
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

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        circuitBreaker = new EventCountCircuitBreaker(BACKOFF_UPPER, 1, TimeUnit.SECONDS, BACKOFF_LOWER);
        try {
            this.channelPool = new GenericObjectPool<>(new ChannelFactory());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}

