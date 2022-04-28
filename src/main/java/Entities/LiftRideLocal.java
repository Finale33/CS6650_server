package Entities;

import com.google.gson.Gson;

public class LiftRideLocal {
  private String resortID;
  private String dayID;
  private String skierID;
  private String time;
  private String liftID;
  private String seasonID;

  public LiftRideLocal(String resortID, String dayID, String skierID, String time,
                       String liftID, String seasonID) {
    this.resortID = resortID;
    this.dayID = dayID;
    this.skierID = skierID;
    this.time = time;
    this.liftID = liftID;
    this.seasonID = seasonID;
  }

  public String getResortID() {
    return resortID;
  }

  public void setResortID(String resortID) {
    this.resortID = resortID;
  }

  public String getDayID() {
    return dayID;
  }

  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  public String getSkierID() {
    return skierID;
  }

  public void setSkierID(String skierID) {
    this.skierID = skierID;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getLiftID() {
    return liftID;
  }

  public String getSeasonID() {
    return seasonID;
  }

  public void setSeasonID(String seasonID) {
    this.seasonID = seasonID;
  }

  public void setLiftID(String liftID) {
    this.liftID = liftID;
  }
  @Override
  public String toString() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}