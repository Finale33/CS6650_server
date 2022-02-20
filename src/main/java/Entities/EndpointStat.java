package Entities;

public class EndpointStat {
    String URL;
    String operation;
    int mean;
    int max;

    public EndpointStat(String URL, String operation, int mean, int max) {
        this.URL = URL;
        this.operation = operation;
        this.mean = mean;
        this.max = max;
    }
}
