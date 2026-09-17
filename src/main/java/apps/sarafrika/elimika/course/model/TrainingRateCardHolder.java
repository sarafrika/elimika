package apps.sarafrika.elimika.course.model;

import java.math.BigDecimal;

/** A stored rate card: the currency plus twelve rates, one per format, location and basis; null means not offered. */
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

    BigDecimal getPrivateOnlineSessionRate();

    void setPrivateOnlineSessionRate(BigDecimal rate);

    BigDecimal getPrivateInpersonSessionRate();

    void setPrivateInpersonSessionRate(BigDecimal rate);

    BigDecimal getGroupOnlineSessionRate();

    void setGroupOnlineSessionRate(BigDecimal rate);

    BigDecimal getGroupInpersonSessionRate();

    void setGroupInpersonSessionRate(BigDecimal rate);

    BigDecimal getPrivateOnlineDailyRate();

    void setPrivateOnlineDailyRate(BigDecimal rate);

    BigDecimal getPrivateInpersonDailyRate();

    void setPrivateInpersonDailyRate(BigDecimal rate);

    BigDecimal getGroupOnlineDailyRate();

    void setGroupOnlineDailyRate(BigDecimal rate);

    BigDecimal getGroupInpersonDailyRate();

    void setGroupInpersonDailyRate(BigDecimal rate);
}
