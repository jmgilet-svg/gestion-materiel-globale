package com.materiel.suite.client.ui.common;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DateTimeField extends JPanel {
  private final JSpinner dateSpinner;
  private final JSpinner timeSpinner;

  public DateTimeField(){
    super(new FlowLayout(FlowLayout.LEFT, 4, 0));
    Date now = new Date();
    dateSpinner = new JSpinner(new SpinnerDateModel(now, null, null, java.util.Calendar.DAY_OF_MONTH));
    timeSpinner = new JSpinner(new SpinnerDateModel(now, null, null, java.util.Calendar.MINUTE));
    dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
    timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm"));
    add(dateSpinner);
    add(timeSpinner);
  }

  public LocalDateTime getDateTime(){
    Date d = (Date) dateSpinner.getValue();
    Date t = (Date) timeSpinner.getValue();
    ZoneId zone = ZoneId.systemDefault();
    LocalDateTime base = LocalDateTime.ofInstant(d.toInstant(), zone);
    LocalDateTime time = LocalDateTime.ofInstant(t.toInstant(), zone);
    return LocalDateTime.of(base.toLocalDate(), time.toLocalTime().withSecond(0).withNano(0));
  }

  public void setDateTime(LocalDateTime ldt){
    if (ldt==null) return;
    ZoneId zone = ZoneId.systemDefault();
    Date date = Date.from(ldt.withHour(0).withMinute(0).withSecond(0).withNano(0).atZone(zone).toInstant());
    Date time = Date.from(ldt.atZone(zone).toInstant());
    dateSpinner.setValue(date);
    timeSpinner.setValue(time);
  }
}
