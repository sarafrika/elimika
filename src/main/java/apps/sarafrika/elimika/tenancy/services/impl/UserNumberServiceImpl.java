package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.utils.VerhoeffCheckDigit;
import apps.sarafrika.elimika.tenancy.services.UserNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserNumberServiceImpl implements UserNumberService {
    private static final long MAX_BASE_VALUE = 99_999_999L;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String nextUserNo() {
        Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('user_no_seq')", Long.class);
        if (nextVal == null) {
            throw new IllegalStateException("Failed to generate user number");
        }
        if (nextVal < 0 || nextVal > MAX_BASE_VALUE) {
            throw new IllegalStateException("User number sequence exhausted");
        }
        String base = String.format(Locale.ROOT, "%08d", nextVal);
        return VerhoeffCheckDigit.append(base);
    }
}
