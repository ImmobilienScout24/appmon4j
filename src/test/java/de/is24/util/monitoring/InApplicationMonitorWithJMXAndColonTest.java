package de.is24.util.monitoring;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * written to reproduce bug #16204 (which it doesn't)
 *
 * @author ptraeder
 */
public class InApplicationMonitorWithJMXAndColonTest {
  @BeforeClass
  public static void setupClass() {
    TestHelper.setInstanceForTesting();
  }

  @AfterClass
  public static void tearDownClass() {
    TestHelper.resetInstanceForTesting();
  }

  @Test
  public void testSimpleValue() {
    InApplicationMonitor.getInstance().addTimerMeasurement("simpleTimer", 5);
  }

  @Test
  public void testValueWithColon() {
    for (int i = 1; i < 10; i++) {
      InApplicationMonitor.getInstance()
      .addTimerMeasurement(
        "class de.is24.email.mailer.service.MailSessionAndTransport.mailsSendWithServer.smtp:://@hammta10.be.ham.is24:25 ",
        42);
      InApplicationMonitor.getInstance().addTimerMeasurement("timerWith:Colon", 42);
      InApplicationMonitor.getInstance().addTimerMeasurement("timerWith=Equals", 42 + 1);
    }
  }

}
