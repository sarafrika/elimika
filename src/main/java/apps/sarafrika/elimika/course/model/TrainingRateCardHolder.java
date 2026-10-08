package apps.sarafrika.elimika.course.model;

import java.math.BigDecimal;

/** A stored rate card: the currency plus eight rates, one per format, location and basis (hourly or daily); null means not offered. */
public interface TrainingRateCardHolder {

    String getRateCurrency();

    void setRateCurrency(String rateCurrency);

    BigDecimal getPrivateOnlineHourlyRate();

    void setPrivateOnlineHourlyRate(BigDecimal rate);

    BigDecimal getPrivateInpersonHourlyRate();

    void setPrivateInpersonHourlyRate(BigDecimal rate);

    BigDecimal getGroupOnlineHourlyRate();

    void setGroupOnlineHourlyRate(BigDecimal rate);

    BigDecimal getGroupInpersonHourlyRate();

    void setGroupInpersonHourlyRate(BigDecimal rate);

    BigDecimal getPrivateOnlineDailyRate();

    void setPrivateOnlineDailyRate(BigDecimal rate);

    BigDecimal getPrivateInpersonDailyRate();

    void setPrivateInpersonDailyRate(BigDecimal rate);

    BigDecimal getGroupOnlineDailyRate();

    void setGroupOnlineDailyRate(BigDecimal rate);

    BigDecimal getGroupInpersonDailyRate();

    void setGroupInpersonDailyRate(BigDecimal rate);
}
