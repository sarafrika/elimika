package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Which of a caller's domains the dashboard they are standing on may exercise.
 * <p>
 * The single authority on the narrowing rule, so that every module asking the question asks it the
 * same way. {@link DomainSecurityService} says what domains the account holds — a fact that is the
 * same on every page — and this says which of them the current request is entitled to act through.
 * The pairing is always a conjunction:
 * <pre>{@code
 * actingDomainCap.permits(UserDomain.admin) && domainSecurityService.isPlatformAdmin()
 * }</pre>
 * <p>
 * <strong>Narrowing only.</strong> Nothing here grants a domain; it only withholds one the caller
 * already holds. A request that claims a dashboard the caller has no domain for gains nothing,
 * because the second half of every conjunction still has to pass on its own. That is what makes the
 * header safe to accept from a browser.
 * <p>
 * With no claim on the request — {@link ActingDomain#UNSPECIFIED}, which covers older clients and
 * every server-to-server caller — every domain is permitted and the platform behaves exactly as it
 * did before acting domains existed.
 */
@Component
@RequiredArgsConstructor
public class ActingDomainCap {

    private final ActingDomainResolver actingDomainResolver;

    /**
     * @param domain the domain a caller would act through
     * @return true when the dashboard this request came from may exercise it
     */
    public boolean permits(UserDomain domain) {
        if (domain == null) {
            return false;
        }
        ActingDomain acting = actingDomainResolver.current();
        // No claim on the request: nothing to narrow to, so every domain stands.
        return !acting.isSpecified() || acting.userDomain() == domain;
    }

    /**
     * Whether the dashboard may exercise any of {@code domains}. Convenience for a check that is
     * satisfied by more than one footing, such as "is this caller staff of some kind?".
     */
    public boolean permitsAny(UserDomain... domains) {
        if (domains == null) {
            return false;
        }
        for (UserDomain domain : domains) {
            if (permits(domain)) {
                return true;
            }
        }
        return false;
    }
}
